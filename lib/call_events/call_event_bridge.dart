import 'package:flutter/services.dart';

/// Su kien ScamPromptEvent nhan qua EventChannel (chi khi app foreground - "duong tang cuong",
/// KHONG phai nguon su that duy nhat, xem ghi chu lib/call_events).
class ScamPromptEvent {
  const ScamPromptEvent({required this.promptId, required this.normalizedNumber});

  final String promptId;
  final String normalizedNumber;

  factory ScamPromptEvent.fromWire(Map<Object?, Object?> raw) {
    return ScamPromptEvent(
      promptId: raw['promptId'] as String,
      normalizedNumber: raw['normalizedNumber'] as String,
    );
  }
}

/// Ket qua resolvePromptAtomically tra ve qua sendDialogResponse - khop enum ResolveOutcome ben Kotlin.
enum DialogResponseOutcome {
  success,
  alreadyResolved,
  writeFailed;

  static DialogResponseOutcome fromWireValue(String value) {
    switch (value) {
      case 'SUCCESS':
        return DialogResponseOutcome.success;
      case 'ALREADY_RESOLVED':
        return DialogResponseOutcome.alreadyResolved;
      case 'WRITE_FAILED':
        return DialogResponseOutcome.writeFailed;
      default:
        throw ArgumentError('DialogResponseOutcome không nhận diện được: $value');
    }
  }
}

/// Client cho "duong tang cuong" FR-2/FR-3/FR-4 (LLD: `CallEventBridge`) - lang nghe
/// `ScamPromptEvent` qua EventChannel va gui phan hoi qua MethodChannel. KHONG phai co che dam
/// bao - Notification he thong (native, [ScamConfirmationReceiver]) la kenh dam bao duy nhat.
class CallEventBridge {
  CallEventBridge({EventChannel? eventChannel, MethodChannel? methodChannel})
      : _eventChannel = eventChannel ?? const EventChannel(_eventChannelName),
        _methodChannel = methodChannel ?? const MethodChannel(_methodChannelName);

  static const String _eventChannelName = 'com.safecall/call_events';
  static const String _methodChannelName = 'com.safecall/call_events_response';

  final EventChannel _eventChannel;
  final MethodChannel _methodChannel;

  Stream<ScamPromptEvent> get callEndedStream {
    return _eventChannel.receiveBroadcastStream().map(
          (event) => ScamPromptEvent.fromWire(event as Map<Object?, Object?>),
        );
  }

  /// [action] la 'YES'/'NO'. Nem [PlatformException] khi loi ky thuat MethodChannel - UI (UIUX:
  /// "giữ dialog mở, hiển thị toast... cho bấm lại") PHAI tu bat va cho bam lai, KHONG tu dong lap lai o day.
  Future<DialogResponseOutcome> sendDialogResponse(String promptId, String action) async {
    final result = await _methodChannel.invokeMethod<String>(
      'sendDialogResponse',
      {'promptId': promptId, 'action': action},
    );
    if (result == null) {
      throw PlatformException(code: 'NULL_RESULT', message: 'Native trả về null');
    }
    return DialogResponseOutcome.fromWireValue(result);
  }
}
