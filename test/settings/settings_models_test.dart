import 'package:flutter_test/flutter_test.dart';
import 'package:safecall/settings/settings_models.dart';

void main() {
  group('SettingsFlags.defaults', () {
    test('contacts priority ON, 3 block-toggle OFF (ADR-008)', () {
      const flags = SettingsFlags.defaults();
      expect(flags.contactsPriorityEnabled, isTrue);
      expect(flags.blockUnknownEnabled, isFalse);
      expect(flags.blockAnonymousEnabled, isFalse);
      expect(flags.blockInternationalEnabled, isFalse);
    });
  });

  group('SettingsFlags.fromRow', () {
    test('maps 1/0 integer columns to booleans', () {
      final flags = SettingsFlags.fromRow({
        'id': 1,
        'contacts_priority_enabled': 1,
        'block_unknown_enabled': 0,
        'block_anonymous_enabled': 1,
        'block_international_enabled': 0,
      });
      expect(flags.contactsPriorityEnabled, isTrue);
      expect(flags.blockUnknownEnabled, isFalse);
      expect(flags.blockAnonymousEnabled, isTrue);
      expect(flags.blockInternationalEnabled, isFalse);
    });
  });

  group('SettingsFlags.valueOf / withValue', () {
    test('valueOf reads the flag matching SettingsFlag', () {
      const flags = SettingsFlags.defaults();
      expect(flags.valueOf(SettingsFlag.contactsPriority), isTrue);
      expect(flags.valueOf(SettingsFlag.blockUnknown), isFalse);
      expect(flags.valueOf(SettingsFlag.blockAnonymous), isFalse);
      expect(flags.valueOf(SettingsFlag.blockInternational), isFalse);
    });

    test('withValue updates only the targeted flag, leaves others untouched', () {
      const flags = SettingsFlags.defaults();
      final updated = flags.withValue(SettingsFlag.blockUnknown, true);

      expect(updated.blockUnknownEnabled, isTrue);
      expect(updated.contactsPriorityEnabled, flags.contactsPriorityEnabled);
      expect(updated.blockAnonymousEnabled, flags.blockAnonymousEnabled);
      expect(updated.blockInternationalEnabled, flags.blockInternationalEnabled);
    });

    test('withValue round-trips through valueOf for every flag', () {
      const flags = SettingsFlags.defaults();
      for (final flag in SettingsFlag.values) {
        final toggled = flags.withValue(flag, !flags.valueOf(flag));
        expect(toggled.valueOf(flag), !flags.valueOf(flag));
      }
    });
  });

  group('SettingsFlag.columnName', () {
    test('matches blocklist_meta column names from ADR-008', () {
      expect(SettingsFlag.contactsPriority.columnName, 'contacts_priority_enabled');
      expect(SettingsFlag.blockUnknown.columnName, 'block_unknown_enabled');
      expect(SettingsFlag.blockAnonymous.columnName, 'block_anonymous_enabled');
      expect(SettingsFlag.blockInternational.columnName, 'block_international_enabled');
    });
  });
}
