package com.safecall.callscreening.monitor

import com.safecall.callscreening.ScreeningDecisionReason

/**
 * Gap-fill (khong co trong LLD con sot lai): tu API 31+, `TelephonyCallback.CallStateListener`
 * KHONG con tra so dien thoai kem theo trang thai RINGING (khac han `PhoneStateListener` cu, va
 * day la gioi han rieng tu Android, khong phai loi thiet ke) - nen `CallMonitorService` khong co
 * cach nao tu lay duoc so dang do chuong truc tiep tu callback trang thai.
 *
 * Giai phap: tai dung `CallScreeningService.onScreenCall` (Phase 3) - no LUON duoc OS goi VOI so
 * day du (`Call.Details.handle`) cho MOI cuoc goi den, truoc khi cuoc goi do chuong - ghi lai vao
 * day moi khi mot cuoc goi DUOC PHEP do chuong (`disallowCall=false`), de `CallMonitorService` doc
 * lai ngay khi quan sat duoc trang thai RINGING. Day la singleton trong process, khong qua DB (so
 * dien thoai tam thoi, khong can ben vung).
 */
object RingingCallNumberHolder {
    /**
     * [decisionReason] (mo rong v1.4, FR-11): chi co gia tri CONTACTS_PRIORITY - "mang theo" ly
     * do KHONG chan tu CallScreeningService sang CallMonitorService.handleCallEnded (chi noi THAT
     * SU ghi call_history cho cuoc goi duoc phep do chuong - xem ghi chu class).
     */
    data class RingingNumber(
        val rawNumber: String?,
        val normalizedNumber: String?,
        val decisionReason: ScreeningDecisionReason? = null,
    )

    @Volatile
    private var current: RingingNumber? = null

    fun setRinging(
        rawNumber: String?,
        normalizedNumber: String?,
        decisionReason: ScreeningDecisionReason? = null,
    ) {
        current = RingingNumber(rawNumber, normalizedNumber, decisionReason)
    }

    fun get(): RingingNumber? = current

    fun clear() {
        current = null
    }
}
