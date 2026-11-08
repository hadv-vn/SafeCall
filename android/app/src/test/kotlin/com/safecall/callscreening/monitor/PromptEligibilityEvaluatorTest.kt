package com.safecall.callscreening.monitor

import com.safecall.callscreening.ContactsReadException
import com.safecall.callscreening.ContactsRepository
import com.safecall.callscreening.db.BlocklistReadException
import com.safecall.callscreening.db.BlocklistRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho [PromptEligibilityEvaluator] (LLD pseudocode E1, FR-2/AC1-AC6). Constructor
 * injection san co (ContactsRepository/BlocklistRepository) - mock truc tiep qua MockK, khong can
 * Robolectric vi khong cham Android framework thuc trong unit nay.
 */
class PromptEligibilityEvaluatorTest {

    private val contactsRepository = mockk<ContactsRepository>()
    private val blocklistRepository = mockk<BlocklistRepository>()
    private lateinit var evaluator: PromptEligibilityEvaluator

    @Before
    fun setUp() {
        evaluator = PromptEligibilityEvaluator(contactsRepository, blocklistRepository)
    }

    private fun entry(
        isIncoming: Boolean = true,
        wasAnswered: Boolean = true,
        durationSeconds: Int = 10,
    ) = ResolvedCallLogEntry(
        rawNumber = "0901234567",
        isIncoming = isIncoming,
        wasAnswered = wasAnswered,
        startedAtEpochMillis = 1_000L,
        durationSeconds = durationSeconds,
    )

    // ---- BUOC som: outgoing/not-answered/unresolvable/duration - khong can mock Contacts/Blocklist ----

    @Test
    fun outgoingCall_returnsNotEligibleOutgoingCall() {
        val result = evaluator.evaluate(entry(isIncoming = false), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.OUTGOING_CALL), result)
    }

    @Test
    fun notAnswered_returnsNotEligibleNotAnswered() {
        val result = evaluator.evaluate(entry(wasAnswered = false), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.NOT_ANSWERED), result)
    }

    @Test
    fun unresolvableNumber_returnsNotEligibleUnresolvableNumber() {
        val result = evaluator.evaluate(entry(), null)

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.UNRESOLVABLE_NUMBER), result)
    }

    @Test
    fun durationExactly2Seconds_returnsNotEligibleDurationTooShort() {
        val result = evaluator.evaluate(entry(durationSeconds = 2), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.DURATION_TOO_SHORT), result)
    }

    @Test
    fun durationExactly3Seconds_doesNotFailDurationCheck() {
        every { contactsRepository.isNumberInContacts(any()) } returns false
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.isConfirmedSafe(any()) } returns false

        val result = evaluator.evaluate(entry(durationSeconds = 3), "+84901234567")

        assertEquals(PromptEligibility.Eligible, result)
    }

    @Test
    fun durationAboveThreshold_allChecksFalse_returnsEligible() {
        every { contactsRepository.isNumberInContacts(any()) } returns false
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.isConfirmedSafe(any()) } returns false

        val result = evaluator.evaluate(entry(durationSeconds = 10), "+84901234567")

        assertEquals(PromptEligibility.Eligible, result)
    }

    // ---- BUOC sau: Contacts/Blocklist - khong qua duoc truoc do ----

    @Test
    fun isContact_returnsNotEligibleIsContact() {
        every { contactsRepository.isNumberInContacts(any()) } returns true

        val result = evaluator.evaluate(entry(), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.IS_CONTACT), result)
    }

    @Test
    fun isContact_shortCircuitsBeforeCheckingBlocklist() {
        every { contactsRepository.isNumberInContacts(any()) } returns true

        evaluator.evaluate(entry(), "+84901234567")

        verify(exactly = 0) { blocklistRepository.isBlockedExact(any()) }
        verify(exactly = 0) { blocklistRepository.isConfirmedSafe(any()) }
    }

    @Test
    fun alreadyBlocked_returnsNotEligibleAlreadyBlocked() {
        every { contactsRepository.isNumberInContacts(any()) } returns false
        every { blocklistRepository.isBlockedExact(any()) } returns true

        val result = evaluator.evaluate(entry(), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.ALREADY_BLOCKED), result)
    }

    @Test
    fun alreadyConfirmedSafe_returnsNotEligibleAlreadyConfirmedSafe() {
        every { contactsRepository.isNumberInContacts(any()) } returns false
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.isConfirmedSafe(any()) } returns true

        val result = evaluator.evaluate(entry(), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.ALREADY_CONFIRMED_SAFE), result)
    }

    // ---- Error case: fail toward TECHNICAL_ERROR, khong de exception thoat ra ngoai ----

    @Test
    fun contactsReadException_returnsNotEligibleTechnicalError() {
        every { contactsRepository.isNumberInContacts(any()) } throws ContactsReadException("boom")

        val result = evaluator.evaluate(entry(), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.TECHNICAL_ERROR), result)
    }

    @Test
    fun blocklistReadExceptionOnIsBlockedExact_returnsNotEligibleTechnicalError() {
        every { contactsRepository.isNumberInContacts(any()) } returns false
        every { blocklistRepository.isBlockedExact(any()) } throws BlocklistReadException("boom")

        val result = evaluator.evaluate(entry(), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.TECHNICAL_ERROR), result)
    }

    @Test
    fun blocklistReadExceptionOnIsConfirmedSafe_returnsNotEligibleTechnicalError() {
        every { contactsRepository.isNumberInContacts(any()) } returns false
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.isConfirmedSafe(any()) } throws BlocklistReadException("boom")

        val result = evaluator.evaluate(entry(), "+84901234567")

        assertEquals(PromptEligibility.NotEligible(NotEligibleReason.TECHNICAL_ERROR), result)
    }
}
