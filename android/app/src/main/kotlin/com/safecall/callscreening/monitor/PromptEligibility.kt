package com.safecall.callscreening.monitor

/**
 * 8 nhanh KHONG du dieu kien (NOT_ELIGIBLE) cua `evaluatePromptEligibility` (LLD pseudocode E1,
 * noi dung chi tiet bi mat trong qua trinh cascade v1.3 - tai dung tu SRS FR-2/AC1-AC6 + WBS Task
 * 4.1.3 "8 nhanh, bien nguong 2.9s/3.0s"). Can `low-level-designer` xac nhan lai thu tu/day du
 * nhanh neu muon khop tuyet doi voi ban LLD goc da mat.
 */
enum class NotEligibleReason {
    NOT_ANSWERED, // FR-2/AC2 - cuoc goi nho
    OUTGOING_CALL, // FR-2/AC2 - cuoc goi di
    UNRESOLVABLE_NUMBER, // khong lay/chuan hoa duoc so tu CallLog
    IS_CONTACT, // FR-2/AC3
    ALREADY_BLOCKED, // FR-2/AC4
    ALREADY_CONFIRMED_SAFE, // FR-2/AC5
    DURATION_TOO_SHORT, // FR-2/AC6 - duoi 3 giay
    TECHNICAL_ERROR, // BlocklistReadException/ContactsReadException
}

sealed class PromptEligibility {
    object Eligible : PromptEligibility()
    data class NotEligible(val reason: NotEligibleReason) : PromptEligibility()
}
