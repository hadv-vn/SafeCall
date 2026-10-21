import 'package:flutter/services.dart';

/// Client MethodChannel cho tra cuu Danh ba (FR-5/AC8: canh bao truoc khi chan mot contact).
/// Day la bridge moi tu phat sinh khi implement (xem ghi chu trong ContactsLookupModule.kt) -
/// LLD khong dac ta ten/channel cu the cho nhu cau nay.
class ContactsBridge {
  ContactsBridge({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(_channelName);

  static const String _channelName = 'com.safecall/contacts';

  final MethodChannel _channel;

  Future<bool> isNumberInContacts(String normalizedNumber) async {
    final result = await _channel.invokeMethod<bool>(
      'isNumberInContacts',
      {'normalizedNumber': normalizedNumber},
    );
    return result ?? false;
  }

  /// FR-9/AC5 - dem so lien he co so khop theo quy tac prefix voi [normalizedPrefix].
  Future<int> countContactsMatchingPrefix(String normalizedPrefix) async {
    final result = await _channel.invokeMethod<int>(
      'countContactsMatchingPrefix',
      {'normalizedPrefix': normalizedPrefix},
    );
    return result ?? 0;
  }
}
