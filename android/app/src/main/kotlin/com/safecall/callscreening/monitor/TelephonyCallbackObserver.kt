package com.safecall.callscreening.monitor

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/** Cai dat cho API 31+ (`Build.VERSION_CODES.S`) - `PhoneStateListener` bi deprecated hoan toan tu day. */
@RequiresApi(Build.VERSION_CODES.S)
class TelephonyCallbackObserver(private val telephonyManager: TelephonyManager) : CallStateObserver {

    private var callback: TelephonyCallback? = null

    // PHAI la main-thread executor (khong phai executor dong bo chay truc tiep tren Binder pool
    // thread) - xac nhan BUG THAT qua test tay (RingingScamOverlay.show() goi WindowManager.addView()
    // trong onCallStateChanged(), nem RuntimeException "Can't create handler inside thread ...
    // that has not called Looper.prepare()" khi executor chay tren Binder thread). Exception nay
    // khien onRawCallStateChanged() thoat giua chung TRUOC khi cap nhat lastState, lam
    // handleCallEnded() khong bao gio chay khi cuoc goi ket thuc - dung bug "mat call_history" da
    // ghi trong Decision Log khoi #7. PhoneStateListenerObserver (API 29-30) khong bi loi nay vi
    // `telephonyManager.listen()` tu dung Handler cua thread goi register() (main thread).
    private val mainThreadExecutor: Executor = Executor { command -> Handler(Looper.getMainLooper()).post(command) }

    override fun register(onStateChanged: (CallState) -> Unit) {
        val listener = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                onStateChanged(state.toCallState())
            }
        }
        callback = listener
        telephonyManager.registerTelephonyCallback(mainThreadExecutor, listener)
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
