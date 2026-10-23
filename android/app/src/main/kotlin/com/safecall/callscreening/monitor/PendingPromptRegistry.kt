package com.safecall.callscreening.monitor

import android.content.ContentValues
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.safecall.callscreening.db.BlocklistRepository
import com.safecall.callscreening.db.BlocklistWriteException
import com.safecall.callscreening.db.CallHistoryRepository
import com.safecall.callscreening.db.SafeCallDatabaseHelper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

enum class PromptKind { POST_CALL, RINGING }

enum class ResolveOutcome { SUCCESS, ALREADY_RESOLVED, WRITE_FAILED }

private class InMemoryPromptState(
    val normalizedNumber: String,
    val kind: PromptKind,
    val callRecordId: Long?,
    val notificationId: Int,
) {
    val resolved = AtomicBoolean(false)
}

/**
 * `PendingPromptRegistry` (LLD) - "duong dam bao" cho FR-2/FR-3/FR-4: dang ky prompt dang treo
 * va giai quyet DUY NHAT MOT LAN theo nguyen tac first-resolution-wins, du goi tu
 * `ScamConfirmationReceiver` (tap notification) hay tu MainActivity (sendDialogResponse, in-app
 * dialog) - CA HAI co the chay trong CUNG process (khong co race lien-process cho truong hop nay,
 * khac voi BlocklistPrefixIndex o Phase 7).
 *
 * 2 lop atomic:
 * 1. In-memory (`AtomicBoolean.compareAndSet`) - nhanh, du cho truong hop process con song.
 * 2. DB (`UPDATE ... WHERE resolved = 0`) - nguon su that ben vung, PHUC HOI DUNG ca khi process
 *    bi kill roi restart (map in-memory rong) - khong can co che phuc hoi rieng (Task 4.2.4),
 *    atomic UPDATE nay tu nhien dung duoc cho ca 2 truong hop.
 */
class PendingPromptRegistry(
    private val context: Context,
    private val dbHelper: SafeCallDatabaseHelper,
    private val blocklistRepository: BlocklistRepository,
    private val callHistoryRepository: CallHistoryRepository,
) {
    companion object {
        private const val MAX_WRITE_RETRIES = 2

        @Volatile
        private var instance: PendingPromptRegistry? = null

        /**
         * Singleton theo process (khong theo Context cu the) - `CallMonitorService`,
         * `ScamConfirmationReceiver`, va `MainActivity` deu nam trong CUNG process (khong khai
         * bao `android:process` rieng cho bat ky thanh phan nao trong Manifest) nen CHIA SE duoc
         * map in-memory - day la dieu kien de lop 1 (fast in-memory atomic) hoat dong dung giua
         * 3 diem goi nay.
         */
        fun getInstance(context: Context): PendingPromptRegistry {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val appContext = context.applicationContext
                    val dbHelper = SafeCallDatabaseHelper(appContext)
                    PendingPromptRegistry(
                        appContext,
                        dbHelper,
                        BlocklistRepository(dbHelper),
                        CallHistoryRepository(dbHelper),
                    ).also { instance = it }
                }
            }
        }
    }

    private val inMemory = ConcurrentHashMap<String, InMemoryPromptState>()

    /**
     * FR-4/AC2 (POST_CALL) / FR-8/AC9 (RINGING) - huy prompt CU chua giai quyet CUNG loai cho
     * CUNG so truoc khi tao prompt MOI (khong resolve=1 trong DB - khong co hanh dong nguoi dung
     * thuc su, chi huy hien thi notification/overlay - xem ly do khong the dat resolved_action o
     * docblock class).
     */
    fun cancelActivePromptFor(normalizedNumber: String, kind: PromptKind) {
        val toCancel = inMemory.entries.filter {
            it.value.normalizedNumber == normalizedNumber &&
                it.value.kind == kind &&
                !it.value.resolved.get()
        }
        for (entry in toCancel) {
            NotificationManagerCompat.from(context).cancel(entry.value.notificationId)
            inMemory.remove(entry.key)
        }
    }

    fun create(
        normalizedNumber: String,
        kind: PromptKind,
        callRecordId: Long?,
        notificationId: Int,
    ): String {
        val promptId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("prompt_id", promptId)
            put("normalized_number", normalizedNumber)
            put("kind", kind.name)
            put("call_record_id", callRecordId)
            put("notification_id", notificationId)
            put("resolved", 0)
            put("created_at", System.currentTimeMillis())
        }
        dbHelper.writableDatabase.insertOrThrow("pending_prompts", null, values)
        inMemory[promptId] = InMemoryPromptState(normalizedNumber, kind, callRecordId, notificationId)
        return promptId
    }

    fun resolvePromptAtomically(promptId: String, action: String, resolvedBy: String): ResolveOutcome {
        // Lop 1 - nhanh, trong process (khong anh huong dung dan neu bo qua vi process vua restart).
        val memoryState = inMemory[promptId]
        if (memoryState != null && !memoryState.resolved.compareAndSet(false, true)) {
            cancelNotificationSafely(memoryState.notificationId)
            return ResolveOutcome.ALREADY_RESOLVED
        }

        // Lop 2 - DB, nguon su that ben vung (xem lop ca khi memoryState == null sau restart).
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val rowsAffected = db.update(
            "pending_prompts",
            ContentValues().apply {
                put("resolved", 1)
                put("resolved_action", action)
                put("resolved_by", resolvedBy)
                put("resolved_at", now)
            },
            "prompt_id = ? AND resolved = 0",
            arrayOf(promptId),
        )

        if (rowsAffected == 0) {
            inMemory.remove(promptId)
            findNotificationId(db, promptId)?.let { cancelNotificationSafely(it) }
            return ResolveOutcome.ALREADY_RESOLVED
        }

        val row = fetchPromptRow(db, promptId)
        if (row == null) {
            // Khong the xay ra trong dieu kien binh thuong (vua UPDATE thanh cong dong nay) - fail-safe.
            return ResolveOutcome.WRITE_FAILED
        }

        val sideEffectSucceeded = performSideEffectWithRetry(action, row.normalizedNumber)
        if (!sideEffectSucceeded) {
            // ERD: "chi false->true, tru rollback WRITE_FAILED" - hoan tac de co the thu lai sau.
            db.update(
                "pending_prompts",
                ContentValues().apply {
                    put("resolved", 0)
                    putNull("resolved_action")
                    putNull("resolved_by")
                    putNull("resolved_at")
                },
                "prompt_id = ?",
                arrayOf(promptId),
            )
            inMemory[promptId]?.resolved?.set(false)
            return ResolveOutcome.WRITE_FAILED
        }

        if (row.kind == PromptKind.POST_CALL.name && row.callRecordId != null) {
            callHistoryRepository.recordUserConfirmation(row.callRecordId, action, resolvedBy)
        }

        inMemory.remove(promptId)
        cancelNotificationSafely(row.notificationId)
        return ResolveOutcome.SUCCESS
    }

    /**
     * FR-8 (Phase 6) - ghi ket qua `endCall()` NGAY vao dong `pending_prompts` da resolved=1,
     * resolved_action='YES', resolved_by='RINGING_OVERLAY' (goi SAU `resolvePromptAtomically` +
     * sau khi thu `endCall()` xong, tu `CallMonitorService.resolveRingingYes`). An toan ve CHECK
     * constraint: `ck_pending_prompts_termination_outcome` chi doi hoi 3 cot kia DA dung gia tri
     * yeu cau tai thoi diem ghi - khong doi hoi ghi CUNG luc nhu truong hop call_history.
     */
    fun setTerminationOutcome(promptId: String, outcome: String) {
        dbHelper.writableDatabase.update(
            "pending_prompts",
            ContentValues().apply { put("termination_outcome", outcome) },
            "prompt_id = ?",
            arrayOf(promptId),
        )
    }

    private data class ResolvedRingingPrompt(val action: String, val terminationOutcome: String?)

    /**
     * FR-8 (Phase 6) - `flushPendingTerminationOutcomeIfAny`: tim prompt `kind=RINGING` DA resolved
     * nhung `call_record_id` con NULL (resolve truoc khi `call_history` row ton tai) cho so nay -
     * goi tu `CallMonitorService.handleCallEnded` ngay sau `insertCallRecord`, theo dung co che
     * flush tong quat da chot o ERD (ap dung cho CA HAI nhanh YES/NO).
     */
    private fun findResolvedRingingPromptFor(normalizedNumber: String): ResolvedRingingPrompt? {
        dbHelper.writableDatabase.rawQuery(
            """
            SELECT resolved_action, termination_outcome FROM pending_prompts
            WHERE kind = 'RINGING' AND normalized_number = ? AND resolved = 1 AND call_record_id IS NULL
            ORDER BY resolved_at DESC LIMIT 1
            """,
            arrayOf(normalizedNumber),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return ResolvedRingingPrompt(
                action = cursor.getString(0),
                terminationOutcome = if (cursor.isNull(1)) null else cursor.getString(1),
            )
        }
    }

    /**
     * Backfill `call_history` cho ban ghi vua tao bang ket qua da resolve truoc do qua
     * `RingingScamOverlay` (neu co) - xem [findResolvedRingingPromptFor].
     */
    fun flushPendingTerminationOutcomeIfAny(normalizedNumber: String, callRecordId: Long) {
        val resolved = findResolvedRingingPromptFor(normalizedNumber) ?: return
        callHistoryRepository.recordUserConfirmation(
            callRecordId,
            resolved.action,
            "RINGING_OVERLAY",
            resolved.terminationOutcome,
        )
    }

    private fun performSideEffectWithRetry(action: String, normalizedNumber: String): Boolean {
        repeat(MAX_WRITE_RETRIES) {
            try {
                when (action) {
                    "YES" -> blocklistRepository.addToBlocklist(normalizedNumber, "AUTO_CONFIRMED")
                    "NO" -> blocklistRepository.markConfirmedSafe(normalizedNumber)
                }
                return true
            } catch (e: BlocklistWriteException) {
                // thu lai
            }
        }
        return false
    }

    private fun cancelNotificationSafely(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private data class PromptRow(
        val normalizedNumber: String,
        val kind: String,
        val callRecordId: Long?,
        val notificationId: Int,
    )

    private fun fetchPromptRow(db: android.database.sqlite.SQLiteDatabase, promptId: String): PromptRow? {
        db.rawQuery(
            "SELECT normalized_number, kind, call_record_id, notification_id FROM pending_prompts WHERE prompt_id = ?",
            arrayOf(promptId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return PromptRow(
                normalizedNumber = cursor.getString(0),
                kind = cursor.getString(1),
                callRecordId = if (cursor.isNull(2)) null else cursor.getLong(2),
                notificationId = cursor.getInt(3),
            )
        }
    }

    private fun findNotificationId(db: android.database.sqlite.SQLiteDatabase, promptId: String): Int? {
        db.rawQuery(
            "SELECT notification_id FROM pending_prompts WHERE prompt_id = ?",
            arrayOf(promptId),
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getInt(0)
        }
    }
}
