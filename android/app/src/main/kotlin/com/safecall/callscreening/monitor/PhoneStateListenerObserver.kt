package com.safecall.callscreening.monitor

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

/**
 * Cai dat cho API 29-30 (`TelephonyCallback` chua ton tai truoc API 31) - dung
 * `PhoneStateListener` (deprecated tu API 31 nhung van hoat dong binh thuong tren 29-30, va la
 * lua chon DUY NHAT tren cac API nay).
 *
 * Luu y da biet (khong anh huong thiet ke - LLD): `PhoneStateListener.phoneNumber`/tham so so
 * dien thoai truyen qua callback co the khac theo OEM tren API 29-30 - KHONG dua vao tham so nay
 * de lay so, luon resolve qua CallLog ([com.safecall.callscreening.monitor.CallLogNumberResolver])
 * thay vi (giong voi API 31+, vi TelephonyCallback cung khong con truyen so truc tiep).
 */
class PhoneStateListenerObserver(private val telephonyManager: TelephonyManager) : CallStateObserver {

    private var listener: PhoneStateListener? = null

    @Suppress("DEPRECATION")
    override fun register(onStateChanged: (CallState) -> Unit) {
        val created = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                onStateChanged(state.let(::mapState))
            }
        }
        listener = created
        telephonyManager.listen(created, PhoneStateListener.LISTEN_CALL_STATE)
    }

    @Suppress("DEPRECATION")
    override fun unregister() {
        listener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        listener = null
    }

    private fun mapState(state: Int): CallState = when (state) {
        TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
        TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
        else -> CallState.IDLE
    }
}
