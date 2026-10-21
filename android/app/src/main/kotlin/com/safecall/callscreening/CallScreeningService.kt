package com.safecall.callscreening

import android.telecom.Call
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
 * `onScreenCall(callDetails)`, pseudocode E5). Mo rong v1.3 (FR-1/FR-9/FR-6, 7.2.2): khop CA
 * EXACT (SQL point-lookup, uu tien truoc - re hon) VA PREFIX (qua [BlocklistPrefixIndex], CHI khi
 * khong khop EXACT); ghi `call_history` cho nhanh bi chan NGAY (vi cuoc goi bi chan khong bao gio
 * dat RINGING de `CallMonitorService` tu ghi).
 *
 * Fail-open BAT BUOC (LLD bang "Xu ly loi/exception"): bat ky loi/timeout nao trong luc doc
 * blocklist deu phai cho cuoc goi tiep tuc do chuong (disallowCall=false) - KHONG BAO GIO chan
 * nham mot cuoc goi hop le do loi ky thuat.
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
                        startedAtEpochMillis = System.currentTimeMillis(),
                        endedAtEpochMillis = System.currentTimeMillis(),
                        durationSeconds = 0,
                        matchedEntryType = screening.entryType,
                        matchedPrefixValue = if (screening.entryType == EntryType.PREFIX) screening.matchedValue else null,
                    ),
                )
            } catch (e: CallHistoryWriteException) {
                // LLD: log.warn - khong anh huong response da gui, mat 1 ban ghi lich su chap nhan duoc.
            }
        } else {
            RingingCallNumberHolder.setRinging(screening.rawNumber, screening.normalizedNumber)
        }
    }

    private data class ScreeningResult(
        val matched: Boolean,
        val rawNumber: String?,
        val normalizedNumber: String?,
        val entryType: EntryType? = null,
        val matchedValue: String? = null,
    )

    private fun screenSafely(callDetails: Call.Details): ScreeningResult {
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
            if (repository.isBlockedExact(normalizedNumber)) {
                ScreeningResult(true, rawNumber, normalizedNumber, EntryType.EXACT, normalizedNumber)
            } else {
                BlocklistPrefixIndex.ensureFresh(repository)
                val matchedPrefix = BlocklistPrefixIndex.matchLongestPrefix(normalizedNumber)
                if (matchedPrefix != null) {
                    ScreeningResult(true, rawNumber, normalizedNumber, EntryType.PREFIX, matchedPrefix)
                } else {
                    ScreeningResult(false, rawNumber, normalizedNumber)
                }
            }
        } catch (e: BlocklistReadException) {
            ScreeningResult(matched = false, rawNumber = rawNumber, normalizedNumber = normalizedNumber) // fail-open bat buoc
        }
    }
}
