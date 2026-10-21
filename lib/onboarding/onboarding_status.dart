import '../permissions/permission_keys.dart';
import '../permissions/permissions_bridge.dart';
import 'permission_step_config.dart';

/// Gop checkRoleStatus() + checkAllPermissionsStatus() thanh 1 snapshot, va cung cap logic suy
/// ra "da cap du quyen bat buoc chua" (AC3) - day la logic nghiep vu cua OnboardingScreen, KHONG
/// thuoc PermissionsBridge (bridge chi forward 1-1, khong tu quyet dinh - xem permissions_bridge.dart).
class OnboardingStatusSnapshot {
  OnboardingStatusSnapshot({required this.roleGranted, required this.permissionStatuses});

  final bool roleGranted;
  final Map<String, PermissionStatus> permissionStatuses;

  bool isGranted(String permissionKey) =>
      permissionStatuses[permissionKey] == PermissionStatus.granted;

  /// AC3: da cap du quyen BAT BUOC (role + cac buoc mandatory) -> bo qua toan bo onboarding.
  bool get hasAllMandatoryGranted {
    if (!roleGranted) return false;
    for (final step in kOnboardingSteps) {
      if (!step.mandatory || step.isRole) continue;
      for (final key in step.permissionKeys) {
        if (!isGranted(key)) return false;
      }
    }
    return true;
  }

  /// Dung cho resume-check (AC4): tim buoc bat buoc dau tien bi thu hoi, hoac null neu con du.
  PermissionStepConfig? firstRevokedMandatoryStep() {
    if (!roleGranted) return kRoleStep;
    for (final step in kOnboardingSteps) {
      if (!step.mandatory || step.isRole) continue;
      final anyMissing = step.permissionKeys.any((key) => !isGranted(key));
      if (anyMissing) return step;
    }
    return null;
  }

  static Future<OnboardingStatusSnapshot> load(PermissionsBridge bridge) async {
    final role = await bridge.checkRoleStatus();
    final statuses = await bridge.checkAllPermissionsStatus();
    return OnboardingStatusSnapshot(roleGranted: role, permissionStatuses: statuses);
  }
}
