package com.safecall.callscreening.monitor

import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

/** Cai dat cho API 31+ (`Build.VERSION_CODES.S`) - `PhoneStateListener` bi deprecated hoan toan tu day. */
@RequiresApi(Build.VERSION_CODES.S)
class TelephonyCallbackObserver(private val telephonyManager: TelephonyManager) : CallStateObserver {

    private var callback: TelephonyCallback? = null

    override fun register(onStateChanged: (CallState) -> Unit) {
        val listener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                onStateChanged(state.toCallState())
            }
        }
        callback = listener
        telephonyManager.registerTelephonyCallback(
            { command -> command.run() }, // Executor dong bo - xu ly tren thread goi lai cua he thong, khong dua len UI thread.
            listener,
        )
    }

    override fun unregister() {
        callback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        callback = null
    }
}

internal fun Int.toCallState(): CallState = when (this) {
    TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
    TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
    else -> CallState.IDLE
}
