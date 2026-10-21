package com.safecall.callscreening

import android.content.Context
import io.flutter.plugin.common.MethodChannel

/**
 * Wrapper MethodChannel cho [ContactsRepository] (FR-5/AC8, BlocklistScreen Dart). Logic tra cuu
 * thuan nam o ContactsRepository - dung chung voi `evaluatePromptEligibility` native-internal
 * (Phase 4), xem ghi chu o do.
 */
class ContactsLookupModule(context: Context) {

    private val repository = ContactsRepository(context)

    fun isNumberInContacts(result: MethodChannel.Result, normalizedNumber: String) {
        try {
            result.success(repository.isNumberInContacts(normalizedNumber))
        } catch (e: ContactsReadException) {
            // Hiem - BlocklistScreen (UI) khong co nhanh "loi tra cuu contact" rieng trong UIUX,
            // coi nhu khong khop de khong chan luong them so (an toan hon bao loi cung).
            result.success(false)
        }
    }

    fun countContactsMatchingPrefix(result: MethodChannel.Result, normalizedPrefix: String) {
        try {
            result.success(repository.countContactsMatchingPrefix(normalizedPrefix))
        } catch (e: ContactsReadException) {
            result.success(0)
        }
    }
}
