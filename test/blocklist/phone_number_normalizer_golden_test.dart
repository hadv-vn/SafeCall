import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:safecall/blocklist/blocklist_models.dart';
import 'package:safecall/blocklist/phone_number_normalizer.dart';

/// Golden-file test doi chieu voi ban Kotlin
/// (android/app/src/test/kotlin/com/safecall/callscreening/PhoneNumberNormalizerGoldenTest.kt) -
/// dong Risk R9 (docs/governance/RISK-SAFECALL.md). Ca 2 phia doc CUNG 1 fixture
/// (test/fixtures/phone_normalizer_golden.json, mirror thu cong tai android/app/src/test/resources/).
void main() {
  final fixtureFile = File('test/fixtures/phone_normalizer_golden.json');
  final fixture =
      jsonDecode(fixtureFile.readAsStringSync()) as Map<String, dynamic>;
  final cases = (fixture['cases'] as List<dynamic>).cast<Map<String, dynamic>>();
  final normalizer = PhoneNumberNormalizer();

  group('PhoneNumberNormalizer golden file (dau so nha mang Viet Nam thuc)', () {
    for (final c in cases) {
      final id = c['id'] as String;
      final raw = c['raw'] as String;
      final entryType = EntryType.fromWireValue(c['entryType'] as String);
      final expectThrows = c['expectThrows'] as bool? ?? false;

      test(id, () {
        if (expectThrows) {
          expect(
            () => normalizer.validateAndNormalize(raw, entryType: entryType),
            throwsA(isA<InvalidPhoneNumberFormatException>()),
          );
        } else {
          final expected = c['expect'] as String;
          expect(
            normalizer.validateAndNormalize(raw, entryType: entryType),
            expected,
          );
        }
      });
    }
  });
}
