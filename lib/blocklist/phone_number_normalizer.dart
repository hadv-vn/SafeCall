import 'blocklist_models.dart';

/// Nem ra khi input khong phai so/dau so hop le (FR-5/AC4, FR-9/AC2-AC3).
class InvalidPhoneNumberFormatException implements Exception {
  InvalidPhoneNumberFormatException(this.message);

  final String message;

  @override
  String toString() => 'InvalidPhoneNumberFormatException: $message';
}

/// Validate + chuan hoa so/dau so dien thoai ve dang E.164 (LLD: `PhoneNumberNormalizer` (Dart)).
///
/// Mo rong v1.3 (FR-9/7.1.1): tham so [EntryType] quyet dinh nguong do dai - EXACT giu nguyen
/// (8-16 ky tu ke ca '+'), PREFIX noi nguong toi thieu xuong 5 ky tu ke ca '+' (tuc toi thieu 4
/// ky tu so khong tinh '+'/ma quoc gia, dung FR-9/AC2).
///
/// Gia dinh ma quoc gia mac dinh +84 (Viet Nam) khi nguoi dung nhap dang noi dia bat dau bang
/// "0" - toan bo tai lieu SRS/ERD deu viet bang tieng Viet va vi du dung dang "0901234567"/"+84901234567"
/// (SRS FR-1/AC2), chua co stakeholder chi dinh thi truong khac - CAN xac nhan lai neu mo rong
/// da quoc gia.
class PhoneNumberNormalizer {
  static const int _countryCode = 84;
  static const int _minTotalLengthExact = 8;
  static const int _minTotalLengthPrefix = 5;
  static const int _maxTotalLength = 16;

  /// [raw] co the o dang "0901234567", "+84901234567", "0084901234567", co khoang trang/dau
  /// gach ngang/ngoac - duoc loai bo truoc khi validate.
  String validateAndNormalize(String raw, {EntryType entryType = EntryType.exact}) {
    final trimmed = raw.trim();
    if (trimmed.isEmpty) {
      throw InvalidPhoneNumberFormatException(
        entryType == EntryType.prefix
            ? 'Vui lòng nhập đầu số cần chặn'
            : 'Vui lòng nhập số điện thoại cần chặn',
      );
    }

    final cleaned = trimmed.replaceAll(RegExp(r'[\s\-().]'), '');

    String withCountryCode;
    if (cleaned.startsWith('+')) {
      final digits = cleaned.substring(1);
      _assertAllDigits(digits, entryType);
      withCountryCode = '+$digits';
    } else if (cleaned.startsWith('00')) {
      final digits = cleaned.substring(2);
      _assertAllDigits(digits, entryType);
      withCountryCode = '+$digits';
    } else if (cleaned.startsWith('0')) {
      final digits = cleaned.substring(1);
      _assertAllDigits(digits, entryType);
      withCountryCode = '+$_countryCode$digits';
    } else {
      throw InvalidPhoneNumberFormatException(_formatErrorMessage(entryType));
    }

    final minLength = entryType == EntryType.prefix ? _minTotalLengthPrefix : _minTotalLengthExact;
    if (withCountryCode.length < minLength || withCountryCode.length > _maxTotalLength) {
      throw InvalidPhoneNumberFormatException(
        entryType == EntryType.prefix
            ? 'Đầu số quá ngắn, có thể chặn nhầm nhiều số không liên quan'
            : 'Số điện thoại không hợp lệ. Vui lòng kiểm tra lại độ dài số.',
      );
    }

    return withCountryCode;
  }

  void _assertAllDigits(String digits, EntryType entryType) {
    if (digits.isEmpty || !RegExp(r'^\d+$').hasMatch(digits)) {
      throw InvalidPhoneNumberFormatException(_formatErrorMessage(entryType));
    }
  }

  String _formatErrorMessage(EntryType entryType) {
    return entryType == EntryType.prefix
        ? 'Đầu số không hợp lệ. Chỉ nhập chữ số, có thể bắt đầu bằng dấu +, ví dụ: 0901 hoặc +84901'
        : 'Số điện thoại không hợp lệ. Vui lòng nhập đúng định dạng, ví dụ: 0901234567 hoặc +84901234567';
  }
}
