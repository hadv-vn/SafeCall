package com.safecall.callscreening

import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.CallScreeningService as AndroidCallScreeningService
import com.safecall.callscreening.db.BlocklistReadException
import com.safecall.callscreening.db.BlocklistRepository
import com.safecall.callscreening.db.CallHistoryRepository
import com.safecall.callscreening.db.CallHistoryWriteException
import com.safecall.callscreening.db.CallRecordDraft
import com.safecall.callscreening.db.SafeCallDatabaseHelper
import com.safecall.callscreening.monitor.RingingCallNumberHolder

/**
 * OS goi khi co cuoc goi den - quyet dinh chan dua tren blocklist (LLD: `CallScreeningService`,
 * `onScreenCall(callDetails)`). Mo rong v1.4 (FR-1/FR-9/FR-11/FR-12): pipeline quyet dinh chan
 * theo DUNG 6 BUOC (LLD v1.4, muc "CallScreeningService - pipeline 6 buoc day du"):
 *   1. So an danh (`handlePresentation` RESTRICTED/UNKNOWN/PAYPHONE) - chan thang theo co
 *      `block_anonymous_enabled` (FR-12), KHONG xet cac buoc sau.
 *   2-3. Khop EXACT (SQL point-lookup, uu tien truoc - re hon) - chan ngay, KHONG xet
 *      CONTACTS-PRIORITY (FR-1/AC9 - khop exact la hanh dong chan CHU DONG, khong bi ghi de).
 *   4. Khop PREFIX (qua [BlocklistPrefixIndex], CHI khi khong khop EXACT) - CONTACTS-PRIORITY
 *      (FR-11) la ngoai le DUY NHAT: BAT + la contact -> KHONG chan.
 *   5. Khong khop ca EXACT va PREFIX - xet BLOCK-TOGGLE (FR-12) "chan so la"/"chan so quoc te",
 *      Contacts luon mien tru ca hai (tai su dung `isContact` da tinh o buoc 4 neu co).
 *   6. Khong bi chan o cac buoc tren - tiep tuc binh thuong toi SCAM-DETECT (`CallMonitorService`).
 * Ghi `call_history` cho nhanh bi chan NGAY (vi cuoc goi bi chan khong bao gio dat RINGING de
 * `CallMonitorService` tu ghi).
 *
 * Fail-open BAT BUOC (LLD bang "Xu ly loi/exception"): bat ky loi/timeout nao trong luc doc
 * blocklist/Contacts deu phai cho cuoc goi tiep tuc do chuong (disallowCall=false) - KHONG BAO GIO
 * chan nham mot cuoc goi hop le do loi ky thuat. Rieng loi tra cuu Contacts (hiem) cung fail-open
 * theo huong "khong phai contact" -> uu tien KHONG chan nham hon la chan dung 1 PREFIX/toggle da
 * cau hinh (LLD v1.4, bang "Xu ly loi/exception" muc `screenSafely` buoc 4/5).
 *
 * Thu tu bat buoc (LLD "Quyet dinh sap xep hot-path"): `respondToCall()` PHAI goi TRUOC
 * `insertCallRecord` - ghi lich su khong nam tren duong toi han ~1500ms, chay fire-and-forget SAU
 * khi OS da nhan phan hoi. Trade-off da duoc chap nhan: mat 1 ban ghi lich su hiem gap neu process
 * bi kill dung luc giua 2 buoc, khong anh huong tinh dung dan cua viec chan.
 */
class CallScreeningService : AndroidCallScreeningService() {

    private val normalizer = PhoneNumberNormalizer()
    private val dbHelper by lazy { SafeCallDatabaseHelper(applicationContext) }
    private val repository: BlocklistRepository by lazy { BlocklistRepository(dbHelper) }
    private val callHistoryRepository: CallHistoryRepository by lazy { CallHistoryRepository(dbHelper) }

    // FR-11/FR-12 (LLD v1.4) - instance rieng, lazy giong dung pattern CallMonitorService dang
    // dung cho cung class nay (goi truc tiep in-process, KHONG qua ContactsLookupModule/
    // MethodChannel - hot-path ~1500ms khong dam bao Flutter engine song).
    private val contactsRepository: ContactsRepository by lazy { ContactsRepository(applicationContext) }

    override fun onScreenCall(callDetails: Call.Details) {
        val screening = screenSafely(callDetails)

        val response = AndroidCallScreeningService.CallResponse.Builder()
            .setDisallowCall(screening.matched)
            .setRejectCall(screening.matched)
            // Quyet dinh trien khai (khong duoc LLD/SRS chi dinh ro): cuoc goi da bi chan khong
            // can hien thong bao nho/goi nhat vao Call Log he thong - nguoi dung da biet qua
            // CallHistoryScreen cua chinh SafeCall.
            .setSkipNotification(screening.matched)
            .setSkipCallLog(screening.matched)
            .build()
        respondToCall(callDetails, response) // TRA LOI OS NGAY - truoc insertCallRecord

        if (screening.matched) {
            try {
                callHistoryRepository.insertCallRecord(
                    CallRecordDraft(
                        rawNumber = screening.rawNumber,
                        normalizedNumber = screening.normalizedNumber,
                        wasAnswered = false,
                        wasBlocked = true, // vao day CHI khi screening.matched - xem "if (screening.matched)" ben tren
                        startedAtEpochMillis = System.currentTimeMillis(),
                        endedAtEpochMillis = System.currentTimeMillis(),
                        durationSeconds = 0,
                        matchedEntryType = screening.entryType,
                        matchedPrefixValue = if (screening.entryType == EntryType.PREFIX) screening.matchedValue else null,
                        decisionReason = screening.decisionReason, // FR-12: null cho EXACT/PREFIX, co gia tri cho BLOCK_TOGGLE_*
                    ),
                )
            } catch (e: CallHistoryWriteException) {
                // LLD: log.warn - khong anh huong response da gui, mat 1 ban ghi lich su chap nhan duoc.
            }
        } else {
            // FR-11: decisionReason=CONTACTS_PRIORITY "mang theo" qua RingingCallNumberHolder vi
            // CallScreeningService KHONG tu ghi call_history cho cuoc goi duoc PHEP do chuong -
            // chi CallMonitorService.handleCallEnded moi ghi, SAU khi cuoc goi ket thuc.
            RingingCallNumberHolder.setRinging(screening.rawNumber, screening.normalizedNumber, screening.decisionReason)
        }
    }

    private data class ScreeningResult(
        val matched: Boolean,
        val rawNumber: String?,
        val normalizedNumber: String?,
        val entryType: EntryType? = null,
        val matchedValue: String? = null,
        val decisionReason: ScreeningDecisionReason? = null,
    )

    private fun screenSafely(callDetails: Call.Details): ScreeningResult {
        // BUOC 1 - nhanh som so an danh, DUNG PIPELINE NGAY (khong xet buoc 2-6).
        when (callDetails.handlePresentation) {
            TelecomManager.PRESENTATION_RESTRICTED,
            TelecomManager.PRESENTATION_UNKNOWN,
            TelecomManager.PRESENTATION_PAYPHONE,
            -> {
                val blockAnonymous = try {
                    repository.getSettingsFlags().blockAnonymousEnabled
                } catch (e: BlocklistReadException) {
                    false // fail-open bat buoc
                }
                return ScreeningResult(
                    matched = blockAnonymous,
                    rawNumber = null,
                    normalizedNumber = null,
                    decisionReason = if (blockAnonymous) ScreeningDecisionReason.BLOCK_TOGGLE_ANONYMOUS else null,
                )
            }
        }

        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart
        if (rawNumber == null) {
            return ScreeningResult(matched = false, rawNumber = null, normalizedNumber = null)
        }

        val normalizedNumber = try {
            normalizer.normalize(rawNumber)
        } catch (e: InvalidPhoneNumberFormatException) {
            null // so khong chuan hoa duoc - khong the khop blocklist, khong phai loi ky thuat can fail-open
        }
        if (normalizedNumber == null) {
            return ScreeningResult(matched = false, rawNumber = rawNumber, normalizedNumber = null)
        }

        return try {
            // BUOC 2-3 - EXACT truoc (re hon), chan ngay, KHONG xet CONTACTS-PRIORITY (FR-1/AC9).
            if (repository.isBlockedExact(normalizedNumber)) {
                return ScreeningResult(true, rawNumber, normalizedNumber, EntryType.EXACT, normalizedNumber)
            }

            BlocklistPrefixIndex.ensureFresh(repository)
            val matchedPrefix = BlocklistPrefixIndex.matchLongestPrefix(normalizedNumber)
            // 1 lan doc duy nhat, dung chung ca buoc 4 VA 5 (LLD v1.4 - khong tang round-trip).
            val flags = repository.getSettingsFlags()

            // BUOC 4 - khop PREFIX: CONTACTS-PRIORITY la ngoai le DUY NHAT (FR-11).
            if (matchedPrefix != null) {
                val isContact = isContactSafely(normalizedNumber)
                if (flags.contactsPriorityEnabled && isContact) {
                    return ScreeningResult(
                        matched = false,
                        rawNumber = rawNumber,
                        normalizedNumber = normalizedNumber,
                        decisionReason = ScreeningDecisionReason.CONTACTS_PRIORITY,
                    )
                }
                return ScreeningResult(true, rawNumber, normalizedNumber, EntryType.PREFIX, matchedPrefix)
            }

            // BUOC 5 - khong khop BLOCK-02 (ca EXACT va PREFIX deu khong khop) - xet BLOCK-TOGGLE.
            val isContact = isContactSafely(normalizedNumber) // CHI 1 trong buoc 4/5 chay toi day moi lan goi
            val isInternational = normalizer.isInternational(normalizedNumber)
            if (flags.blockInternationalEnabled && isInternational && !isContact) {
                return ScreeningResult(
                    matched = true,
                    rawNumber = rawNumber,
                    normalizedNumber = normalizedNumber,
                    decisionReason = ScreeningDecisionReason.BLOCK_TOGGLE_INTERNATIONAL,
                )
            }
            if (flags.blockUnknownEnabled && !isContact) {
                return ScreeningResult(
                    matched = true,
                    rawNumber = rawNumber,
                    normalizedNumber = normalizedNumber,
                    decisionReason = ScreeningDecisionReason.BLOCK_TOGGLE_UNKNOWN,
                )
            }

            // BUOC 6 - khong bi chan o cac buoc tren - tiep tuc binh thuong toi SCAM-DETECT.
            ScreeningResult(false, rawNumber, normalizedNumber)
        } catch (e: BlocklistReadException) {
            ScreeningResult(matched = false, rawNumber = rawNumber, normalizedNumber = normalizedNumber) // fail-open bat buoc
        }
    }

    /** Fail-open cho loi ky thuat (hiem) khi tra cuu Contacts - xem ghi chu class. */
    private fun isContactSafely(normalizedNumber: String): Boolean {
        return try {
            contactsRepository.isNumberInContacts(normalizedNumber)
        } catch (e: ContactsReadException) {
            false
        }
    }
}
