import 'package:flutter/material.dart';

import 'call_event_bridge.dart';

/// Dialog in-app (UIUX: `ScamConfirmationDialog`) - kenh TANG CUONG cho FR-2/FR-3/FR-4, chi hien
/// khi app foreground. KHONG phai co che dam bao (xem call_event_bridge.dart).
class ScamConfirmationDialog extends StatefulWidget {
  const ScamConfirmationDialog({super.key, required this.event, required this.bridge});

  final ScamPromptEvent event;
  final CallEventBridge bridge;

  @override
  State<ScamConfirmationDialog> createState() => _ScamConfirmationDialogState();
}

class _ScamConfirmationDialogState extends State<ScamConfirmationDialog> {
  bool _sending = false;

  Future<void> _respond(String action) async {
    setState(() => _sending = true);
    try {
      final outcome = await widget.bridge.sendDialogResponse(widget.event.promptId, action);
      if (!mounted) return;
      switch (outcome) {
        case DialogResponseOutcome.success:
          Navigator.of(context).pop();
          _showToast(
            action == 'YES' ? 'Đã thêm số vào danh sách chặn' : 'Đã ghi nhận, sẽ không hỏi lại số này',
          );
          break;
        case DialogResponseOutcome.alreadyResolved:
          Navigator.of(context).pop();
          _showToast('Cuộc gọi này đã được xử lý trước đó');
          break;
        case DialogResponseOutcome.writeFailed:
          setState(() => _sending = false);
          _showToast('Không gửi được phản hồi, vui lòng thử lại');
          break;
      }
    } catch (_) {
      if (!mounted) return;
      setState(() => _sending = false);
      _showToast('Không gửi được phản hồi, vui lòng thử lại');
    }
  }

  void _showToast(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    // AC2 (FR-4): back/tap-ngoai dialog KHONG gui phan hoi gi - showDialog mac dinh da dung
    // (barrierDismissible=true, pop tu back button khong goi _respond).
    return AlertDialog(
      title: const Text('Đây có phải cuộc gọi lừa đảo không?'),
      content: Text('Số điện thoại: ${widget.event.normalizedNumber}'),
      actions: [
        TextButton(
          onPressed: _sending ? null : () => _respond('NO'),
          child: const Text('Không'),
        ),
        TextButton(
          onPressed: _sending ? null : () => _respond('YES'),
          child: const Text('Có'),
        ),
      ],
    );
  }
}

/// Lang nghe `callEndedStream` xuyen suot vong doi app, hien [ScamConfirmationDialog] khi co su
/// kien moi (chi khi app foreground - EventChannel chi phat khi co sink dang lang nghe).
///
/// Dung [navigatorKey] (khong phai context cua chinh widget nay) de goi showDialog - widget nay
/// dat trong `MaterialApp.builder`, tuc NAM PHIA TREN Navigator ma MaterialApp tu tao; context
/// cua chinh no se khong tim thay Navigator nao ben duoi.
class ScamPromptListener extends StatefulWidget {
  const ScamPromptListener({
    super.key,
    required this.child,
    required this.navigatorKey,
    this.bridge,
  });

  final Widget child;
  final GlobalKey<NavigatorState> navigatorKey;
  final CallEventBridge? bridge;

  @override
  State<ScamPromptListener> createState() => _ScamPromptListenerState();
}

class _ScamPromptListenerState extends State<ScamPromptListener> {
  late final CallEventBridge _bridge = widget.bridge ?? CallEventBridge();
  bool _dialogShowing = false;

  @override
  void initState() {
    super.initState();
    _bridge.callEndedStream.listen(_onEvent);
  }

  void _onEvent(ScamPromptEvent event) {
    if (_dialogShowing) return; // Mot dialog mot luc - prompt moi (so khac) se cho prompt hien tai dong truoc.
    final context = widget.navigatorKey.currentContext;
    if (context == null) return;
    _dialogShowing = true;
    showDialog<void>(
      context: context,
      builder: (context) => ScamConfirmationDialog(event: event, bridge: _bridge),
    ).whenComplete(() => _dialogShowing = false);
  }

  @override
  Widget build(BuildContext context) => widget.child;
}
