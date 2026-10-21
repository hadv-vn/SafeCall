/// Khoa quyen dung chung giua Dart va Kotlin - PHAI khop tuyet doi voi
/// android/.../callscreening/PermissionKeys.kt.
library;

const String kPermissionReadPhoneState = 'READ_PHONE_STATE';
const String kPermissionReadCallLog = 'READ_CALL_LOG';
const String kPermissionReadContacts = 'READ_CONTACTS';
const String kPermissionPostNotifications = 'POST_NOTIFICATIONS';

/// (Mới - Phase 6/FR-8/ADR-007) Bắt buộc cho toàn bộ FR-8 - xin qua Settings
/// (`ACTION_MANAGE_OVERLAY_PERMISSION`), KHÔNG qua dialog runtime thường.
const String kPermissionSystemAlertWindow = 'SYSTEM_ALERT_WINDOW';

/// (Mới - Phase 6/FR-8/ADR-005) Tùy chọn (suy giảm nhẹ nhàng) - chỉ ảnh hưởng nhánh ngắt cuộc gọi.
const String kPermissionAnswerPhoneCalls = 'ANSWER_PHONE_CALLS';

/// Khop enum PermissionStatus ben Kotlin (toString()).
enum PermissionStatus {
  granted,
  denied,
  notRequested;

  static PermissionStatus fromWireValue(String value) {
    switch (value) {
      case 'GRANTED':
        return PermissionStatus.granted;
      case 'DENIED':
        return PermissionStatus.denied;
      case 'NOT_REQUESTED':
        return PermissionStatus.notRequested;
      default:
        throw ArgumentError('Gia tri PermissionStatus khong hop le: $value');
    }
  }
}
