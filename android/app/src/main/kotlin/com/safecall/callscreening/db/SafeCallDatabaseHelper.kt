package com.safecall.callscreening.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLiteOpenHelper phia native (Kotlin). Mo CHUNG MOT file database voi phia Dart (sqflite) -
 * ten file va duong dan PHAI khop voi lib/db/safecall_schema.dart (xem DATABASE_FILE_NAME).
 *
 * Idempotent theo thiet ke (Task 1.1.1/1.3.1): ca Dart va native co the la nguoi mo/tao DB truoc
 * tien (khong co thu tu khoi dong co dinh) - toan bo CREATE_STATEMENTS dung IF NOT EXISTS, va
 * SQLite tu xu ly khoa file giua cac connection dong thoi (PRAGMA busy_timeout) nen khong can co
 * che "ai mo truoc" rieng o tang ung dung.
 */
class SafeCallDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_FILE_NAME, null, SafeCallSchema.SCHEMA_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        // Sua loi phat hien khi test tren thiet bi that (cung loi nhu ben Dart/sqflite):
        // SQLiteOpenHelper cua Android tu boc onCreate() trong 1 transaction - PRAGMA
        // journal_mode/busy_timeout deu KHONG duoc phep (hoac bi am tham bo qua) khi dang trong
        // transaction. PHAI dat o onConfigure() (chay TRUOC moi transaction, moi lan mo connection).
        db.execSQL("PRAGMA journal_mode = WAL")
        db.execSQL("PRAGMA synchronous = NORMAL")
        db.execSQL("PRAGMA busy_timeout = ${SafeCallSchema.PRAGMA_BUSY_TIMEOUT_OTHER_MS}")
        // PRAGMA foreign_keys la PER-CONNECTION - PHAI set lai o moi lan mo (ERD muc "Thiet lap PRAGMA").
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        SafeCallSchema.CREATE_STATEMENTS.forEach { statement -> db.execSQL(statement) }
        recordMigrationApplied(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Chua co migration nao vuot qua v1 trong Phase 1 - placeholder cho Phase sau.
        // KHONG drop/tao lai bang o day khi co version 2+ trong tuong lai.
    }

    /**
     * Mo connection DOC rieng cho CallScreeningService.onScreenCall (ngan han, timeout thap -
     * ERD: PRAGMA busy_timeout = 1000 cho connection nay, khac voi connection ghi/UI).
     */
    fun openReadOnlyForScreening(): SQLiteDatabase {
        val db = readableDatabase
        db.execSQL("PRAGMA busy_timeout = ${SafeCallSchema.PRAGMA_BUSY_TIMEOUT_READ_MS}")
        return db
    }

    private fun recordMigrationApplied(db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put("version", SafeCallSchema.SCHEMA_VERSION)
            put("applied_at", System.currentTimeMillis())
            put("applied_by", "NATIVE")
        }
        db.insert("schema_migrations", null, values)
    }

    companion object {
        const val DATABASE_FILE_NAME = "safecall.db"
    }
}
