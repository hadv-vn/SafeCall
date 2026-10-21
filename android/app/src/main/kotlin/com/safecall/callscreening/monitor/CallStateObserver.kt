package com.safecall.callscreening.monitor

/**
 * Tru tuong hoa viec lang nghe thay doi trang thai cuoc goi - 2 cai dat khac nhau theo API level
 * (LLD: `CallStateObserver`/`CallStateListener`/`TelephonyCallbackObserver`/
 * `PhoneStateListenerObserver`/`CallStateObserverFactory`): `TelephonyCallback` (API 31+,
 * `PhoneStateListener` bi deprecated hoan toan) vs `PhoneStateListener` (API 29-30, con dung
 * duoc, `TelephonyCallback` chua ton tai).
 */
interface CallStateObserver {
    /** @throws SecurityException neu thieu quyen READ_PHONE_STATE - lan truyen len onCreate() cua service. */
    fun register(onStateChanged: (CallState) -> Unit)
    fun unregister()
}
