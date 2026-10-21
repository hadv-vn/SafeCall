package com.safecall.callscreening

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat

/** Nem ra khi doc Contacts provider loi ky thuat (hiem) - khac voi "khong co quyen" (tra false, khong phai loi). */
class ContactsReadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Logic tra cuu contact THUAN (khong phu thuoc MethodChannel) - dung chung boi [ContactsLookupModule]
 * (Dart-facing, FR-5/AC8) VA `evaluatePromptEligibility` trong `CallMonitorService` (native-internal,
 * FR-2/AC3) - tach ra de khong trung lap logic giua 2 noi goi.
 */
class ContactsRepository(private val context: Context) {

    fun isNumberInContacts(normalizedNumber: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // FR-7/AC5: chua cap quyen Danh ba -> coi nhu khong phai contact (suy giam nhe nhang).
            return false
        }
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null,
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val contactNumber = it.getString(0) ?: continue
                    if (PhoneNumberUtils.compare(contactNumber, normalizedNumber)) {
                        return true
                    }
                }
            }
            return false
        } catch (e: Exception) {
            throw ContactsReadException("Khong doc duoc Contacts provider", e)
        }
    }

    /**
     * FR-9/AC5 - dem so CONTACT (khong phai so dien thoai) co ít nhất 1 số BẮT ĐẦU BẰNG
     * [normalizedPrefix] sau chuan hoa - LLD tu ghi day la diem chua chot ("cần xác nhận riêng...
     * đếm theo 'số contact' hay 'số điện thoại'"), chon dem theo CONTACT (dedupe qua CONTACT_ID)
     * vi khop voi cau UIUX "khớp với N liên hệ trong danh bạ".
     *
     * So contact khong chuan hoa duoc (vd extension, ky tu la) bi BO QUA am tham - day chi la canh
     * bao uoc luong, khong phai validate bat buoc dung 100%.
     */
    fun countContactsMatchingPrefix(normalizedPrefix: String): Int {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return 0
        }
        val normalizer = PhoneNumberNormalizer()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null,
                null,
                null,
            )
            val matchedContactIds = mutableSetOf<Long>()
            cursor?.use {
                while (it.moveToNext()) {
                    val contactId = it.getLong(0)
                    val contactNumber = it.getString(1) ?: continue
                    val normalizedContactNumber = try {
                        normalizer.normalize(contactNumber)
                    } catch (e: InvalidPhoneNumberFormatException) {
                        continue
                    }
                    if (normalizedContactNumber.startsWith(normalizedPrefix)) {
                        matchedContactIds.add(contactId)
                    }
                }
            }
            return matchedContactIds.size
        } catch (e: Exception) {
            throw ContactsReadException("Khong doc duoc Contacts provider", e)
        }
    }
}
