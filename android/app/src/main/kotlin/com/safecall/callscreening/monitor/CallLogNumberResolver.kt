package com.safecall.callscreening.monitor

import android.content.Context
import android.provider.CallLog

/** Nem ra khi khong doc duoc CallLog (thieu quyen/loi provider) - khac voi "khong tim thay ban ghi". */
class CallLogReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class ResolvedCallLogEntry(
    val rawNumber: String?,
    val isIncoming: Boolean,
    val wasAnswered: Boolean,
    val startedAtEpochMillis: Long,
    val durationSeconds: Int,
)

/**
 * Lay thong tin cuoc goi vua ket thuc qua CallLog content provider (LLD Rui ro 1 da giai quyet:
 * FR-2 dung `READ_CALL_LOG`, tach khoi role - khong dua vao tham so so dien thoai tu
 * PhoneStateListener/TelephonyCallback vi khong dang tin cay tren moi API/OEM).
 */
class CallLogNumberResolver(private val context: Context) {

    /** Lay ban ghi CallLog GAN NHAT - goi ngay sau khi CallStateObserver bao IDLE (cuoc goi vua ket thuc). */
    fun resolveMostRecentCall(): ResolvedCallLogEntry? {
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 1",
            )
            cursor?.use {
                if (!it.moveToFirst()) return null
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val duration = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))

                val isIncoming = type == CallLog.Calls.INCOMING_TYPE || type == CallLog.Calls.MISSED_TYPE
                val wasAnswered = type == CallLog.Calls.INCOMING_TYPE

                return ResolvedCallLogEntry(
                    rawNumber = number,
                    isIncoming = isIncoming,
                    wasAnswered = wasAnswered,
                    startedAtEpochMillis = date,
                    durationSeconds = duration,
                )
            }
            return null
        } catch (e: Exception) {
            throw CallLogReadException("Khong doc duoc CallLog", e)
        }
    }
}
