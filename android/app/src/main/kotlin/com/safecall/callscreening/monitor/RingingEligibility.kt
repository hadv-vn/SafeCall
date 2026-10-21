package com.safecall.callscreening.monitor

import com.safecall.callscreening.ContactsReadException
import com.safecall.callscreening.ContactsRepository
import com.safecall.callscreening.db.BlocklistReadException
import com.safecall.callscreening.db.BlocklistRepository

/**
 * `evaluateRingingPromptEligibility` (LLD pseudocode E3, noi dung chi tiet bi mat trong qua trinh
 * cascade - tai dung tu SRS FR-8/AC1-AC4,AC10). Khac [PromptEligibilityEvaluator] (FR-2): KHONG
 * co kiem tra wasAnswered/duration (cuoc goi con dang do chuong, chua co ca hai thong tin do).
 */
enum class RingingNotEligibleReason {
    IS_CONTACT, // FR-8/AC2
    ALREADY_BLOCKED, // FR-8/AC3 (phong ve - thuc te khong toi day vi da bi chan o Luong 1)
    ALREADY_CONFIRMED_SAFE, // FR-8/AC4
    UNRESOLVABLE_NUMBER,
    TECHNICAL_ERROR,
}

sealed class RingingEligibility {
    object Eligible : RingingEligibility()
    data class NotEligible(val reason: RingingNotEligibleReason) : RingingEligibility()
}

class RingingEligibilityEvaluator(
    private val contactsRepository: ContactsRepository,
    private val blocklistRepository: BlocklistRepository,
) {
    fun evaluate(normalizedNumber: String?): RingingEligibility {
        if (normalizedNumber == null) {
            return RingingEligibility.NotEligible(RingingNotEligibleReason.UNRESOLVABLE_NUMBER)
        }
        return try {
            if (contactsRepository.isNumberInContacts(normalizedNumber)) {
                return RingingEligibility.NotEligible(RingingNotEligibleReason.IS_CONTACT)
            }
            if (blocklistRepository.isBlockedExact(normalizedNumber)) {
                return RingingEligibility.NotEligible(RingingNotEligibleReason.ALREADY_BLOCKED)
            }
            if (blocklistRepository.isConfirmedSafe(normalizedNumber)) {
                return RingingEligibility.NotEligible(RingingNotEligibleReason.ALREADY_CONFIRMED_SAFE)
            }
            RingingEligibility.Eligible
        } catch (e: ContactsReadException) {
            RingingEligibility.NotEligible(RingingNotEligibleReason.TECHNICAL_ERROR)
        } catch (e: BlocklistReadException) {
            RingingEligibility.NotEligible(RingingNotEligibleReason.TECHNICAL_ERROR)
        }
    }
}
