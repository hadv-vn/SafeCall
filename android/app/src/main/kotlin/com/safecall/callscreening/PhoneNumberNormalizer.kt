package com.safecall.callscreening

/** Nem ra khi input khong phai so/dau so hop le - song song voi Dart InvalidPhoneNumberFormatException. */
class InvalidPhoneNumberFormatException(message: String) : Exception(message)

/**
 * Validate + chuan hoa so/dau so dien thoai ve dang E.164 (LLD: `PhoneNumberNormalizer` (Kotlin)) -
 * PHAI cho output GIONG TUYET DOI voi lib/blocklist/phone_number_normalizer.dart cho cung input
 * (ERD: yeu cau cung cho UNIQUE constraint hoat dong dung).
 *
 * Mo rong v1.3 (FR-9/7.1.1): tham so [EntryType] quyet dinh nguong do dai - EXACT giu nguyen
 * (8-16 ky tu ke ca '+'), PREFIX noi nguong toi thieu xuong 5 ky tu ke ca '+'.
 */
class PhoneNumberNormalizer {
    companion object {
        private const val COUNTRY_CODE = 84
        private const val MIN_TOTAL_LENGTH_EXACT = 8
        private const val MIN_TOTAL_LENGTH_PREFIX = 5
        private const val MAX_TOTAL_LENGTH = 16
        private val CLEAN_REGEX = Regex("[\\s\\-().]")
        private val DIGITS_ONLY_REGEX = Regex("^\\d+$")
    }

    fun normalize(raw: String, entryType: EntryType = EntryType.EXACT): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            throw InvalidPhoneNumberFormatException(
                if (entryType == EntryType.PREFIX) "Vui lòng nhập đầu số cần chặn" else "Vui lòng nhập số điện thoại cần chặn",
            )
        }

        val cleaned = CLEAN_REGEX.replace(trimmed, "")

        val withCountryCode = when {
            cleaned.startsWith("+") -> {
                val digits = cleaned.substring(1)
                assertAllDigits(digits, entryType)
                "+$digits"
            }
            cleaned.startsWith("00") -> {
                val digits = cleaned.substring(2)
                assertAllDigits(digits, entryType)
                "+$digits"
            }
            cleaned.startsWith("0") -> {
                val digits = cleaned.substring(1)
                assertAllDigits(digits, entryType)
                "+$COUNTRY_CODE$digits"
            }
            else -> throw InvalidPhoneNumberFormatException(formatErrorMessage(entryType))
        }

        val minLength = if (entryType == EntryType.PREFIX) MIN_TOTAL_LENGTH_PREFIX else MIN_TOTAL_LENGTH_EXACT
        if (withCountryCode.length < minLength || withCountryCode.length > MAX_TOTAL_LENGTH) {
            throw InvalidPhoneNumberFormatException(
                if (entryType == EntryType.PREFIX) {
                    "Đầu số quá ngắn, có thể chặn nhầm nhiều số không liên quan"
                } else {
                    "Số điện thoại không hợp lệ. Vui lòng kiểm tra lại độ dài số."
                },
            )
        }

        return withCountryCode
    }

    private fun assertAllDigits(digits: String, entryType: EntryType) {
        if (digits.isEmpty() || !DIGITS_ONLY_REGEX.matches(digits)) {
            throw InvalidPhoneNumberFormatException(formatErrorMessage(entryType))
        }
    }

    private fun formatErrorMessage(entryType: EntryType): String {
        return if (entryType == EntryType.PREFIX) {
            "Đầu số không hợp lệ. Chỉ nhập chữ số, có thể bắt đầu bằng dấu +, ví dụ: 0901 hoặc +84901"
        } else {
            "Số điện thoại không hợp lệ. Vui lòng nhập đúng định dạng, ví dụ: 0901234567 hoặc +84901234567"
        }
    }
}
