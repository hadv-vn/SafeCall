package com.safecall.callscreening

import android.net.Uri
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.CallScreeningService as AndroidCallScreeningService
import com.safecall.callscreening.db.BlocklistReadException
import com.safecall.callscreening.db.BlocklistRepository
import com.safecall.callscreening.db.CallHistoryRepository
import com.safecall.callscreening.db.CallHistoryWriteException
import com.safecall.callscreening.db.CallRecordDraft
import com.safecall.callscreening.db.SafeCallDatabaseHelper
import com.safecall.callscreening.db.SettingsFlags
import com.safecall.callscreening.monitor.RingingCallNumberHolder
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit test cho pipeline 6 buoc cua `CallScreeningService.onScreenCall`/`screenSafely` (LLD
 * v1.4). KHONG sua code san pham. 2 kho khan ky thuat da xac nhan THUC TE (chay Gradle, khong chi
 * suy luan) va cach xu ly bang MockK thuan:
 *
 * 1) Khong co constructor injection - 4 dependency (`dbHelper`/`repository`/
 *    `callHistoryRepository`/`contactsRepository`) deu `by lazy { ... }`. Da thu `spyk()` +
 *    `mockkConstructor()` truoc (xem lich su sua doi) nhung THAT BAI THUC SU: Kotlin bien dich
 *    `by lazy { X(applicationContext) }` thanh 1 method-reference BIND CUNG vao instance GOC tai
 *    thoi diem field-initializer chay trong constructor - TRUOC khi `spyk()` boc lai. Vi vay stub
 *    tren object spy (`every { spy.applicationContext } returns ... }`) KHONG chan duoc loi goi
 *    thuc ben trong lambda do (da xac nhan qua stack trace thuc: "Method getApplicationContext in
 *    android.content.ContextWrapper not mocked", nem ra tu chinh instance goc, khong qua spy).
 *    Giai phap dung o day: dung reflection (chi trong test, KHONG dong vao file .kt san pham) ghi
 *    de truc tiep field backing `<ten-property>$delegate` (kieu `kotlin.Lazy`) bang `lazyOf(mock)`
 *    - 1 Lazy DA RESOLVE SAN, khong bao gio goi lai lambda goc => khong con dam vao
 *    `applicationContext`/`SQLiteOpenHelper` thuc nua. Nho vay dung duoc `mockk<T>()` thong thuong
 *    cho 4 dependency, khong can `mockkConstructor`.
 * 2) `CallScreeningService` ke thua `android.telecom.CallScreeningService` (bien dich tu
 *    `android.jar` dang stub trong JVM test thuan, khong Robolectric) - `respondToCall(...)` la
 *    `public final`, goi thuc se throw. Khac voi cac field `by lazy`, loi goi `respondToCall` nam
 *    NGAY TRONG than method `onScreenCall` (khong qua lambda rieng) nen `spyk()` chan duoc binh
 *    thuong (dispatch dung vao doi tuong spy).
 * 3) `CallResponse.Builder()` cung la class framework stub, duoc khoi tao TRUC TIEP (khong injection)
 *    ngay trong `onScreenCall` - dung `mockkConstructor` de chan toan bo constructor + method chain
 *    fluent cua class nay. CHI de khong crash khi chay qua doan code nay - KHONG assert vao cac
 *    truong cua `CallResponse` (anh xa 1:1 tam thuong tu `screening.matched`, khong phai logic
 *    nghiep vu can kiem thu sau - xem nguyen tac #5 "uu tien test qua hanh vi quan sat duoc").
 * 4) `BlocklistPrefixIndex`/`RingingCallNumberHolder` la singleton `object` dung truc tiep trong
 *    pipeline - mock qua `mockkObject` de co lap VA de `verify` tham so goi `setRinging(...)`.
 */
class CallScreeningServiceTest {

    private val blocklistRepository = mockk<BlocklistRepository>()
    private val callHistoryRepository = mockk<CallHistoryRepository>()
    private val contactsRepository = mockk<ContactsRepository>()

    @Before
    fun setUp() {
        mockkConstructor(AndroidCallScreeningService.CallResponse.Builder::class)
        mockkObject(BlocklistPrefixIndex)
        mockkObject(RingingCallNumberHolder)

        every { BlocklistPrefixIndex.ensureFresh(any()) } just Runs
        every { RingingCallNumberHolder.setRinging(any(), any(), any()) } just Runs

        // CallResponse.Builder fluent API - chi de khong crash, khong phai trong tam kiem thu.
        every {
            anyConstructed<AndroidCallScreeningService.CallResponse.Builder>().setDisallowCall(any())
        } answers { self as AndroidCallScreeningService.CallResponse.Builder }
        every {
            anyConstructed<AndroidCallScreeningService.CallResponse.Builder>().setRejectCall(any())
        } answers { self as AndroidCallScreeningService.CallResponse.Builder }
        every {
            anyConstructed<AndroidCallScreeningService.CallResponse.Builder>().setSkipNotification(any())
        } answers { self as AndroidCallScreeningService.CallResponse.Builder }
        every {
            anyConstructed<AndroidCallScreeningService.CallResponse.Builder>().setSkipCallLog(any())
        } answers { self as AndroidCallScreeningService.CallResponse.Builder }
        every {
            anyConstructed<AndroidCallScreeningService.CallResponse.Builder>().build()
        } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun flags(
        contactsPriorityEnabled: Boolean = false,
        blockUnknownEnabled: Boolean = false,
        blockAnonymousEnabled: Boolean = false,
        blockInternationalEnabled: Boolean = false,
    ) = SettingsFlags(contactsPriorityEnabled, blockUnknownEnabled, blockAnonymousEnabled, blockInternationalEnabled)

    /**
     * Ghi de truc tiep backing field `kotlin.Lazy` cua 1 property `by lazy` - xem ghi chu dau file
     * ve ly do KHONG dung duoc `spyk()`/`mockkConstructor()` don thuan cho cac property nay.
     */
    private fun forceLazyValue(target: Any, propertyName: String, value: Any) {
        val field = CallScreeningService::class.java.getDeclaredField("$propertyName\$delegate")
        field.isAccessible = true
        field.set(target, lazyOf(value))
    }

    /** Spy tren instance CallScreeningService thuc, voi 4 dependency da duoc ghi de bang mock. */
    private fun newSpyService(): CallScreeningService {
        val real = CallScreeningService()
        forceLazyValue(real, "dbHelper", mockk<SafeCallDatabaseHelper>())
        forceLazyValue(real, "repository", blocklistRepository)
        forceLazyValue(real, "callHistoryRepository", callHistoryRepository)
        forceLazyValue(real, "contactsRepository", contactsRepository)

        val spy = spyk(real)
        every { spy.respondToCall(any(), any()) } just Runs
        return spy
    }

    private fun mockCallDetails(
        presentation: Int = TelecomManager.PRESENTATION_ALLOWED,
        rawNumber: String? = "0901234567",
    ): Call.Details {
        val details = mockk<Call.Details>()
        every { details.handlePresentation } returns presentation
        if (rawNumber == null) {
            every { details.handle } returns null
        } else {
            val uri = mockk<Uri>()
            every { uri.schemeSpecificPart } returns rawNumber
            every { details.handle } returns uri
        }
        return details
    }

    // =====================================================================================
    // BUOC 1 - so an danh
    // =====================================================================================

    @Test
    fun presentationRestricted_blockAnonymousEnabled_blocksAndRecordsBlockToggleAnonymous() {
        every { blocklistRepository.getSettingsFlags() } returns flags(blockAnonymousEnabled = true)
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(presentation = TelecomManager.PRESENTATION_RESTRICTED, rawNumber = null)

        spy.onScreenCall(details)

        verify { spy.respondToCall(details, any()) }
        val draft = recordSlot.captured
        assertEquals(true, draft.wasBlocked)
        assertEquals(null, draft.matchedEntryType)
        assertEquals(null, draft.matchedPrefixValue)
        assertEquals(ScreeningDecisionReason.BLOCK_TOGGLE_ANONYMOUS, draft.decisionReason)
        assertEquals(null, draft.rawNumber)
        assertEquals(null, draft.normalizedNumber)
    }

    @Test
    fun presentationUnknown_blockAnonymousDisabled_allowsAndSetsRinging() {
        every { blocklistRepository.getSettingsFlags() } returns flags(blockAnonymousEnabled = false)

        val spy = newSpyService()
        val details = mockCallDetails(presentation = TelecomManager.PRESENTATION_UNKNOWN, rawNumber = null)

        spy.onScreenCall(details)

        verify { spy.respondToCall(details, any()) }
        verify { RingingCallNumberHolder.setRinging(null, null, null) }
        verify(exactly = 0) { callHistoryRepository.insertCallRecord(any()) }
    }

    @Test
    fun presentationPayphone_getSettingsFlagsThrows_failsOpenAllowsCall() {
        every { blocklistRepository.getSettingsFlags() } throws BlocklistReadException("boom")

        val spy = newSpyService()
        val details = mockCallDetails(presentation = TelecomManager.PRESENTATION_PAYPHONE, rawNumber = null)

        spy.onScreenCall(details)

        verify { RingingCallNumberHolder.setRinging(null, null, null) }
        verify(exactly = 0) { callHistoryRepository.insertCallRecord(any()) }
    }

    // =====================================================================================
    // BUOC 2-3 - EXACT (+ truong hop bien ngoai pipeline chinh: handle null / normalize loi)
    // =====================================================================================

    @Test
    fun handleNull_rawNumberNull_allowsCallWithoutNormalizing() {
        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = null)

        spy.onScreenCall(details)

        verify { RingingCallNumberHolder.setRinging(null, null, null) }
        verify(exactly = 0) { blocklistRepository.isBlockedExact(any()) }
    }

    @Test
    fun rawNumberInvalidFormat_normalizeThrows_allowsCallMatchedFalse() {
        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "abc")

        spy.onScreenCall(details)

        verify { RingingCallNumberHolder.setRinging("abc", null, null) }
        verify(exactly = 0) { blocklistRepository.isBlockedExact(any()) }
    }

    @Test
    fun exactMatch_blocksAndRecordsEntryTypeExactNoPrefixValue() {
        every { blocklistRepository.isBlockedExact("+84901234567") } returns true
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        val draft = recordSlot.captured
        assertEquals(EntryType.EXACT, draft.matchedEntryType)
        assertEquals(null, draft.matchedPrefixValue)
        assertEquals(null, draft.decisionReason)
        assertEquals(true, draft.wasBlocked)
        assertEquals("+84901234567", draft.normalizedNumber)
    }

    @Test
    fun exactMatch_doesNotCheckContactsPriority() {
        every { blocklistRepository.isBlockedExact(any()) } returns true
        every { callHistoryRepository.insertCallRecord(any()) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        verify(exactly = 0) { contactsRepository.isNumberInContacts(any()) }
    }

    // =====================================================================================
    // BUOC 4 - PREFIX + Contacts-priority
    // =====================================================================================

    @Test
    fun prefixMatch_contactsPriorityDisabled_blocksAndRecordsEntryTypePrefixWithValue() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns flags(contactsPriorityEnabled = false)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns "+8490"
        every { contactsRepository.isNumberInContacts(any()) } returns false
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        val draft = recordSlot.captured
        assertEquals(EntryType.PREFIX, draft.matchedEntryType)
        assertEquals("+8490", draft.matchedPrefixValue)
        assertEquals(true, draft.wasBlocked)
        assertEquals(null, draft.decisionReason)
    }

    @Test
    fun prefixMatch_contactsPriorityEnabledAndIsContact_allowsWithDecisionReasonContactsPriority() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns flags(contactsPriorityEnabled = true)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns "+8490"
        every { contactsRepository.isNumberInContacts(any()) } returns true

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        verify {
            RingingCallNumberHolder.setRinging("0901234567", "+84901234567", ScreeningDecisionReason.CONTACTS_PRIORITY)
        }
        verify(exactly = 0) { callHistoryRepository.insertCallRecord(any()) }
    }

    @Test
    fun prefixMatch_contactsPriorityEnabledButNotContact_stillBlocks() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns flags(contactsPriorityEnabled = true)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns "+8490"
        every { contactsRepository.isNumberInContacts(any()) } returns false
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        assertEquals(EntryType.PREFIX, recordSlot.captured.matchedEntryType)
        assertEquals(true, recordSlot.captured.wasBlocked)
    }

    @Test
    fun prefixMatch_contactsPriorityDisabledEvenIfContact_stillBlocks() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns flags(contactsPriorityEnabled = false)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns "+8490"
        every { contactsRepository.isNumberInContacts(any()) } returns true
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        assertEquals(EntryType.PREFIX, recordSlot.captured.matchedEntryType)
        assertEquals(true, recordSlot.captured.wasBlocked)
    }

    @Test
    fun prefixMatch_contactsReadExceptionDuringLookup_failsOpenToNotContact_stillBlocks() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns flags(contactsPriorityEnabled = true)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns "+8490"
        every { contactsRepository.isNumberInContacts(any()) } throws ContactsReadException("boom")
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        assertEquals(EntryType.PREFIX, recordSlot.captured.matchedEntryType)
        assertEquals(true, recordSlot.captured.wasBlocked)
    }

    // =====================================================================================
    // BUOC 5 - BLOCK-TOGGLE quoc te / so la (khong khop EXACT/PREFIX)
    // =====================================================================================

    @Test
    fun noMatch_internationalNumberAndToggleEnabled_blocksWithReasonInternational() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns
            flags(blockInternationalEnabled = true, blockUnknownEnabled = false)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns null
        every { contactsRepository.isNumberInContacts(any()) } returns false
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "+15551234567")

        spy.onScreenCall(details)

        assertEquals(ScreeningDecisionReason.BLOCK_TOGGLE_INTERNATIONAL, recordSlot.captured.decisionReason)
        assertEquals(true, recordSlot.captured.wasBlocked)
        assertEquals(null, recordSlot.captured.matchedEntryType)
    }

    @Test
    fun noMatch_internationalNumberIsContact_allowsDespiteToggleEnabled() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns
            flags(blockInternationalEnabled = true, blockUnknownEnabled = true)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns null
        every { contactsRepository.isNumberInContacts(any()) } returns true

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "+15551234567")

        spy.onScreenCall(details)

        verify { RingingCallNumberHolder.setRinging("+15551234567", "+15551234567", null) }
        verify(exactly = 0) { callHistoryRepository.insertCallRecord(any()) }
    }

    @Test
    fun noMatch_domesticNumberAndBlockUnknownEnabled_blocksWithReasonUnknown() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns
            flags(blockInternationalEnabled = false, blockUnknownEnabled = true)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns null
        every { contactsRepository.isNumberInContacts(any()) } returns false
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        assertEquals(ScreeningDecisionReason.BLOCK_TOGGLE_UNKNOWN, recordSlot.captured.decisionReason)
        assertEquals(true, recordSlot.captured.wasBlocked)
    }

    @Test
    fun noMatch_bothTogglesEnabledInternationalNumber_internationalTakesPriorityOverUnknown() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns
            flags(blockInternationalEnabled = true, blockUnknownEnabled = true)
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns null
        every { contactsRepository.isNumberInContacts(any()) } returns false
        val recordSlot = slot<CallRecordDraft>()
        every { callHistoryRepository.insertCallRecord(capture(recordSlot)) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "+15551234567")

        spy.onScreenCall(details)

        assertEquals(ScreeningDecisionReason.BLOCK_TOGGLE_INTERNATIONAL, recordSlot.captured.decisionReason)
    }

    @Test
    fun noMatch_bothTogglesDisabled_allowsCall() {
        every { blocklistRepository.isBlockedExact(any()) } returns false
        every { blocklistRepository.getSettingsFlags() } returns flags()
        every { BlocklistPrefixIndex.matchLongestPrefix(any()) } returns null
        every { contactsRepository.isNumberInContacts(any()) } returns false

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        verify { RingingCallNumberHolder.setRinging("0901234567", "+84901234567", null) }
        verify(exactly = 0) { callHistoryRepository.insertCallRecord(any()) }
    }

    // =====================================================================================
    // Fail-open chung + thu tu goi + swallow loi ghi lich su
    // =====================================================================================

    @Test
    fun isBlockedExactThrowsBlocklistReadException_failsOpenAllowsCall() {
        every { blocklistRepository.isBlockedExact(any()) } throws BlocklistReadException("boom")

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        verify { RingingCallNumberHolder.setRinging("0901234567", "+84901234567", null) }
        verify(exactly = 0) { callHistoryRepository.insertCallRecord(any()) }
    }

    @Test
    fun matchedTrue_callsRespondToCallBeforeInsertCallRecord() {
        every { blocklistRepository.isBlockedExact(any()) } returns true
        every { callHistoryRepository.insertCallRecord(any()) } returns 1L

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        spy.onScreenCall(details)

        verifyOrder {
            spy.respondToCall(details, any())
            callHistoryRepository.insertCallRecord(any())
        }
    }

    @Test
    fun insertCallRecordThrowsCallHistoryWriteException_isSwallowed_doesNotPropagate() {
        every { blocklistRepository.isBlockedExact(any()) } returns true
        every { callHistoryRepository.insertCallRecord(any()) } throws CallHistoryWriteException("boom")

        val spy = newSpyService()
        val details = mockCallDetails(rawNumber = "0901234567")

        // Khong duoc throw ra ngoai onScreenCall - neu throw, JUnit tu fail test nay.
        spy.onScreenCall(details)

        verify { spy.respondToCall(details, any()) }
    }
}
