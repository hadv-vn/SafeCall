import 'package:flutter/material.dart';

import '../permissions/permission_keys.dart';
import '../permissions/permissions_bridge.dart';

/// Banner co dinh canh bao thieu quyen Danh ba (UIUX buoc 5, AC5 FR-7) - dismissible nhung quay
/// lai neu con chua cap. Hien thi xuyen suot app (Home/BlocklistScreen/CallHistoryScreen) - o
/// Phase 1 nay chi gan vao Home placeholder, cac man hinh Phase 2/5 se tai su dung widget nay.
class ContactsDeniedBanner extends StatefulWidget {
  const ContactsDeniedBanner({super.key, required this.bridge});

  final PermissionsBridge bridge;

  @override
  State<ContactsDeniedBanner> createState() => _ContactsDeniedBannerState();
}

class _ContactsDeniedBannerState extends State<ContactsDeniedBanner> {
  bool _dismissed = false;
  PermissionStatus? _status;

  @override
  void initState() {
    super.initState();
    _refresh();
  }

  Future<void> _refresh() async {
    final status = await widget.bridge.checkPermissionStatus(kPermissionReadContacts);
    if (!mounted) return;
    setState(() {
      _status = status;
      // Quyen van chua cap -> banner "quay lai" du da tung dismiss truoc do (UIUX: "dismissible
      // nhung quay lai neu con chua cap").
      if (status != PermissionStatus.granted) _dismissed = false;
    });
  }

  Future<void> _onGrantPressed() async {
    await widget.bridge.requestPermission(kPermissionReadContacts);
    await _refresh();
  }

  @override
  Widget build(BuildContext context) {
    if (_status == null || _status == PermissionStatus.granted || _dismissed) {
      return const SizedBox.shrink();
    }
    return Material(
      color: Theme.of(context).colorScheme.errorContainer,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        child: Row(
          children: [
            Icon(Icons.warning_amber, color: Theme.of(context).colorScheme.onErrorContainer),
            const SizedBox(width: 8),
            Expanded(
              child: Semantics(
                liveRegion: true,
                child: Text(
                  'Bạn chưa cấp quyền Danh bạ — mọi số gọi đến đều được coi là số lạ.',
                  style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer),
                ),
              ),
            ),
            TextButton(onPressed: _onGrantPressed, child: const Text('Cấp quyền')),
            IconButton(
              icon: const Icon(Icons.close),
              onPressed: () => setState(() => _dismissed = true),
            ),
          ],
        ),
      ),
    );
  }
}
