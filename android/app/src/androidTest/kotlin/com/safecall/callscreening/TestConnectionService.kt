package com.safecall.callscreening

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * ConnectionService test harness MANAGED - KHONG self-managed (KHONG set
 * `PhoneAccount.CAPABILITY_SELF_MANAGED`, KHONG dung `MANAGE_OWN_CALLS`/Core-Telecom) - chi dung
 * cho `AnonymousCallBlockingInstrumentedTest` (goi FR-12 AC3).
 *
 * Ly do BAT BUOC phai MANAGED (xem comment chi tiet trong file test): da xac nhan qua ma nguon
 * AOSP (`CallsManager.onSuccessfulIncomingCall` bo qua call filtering/`CallScreeningService` cho
 * cuoc goi `isSelfManaged()`) - self-managed se KHONG BAO GIO khien `CallScreeningService` cua
 * SafeCall duoc goi, vo dung cho muc dich tai tao FR-12 AC3 tren AVD.
 *
 * [onCreateIncomingConnection] doc `presentation` tu extras cua [ConnectionRequest] (key
 * [EXTRA_PRESENTATION], tu dinh nghia rieng cho test nay - KHONG phai API Android chuan) de 1
 * ConnectionService tiem duoc ca 3 gia tri PRESENTATION_RESTRICTED/UNKNOWN/PAYPHONE tuy test case
 * truyen vao qua `TelecomManager.addNewIncomingCall(handle, extras)`.
 */
class TestConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest,
    ): Connection {
        val presentation = request.extras?.getInt(EXTRA_PRESENTATION, TelecomManager.PRESENTATION_ALLOWED)
            ?: TelecomManager.PRESENTATION_ALLOWED

        val connection = object : Connection() {
            override fun onDisconnect() {
                // Telecom tu goi callback nay khi NO muon ket thuc cuoc goi (vd: nhanh BI CHAN -
                // CallScreeningService tra disallow/reject=true) - PHAI tu danh dau disconnected +
                // destroy() de hoan tat vong doi, khong duoc bo qua.
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
                if (activeConnection === this) activeConnection = null
            }
        }
        // address=null + presentation tuong ung - mo phong DUNG hanh vi Telecom thuc cho so an
        // danh/Private Caller ID (RIL thuc cung gui address null kem presentation flag, KHONG
        // phai mot so thuc bi che) - khong anh huong ket qua vi BUOC 1 cua CallScreeningService
        // (xem CallScreeningService.screenSafely) hoan toan KHONG doc callDetails.handle cho 3 gia
        // tri presentation nay.
        connection.setAddress(null, presentation)
        connection.setConnectionCapabilities(Connection.CAPABILITY_HOLD)
        connection.setRinging() // tuong duong STATE_RINGING - khong co setter cong khai nao khac.
        activeConnection = connection
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest,
    ) {
        // Khong xu ly gi them - test duoc kiem soat chat, khong ky vong roi vao nhanh nay. Neu
        // Telecom goi toi day thay vi onCreateIncomingConnection, test se tu fail do khong bao gio
        // thay ban ghi call_history/connection nao trong AWAIT_TIMEOUT_MS - day la tin hieu loi
        // that (vd: PhoneAccount chua duoc enable), khong bi nuot am tham.
    }

    companion object {
        const val EXTRA_PRESENTATION = "com.safecall.callscreening.test.EXTRA_PRESENTATION"

        @Volatile
        private var activeConnection: Connection? = null

        /**
         * Dung boi `@After` cua test de chu dong ket thuc cuoc goi dang do chuong sau MOI test
         * case (chi can thiet cho nhanh KHONG bi chan - nhanh BI CHAN da duoc Telecom tu dong
         * disconnect qua [onCreateIncomingConnection]'s `onDisconnect()` o tren) - tranh de lai
         * cuoc goi "mo coi" tren Telecom giua cac test, se lam cac test sau bi anh huong.
         * try/catch chi de don dep an toan (connection co the da duoc destroy() tu truoc) - KHONG
         * nuot loi assertion nao cua test, chi bao ve buoc teardown.
         */
        fun endActiveConnection() {
            val connection = activeConnection ?: return
            try {
                connection.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                connection.destroy()
            } catch (e: IllegalStateException) {
                // Da duoc Telecom tu dong disconnect truoc do (nhanh bi chan) - khong con gi de don dep.
            }
            activeConnection = null
        }
    }
}
