package com.safecall.callscreening.db

import android.content.ContentValues
import com.safecall.callscreening.EntryType

/** Nem ra khi ghi `call_history` loi - LLD bang loi: catch, log, KHONG retry, KHONG anh huong response da gui. */
class CallHistoryWriteException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Draft ban ghi lich su cuoc goi truoc khi insert (LLD: tham so cua `insertCallRecord(draft)`).
 * [matchedEntryType]/[matchedPrefixValue] (FR-6/FR-9, 7.3.1) CHI co gia tri khi `wasBlocked=true`
 * (nhanh chan real-time, ghi tu `CallScreeningService`) - `CallMonitorService` (Luong 2) luon
 * truyen ca 2 gia tri null (dung nhu LLD: "CallMonitorService luon truyen matchedEntryType=null").
 */
data class CallRecordDraft(
    val rawNumber: String?,
    val normalizedNumber: String?,
    val wasAnswered: Boolean,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationSeconds: Int,
    val matchedEntryType: EntryType? = null,
    val matchedPrefixValue: String? = null,
)

/**
 * DAO ghi `call_history` (LLD: `CallHistoryRepository`).
 *
 * `updateWasPrompted` la method PHAT SINH KHI CODE (khong co trong LLD con sot lai) - LLD mo ta
 * pseudocode Luong 2 goi `insertCallRecord` TRUOC `evaluatePromptEligibility`, nghia la
 * `was_prompted` chua biet duoc tai thoi diem insert; can mot UPDATE rieng ngay sau khi tao
 * prompt thanh cong. Can `low-level-designer` xac nhan lai neu muon chinh thuc hoa.
 */
class CallHistoryRepository(private val dbHelper: SafeCallDatabaseHelper) {

    fun insertCallRecord(draft: CallRecordDraft): Long {
        try {
            val wasBlocked = draft.matchedEntryType != null
            val values = ContentValues().apply {
                put("raw_number", draft.rawNumber)
                put("normalized_number", draft.normalizedNumber)
                put("direction", "INCOMING")
                put("was_answered", if (draft.wasAnswered) 1 else 0)
                put("was_blocked", if (wasBlocked) 1 else 0)
                put("was_prompted", 0)
                put("started_at", draft.startedAtEpochMillis)
                put("ended_at", draft.endedAtEpochMillis)
                put("duration_seconds", draft.durationSeconds)
                put("matched_entry_type", draft.matchedEntryType?.name)
                put(
                    "matched_prefix_value",
                    if (draft.matchedEntryType == EntryType.PREFIX) draft.matchedPrefixValue else null,
                )
            }
            return dbHelper.writableDatabase.insertOrThrow("call_history", null, values)
        } catch (e: Exception) {
            throw CallHistoryWriteException("Khong ghi duoc call_history", e)
        }
    }

    fun updateWasPrompted(callRecordId: Long, wasPrompted: Boolean) {
        try {
            val values = ContentValues().apply { put("was_prompted", if (wasPrompted) 1 else 0) }
            dbHelper.writableDatabase.update(
                "call_history", values, "id = ?", arrayOf(callRecordId.toString()),
            )
        } catch (e: Exception) {
            throw CallHistoryWriteException("Khong cap nhat duoc was_prompted", e)
        }
    }

    /**
     * FR-3/FR-4/FR-8 - goi boi `resolvePromptAtomically` (POST_CALL, thang first-resolution-wins)
     * VA boi `flushPendingTerminationOutcomeIfAny` (RINGING, Phase 6) khi backfill ket qua da co
     * tu truoc luc tao ban ghi `call_history`.
     *
     * QUAN TRONG (sua loi thiet ke phat hien khi code, Phase 4): LLD liet ke 2 method rieng
     * `updateConfirmationResponse(callRecordId, response)` va `setConfirmationSource(callRecordId,
     * source)`, nhung ERD co CHECK constraint `ck_call_history_response_requires_source` doi hoi
     * CA HAI cot cung NULL hoac cung NOT NULL - SQLite kiem tra CHECK ngay tai moi statement UPDATE
     * (khong hoan lai den commit). Goi 2 UPDATE rieng se lam statement DAU TIEN vi pham CHECK ngay
     * lap tuc (mot cot da co gia tri, cot kia van NULL). Gop thanh 1 UPDATE duy nhat o day.
     *
     * Luon set `was_prompted=1` trong CUNG statement (moi lan goi method nay DEU dang ghi nhan
     * phan hoi cho mot prompt da hien thi - qua Notification/Dialog HOAC qua RingingScamOverlay -
     * CHECK `ck_call_history_response_requires_prompt` doi hoi dieu nay) - thay cho viec dua vao
     * [updateWasPrompted] da chay truoc do (van dung cho nhanh POST_CALL, nhung khong ap dung cho
     * nhanh RINGING-flush vi ban ghi call_history luc insert chua he biet gi ve prompt RINGING).
     *
     * [terminationOutcome] null cho nhanh "Khong"/FR-3-FR-4 (khong lien quan ngat goi); co gia tri
     * CHI cho nhanh FR-8 "Co" luc dang do chuong.
     */
    fun recordUserConfirmation(
        callRecordId: Long,
        response: String,
        source: String,
        terminationOutcome: String? = null,
    ) {
        try {
            val values = ContentValues().apply {
                put("user_response", response)
                put("confirmation_source", source)
                put("was_prompted", 1)
                if (terminationOutcome != null) put("termination_outcome", terminationOutcome)
            }
            dbHelper.writableDatabase.update(
                "call_history", values, "id = ?", arrayOf(callRecordId.toString()),
            )
        } catch (e: Exception) {
            throw CallHistoryWriteException("Khong cap nhat duoc user_response/confirmation_source", e)
        }
    }
}
