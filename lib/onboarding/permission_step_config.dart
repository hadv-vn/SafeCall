import '../permissions/permission_keys.dart';

/// Mo ta 1 buoc "Rationale" trong luong Onboarding/Permission (UIUX-SafeCall-Phase1.md, muc
/// "Man hinh: Onboarding + Permission"). Mot buoc co the gom nhieu quyen Android cung xin lien
/// tiep nhung chi hien THI MOT man hinh giai thich (vd "Trang thai/nhat ky cuoc goi" gom ca
/// READ_PHONE_STATE va READ_CALL_LOG).
class PermissionStepConfig {
  const PermissionStepConfig({
    required this.id,
    required this.title,
    required this.rationale,
    required this.permissionKeys,
    required this.mandatory,
    this.isRole = false,
    this.isSettingsRedirect = false,
  });

  final String id;
  final String title;
  final String rationale;
  final List<String> permissionKeys;
  final bool mandatory;
  final bool isRole;

  /// (Mới - Phase 6/FR-8/ADR-007) true CHỈ cho `SYSTEM_ALERT_WINDOW` - xin qua màn hình Settings
  /// riêng (`ACTION_MANAGE_OVERLAY_PERMISSION`), KHÔNG có callback đồng bộ trả kết quả như quyền
  /// runtime thường - luồng UI khác hẳn (xem `OnboardingFlowScreen._FlowPhase.settingsPending`).
  final bool isSettingsRedirect;
}

const PermissionStepConfig kRoleStep = PermissionStepConfig(
  id: 'role_call_screening',
  title: 'Vai trò chặn gọi',
  rationale:
      'SafeCall cần Vai trò chặn gọi để tự động chặn số lừa đảo ngay khi gọi đến, '
      'không cần bạn làm gì thêm.',
  permissionKeys: [],
  mandatory: true,
  isRole: true,
);

const PermissionStepConfig kCallStateStep = PermissionStepConfig(
  id: 'call_state_and_log',
  title: 'Trạng thái và nhật ký cuộc gọi',
  rationale:
      'SafeCall cần quyền này để phát hiện khi một cuộc gọi đến kết thúc, hỏi xác nhận có phải '
      'lừa đảo không, và ghi lại lịch sử cuộc gọi.',
  permissionKeys: [kPermissionReadPhoneState, kPermissionReadCallLog],
  mandatory: true,
);

const PermissionStepConfig kNotificationsStep = PermissionStepConfig(
  id: 'notifications',
  title: 'Thông báo',
  rationale:
      'SafeCall cần quyền Thông báo để hiển thị câu hỏi xác nhận lừa đảo kể cả khi ứng dụng '
      'đang ở nền hoặc đã bị tắt.',
  permissionKeys: [kPermissionPostNotifications],
  mandatory: true,
);

/// (Mới - Phase 6/FR-8/ADR-007) Bắt buộc cho TOÀN BỘ FR-8 - không có nhánh suy giảm như các
/// quyền tùy chọn khác. Không có "Để sau" (xem OnboardingFlowScreen - chỉ ẩn nút Để sau khi
/// !mandatory, bước này mandatory=true).
const PermissionStepConfig kSystemAlertWindowStep = PermissionStepConfig(
  id: 'system_alert_window',
  title: 'Hiển thị đè lên ứng dụng khác',
  rationale:
      'Đây là một quyền đặc biệt của Android. Khi bạn bấm "Mở Cài đặt", bạn sẽ được chuyển sang '
      'một màn hình Cài đặt riêng của máy để tự tay bật quyền này. SafeCall cần quyền này để hiện '
      'ngay cảnh báo lừa đảo đè lên màn hình bạn đang dùng, đúng lúc một cuộc gọi lừa đảo đang đổ '
      'chuông. Nếu không cấp quyền này, tính năng cảnh báo lúc đang đổ chuông sẽ KHÔNG hoạt động '
      'dưới bất kỳ hình thức nào.',
  permissionKeys: [kPermissionSystemAlertWindow],
  mandatory: true,
  isSettingsRedirect: true,
);

const PermissionStepConfig kContactsStep = PermissionStepConfig(
  id: 'contacts',
  title: 'Danh bạ',
  rationale:
      'SafeCall dùng Danh bạ để không hỏi xác nhận với số đã có trong danh bạ của bạn và cảnh '
      'báo nếu bạn vô tình chặn một số đã lưu. Nếu không cấp, mọi số gọi đến sẽ được coi là số lạ.',
  permissionKeys: [kPermissionReadContacts],
  mandatory: false,
);

/// (Mới - Phase 6/FR-8/ADR-005) Tùy chọn - thiếu quyền này overlay vẫn hiện và vẫn chặn được số
/// cho lần sau, chỉ không tự ngắt được cuộc gọi hiện tại (outcome NOT_APPLICABLE_STATE_CHANGED).
const PermissionStepConfig kAnswerPhoneCallsStep = PermissionStepConfig(
  id: 'answer_phone_calls',
  title: 'Ngắt cuộc gọi lừa đảo ngay khi đang đổ chuông',
  rationale:
      'SafeCall cần quyền này để tự động từ chối cuộc gọi ngay khi bạn xác nhận đó là lừa đảo '
      'trong lúc máy còn đang đổ chuông. Nếu không cấp quyền này, SafeCall vẫn thêm số vào danh '
      'sách chặn khi bạn xác nhận, nhưng sẽ KHÔNG thể tự ngắt cuộc gọi hiện tại.',
  permissionKeys: [kPermissionAnswerPhoneCalls],
  mandatory: false,
);

/// Thu tu buoc rationale theo UIUX (nguyen tac "bat buoc nhat truoc", SYSTEM_ALERT_WINDOW ngay
/// sau 3 quyen bat buoc dau, truoc 2 quyen tuy chon).
const List<PermissionStepConfig> kOnboardingSteps = <PermissionStepConfig>[
  kRoleStep,
  kCallStateStep,
  kNotificationsStep,
  kSystemAlertWindowStep,
  kContactsStep,
  kAnswerPhoneCallsStep,
];
