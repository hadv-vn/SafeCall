import 'package:flutter/services.dart';

import 'permission_keys.dart';

/// Nem ra khi native tra loi ROLE_UNAVAILABLE/REQUEST_IN_PROGRESS/UNKNOWN_PERMISSION -
/// khac voi tu choi quyen binh thuong (DENIED van la ket qua thanh cong ve mat giao thuc).
class PermissionsBridgeException implements Exception {
  PermissionsBridgeException(this.code, this.message);

  final String code;
  final String? message;

  @override
  String toString() => 'PermissionsBridgeException($code): $message';
}

/// Client MethodChannel cho quyen/role (LLD: `PermissionsBridge`) - KHONG tu quyet dinh logic
/// quyen (vd khong tu suy ra "da du quyen bat buoc chua"), chi forward 1-1 sang native va tra
/// ve ket qua da parse. Logic nghiep vu (phan loai bat buoc/tuy chon, dieu huong man hinh) thuoc
/// OnboardingScreen/PermissionScreen (Task 1.2.1).
class PermissionsBridge {
  PermissionsBridge({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(_channelName);

  static const String _channelName = 'com.safecall/permissions';

  final MethodChannel _channel;

  Future<bool> checkRoleStatus() async {
    final result = await _invoke<bool>('checkRoleStatus');
    return result;
  }

  Future<bool> requestRole() async {
    final result = await _invoke<bool>('requestRole');
    return result;
  }

  Future<PermissionStatus> checkPermissionStatus(String permissionKey) async {
    final result = await _invoke<String>(
      'checkPermissionStatus',
      {'permission': permissionKey},
    );
    return PermissionStatus.fromWireValue(result);
  }

  Future<PermissionStatus> requestPermission(String permissionKey) async {
    final result = await _invoke<String>(
      'requestPermission',
      {'permission': permissionKey},
    );
    return PermissionStatus.fromWireValue(result);
  }

  Future<Map<String, PermissionStatus>> checkAllPermissionsStatus() async {
    final raw = await _invoke<Map<Object?, Object?>>('checkAllPermissionsStatus');
    return raw.map(
      (key, value) => MapEntry(
        key as String,
        PermissionStatus.fromWireValue(value as String),
      ),
    );
  }

  Future<T> _invoke<T>(String method, [Map<String, dynamic>? arguments]) async {
    try {
      final result = await _channel.invokeMethod<T>(method, arguments);
      if (result == null) {
        throw PermissionsBridgeException('NULL_RESULT', 'Native tra ve null cho $method');
      }
      return result;
    } on PlatformException catch (e) {
      throw PermissionsBridgeException(e.code, e.message);
    }
  }
}
