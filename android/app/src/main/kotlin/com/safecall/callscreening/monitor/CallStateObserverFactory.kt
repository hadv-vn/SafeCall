package com.safecall.callscreening.monitor

import android.os.Build
import android.telephony.TelephonyManager

object CallStateObserverFactory {
    fun create(telephonyManager: TelephonyManager): CallStateObserver {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            TelephonyCallbackObserver(telephonyManager)
        } else {
            PhoneStateListenerObserver(telephonyManager)
        }
    }
}
