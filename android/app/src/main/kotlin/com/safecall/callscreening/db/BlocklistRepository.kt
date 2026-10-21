package com.safecall.callscreening.db

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import com.safecall.callscreening.EntryType

/**
 * DAO doc/ghi `blocklist`+`confirmed_safe` (LLD: `BlocklistRepository`). `isBlockedExact` dung
 * connection RIENG qua [SafeCallDatabaseHelper.openReadOnlyForScreening] (busy_timeout=1000ms)
 * cho hot-path `CallScreeningService.onScreenCall`. Cac method ghi dung connection ghi thuong
 * (`writableDatabase`, busy_timeout=3000ms) - duoc goi tu `PendingPromptRegistry.resolvePromptAtomically`
 * (native-to-native, khong qua UI thread).
 */
class BlocklistRepository(private val dbHelper: SafeCallDatabaseHelper) {

    /** Dung boi onScreenCall - throws [BlocklistReadException] khi loi, KHONG tu fail-open o day
     * (fail-open la trach nhiem cua CallScreeningService, repository chi bao loi trung thuc).
     * CHI khop EXACT - UNIQUE composite (normalized_number, entry_type) sargable cho query nay,
     * KHOP PREFIX la trach nhiem cua [BlocklistPrefixIndex] (khong qua repository). */
    fun isBlockedExact(normalizedNumber: String): Boolean {
        try {
            val db = dbHelper.openReadOnlyForScreening()
            db.rawQuery(
                "SELECT 1 FROM blocklist WHERE normalized_number = ? AND entry_type = 'EXACT' LIMIT 1",
                arrayOf(normalizedNumber),
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        } catch (e: Exception) {
            throw BlocklistReadException("Khong doc duoc bang blocklist", e)
        }
    }

    /** FR-9/7.2.1 - doc toan bo entry PREFIX qua partial index `idx_blocklist_prefix_values`,
     * dung boi `BlocklistPrefixIndex.rebuild`. */
    fun getAllPrefixValues(): List<String> {
        try {
            val db = dbHelper.openReadOnlyForScreening()
            db.rawQuery(
                "SELECT normalized_number FROM blocklist WHERE entry_type = 'PREFIX'",
                null,
            ).use { cursor ->
                val result = mutableListOf<String>()
                while (cursor.moveToNext()) result.add(cursor.getString(0))
                return result
            }
        } catch (e: Exception) {
            throw BlocklistReadException("Khong doc duoc danh sach PREFIX", e)
        }
    }

    /** FR-9/7.2.1 - point-read `blocklist_meta.prefix_version`, dung boi `BlocklistPrefixIndex.ensureFresh`. */
    fun getPrefixVersion(): Long {
        try {
            val db = dbHelper.openReadOnlyForScreening()
            db.rawQuery("SELECT prefix_version FROM blocklist_meta WHERE id = 1", null).use { cursor ->
                if (!cursor.moveToFirst()) return 0L
                return cursor.getLong(0)
            }
        } catch (e: Exception) {
            throw BlocklistReadException("Khong doc duoc blocklist_meta", e)
        }
    }

    /** FR-2/AC4 (evaluatePromptEligibility defensive-check). */
    fun isConfirmedSafe(normalizedNumber: String): Boolean {
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                "SELECT 1 FROM confirmed_safe WHERE normalized_number = ? LIMIT 1",
                arrayOf(normalizedNumber),
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        } catch (e: Exception) {
            throw BlocklistReadException("Khong doc duoc bang confirmed_safe", e)
        }
    }

    /**
     * FR-3/AC1-AC2 - goi boi `resolvePromptAtomically` khi action=YES. INSERT truc tiep + bat
     * UNIQUE (khong SELECT-roi-INSERT, theo LLD "Luong 3") - tra ve true neu moi them, false neu
     * da ton tai san (AC2 - khong tao ban ghi trung).
     */
    fun addToBlocklist(normalizedNumber: String, source: String, entryType: EntryType = EntryType.EXACT): Boolean {
        return try {
            val values = ContentValues().apply {
                put("normalized_number", normalizedNumber)
                put("entry_type", entryType.name)
                put("source", source)
                put("added_at", System.currentTimeMillis())
            }
            dbHelper.writableDatabase.insertOrThrow("blocklist", null, values)
            true
        } catch (e: SQLiteConstraintException) {
            false // AC2: da ton tai san - khong phai loi
        } catch (e: Exception) {
            throw BlocklistWriteException("Khong ghi duoc vao blocklist", e)
        }
    }

    /** FR-4/AC1 - goi boi `resolvePromptAtomically` khi action=NO. Idempotent (INSERT OR IGNORE). */
    fun markConfirmedSafe(normalizedNumber: String) {
        try {
            val values = ContentValues().apply {
                put("normalized_number", normalizedNumber)
                put("confirmed_at", System.currentTimeMillis())
            }
            dbHelper.writableDatabase.insertWithOnConflict(
                "confirmed_safe",
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE,
            )
        } catch (e: Exception) {
            throw BlocklistWriteException("Khong ghi duoc vao confirmed_safe", e)
        }
    }
}
