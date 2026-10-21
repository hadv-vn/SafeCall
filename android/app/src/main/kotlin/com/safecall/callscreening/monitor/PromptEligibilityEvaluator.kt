package com.safecall.callscreening.monitor

import com.safecall.callscreening.ContactsReadException
import com.safecall.callscreening.ContactsRepository
import com.safecall.callscreening.db.BlocklistReadException
import com.safecall.callscreening.db.BlocklistRepository

/**
 * `evaluatePromptEligibility` (LLD pseudocode E1) - quyet dinh co tao prompt xac nhan sau cuoc
 * goi hay khong (FR-2/AC1-AC6). Goi SAU khi da `insertCallRecord` (Luong 2), truoc khi tao
 * `pending_prompts`.
 */
class PromptEligibilityEvaluator(
    private val contactsRepository: ContactsRepository,
    private val blocklistRepository: BlocklistRepository,
) {
    companion object {
        private const val MIN_DURATION_SECONDS = 3
    }

    fun evaluate(entry: ResolvedCallLogEntry, normalizedNumber: String?): PromptEligibility {
        if (!entry.isIncoming) return PromptEligibility.NotEligible(NotEligibleReason.OUTGOING_CALL)
        if (!entry.wasAnswered) return PromptEligibility.NotEligible(NotEligibleReason.NOT_ANSWERED)
        if (normalizedNumber == null) {
            return PromptEligibility.NotEligible(NotEligibleReason.UNRESOLVABLE_NUMBER)
        }
        if (entry.durationSeconds < MIN_DURATION_SECONDS) {
            return PromptEligibility.NotEligible(NotEligibleReason.DURATION_TOO_SHORT)
        }

        return try {
            if (contactsRepository.isNumberInContacts(normalizedNumber)) {
                return PromptEligibility.NotEligible(NotEligibleReason.IS_CONTACT)
            }
            if (blocklistRepository.isBlockedExact(normalizedNumber)) {
                return PromptEligibility.NotEligible(NotEligibleReason.ALREADY_BLOCKED)
            }
            if (blocklistRepository.isConfirmedSafe(normalizedNumber)) {
                return PromptEligibility.NotEligible(NotEligibleReason.ALREADY_CONFIRMED_SAFE)
            }
            PromptEligibility.Eligible
        } catch (e: ContactsReadException) {
            PromptEligibility.NotEligible(NotEligibleReason.TECHNICAL_ERROR)
        } catch (e: BlocklistReadException) {
            PromptEligibility.NotEligible(NotEligibleReason.TECHNICAL_ERROR)
        }
    }
}
