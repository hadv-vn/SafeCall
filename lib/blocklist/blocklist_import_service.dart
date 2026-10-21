import 'dart:convert';

import 'package:file_picker/file_picker.dart';

import 'blocklist_export_import_models.dart';
import 'blocklist_models.dart';
import 'blocklist_service.dart';
import 'phone_number_normalizer.dart';

/// Import blocklist tu file JSON (LLD: `BlocklistImportService`, pseudocode E7, FR-10/AC4-AC11).
/// Validate TOAN BO entry TRUOC (chi doc), roi ghi trong 1 transaction SQL DUY NHAT qua
/// `BlocklistService.mergeEntriesInTransaction` - tan dung ACID SQLite, khong can rollback tu viet.
///
/// Quyet dinh (dung nguyen LLD pseudocode E7, khong phai gia dinh moi): LUON ghi de `source` thanh
/// `IMPORTED`, KHONG giu nguyen gia tri trong file - ERD tu ghi "entries[].source: chỉ tham khảo,
/// KHÔNG tin trực tiếp".
class BlocklistImportService {
  BlocklistImportService({
    BlocklistService? blocklistService,
    PhoneNumberNormalizer? normalizer,
  })  : _blocklistService = blocklistService ?? BlocklistService(),
        _normalizer = normalizer ?? PhoneNumberNormalizer();

  final BlocklistService _blocklistService;
  final PhoneNumberNormalizer _normalizer;

  Future<ImportResult> importBlocklist() async {
    final picked = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['json'],
      withData: true,
    );
    final bytes = picked?.files.single.bytes;
    if (bytes == null) {
      return const ImportResult(ImportOutcome.cancelled);
    }

    Map<String, dynamic> parsed;
    try {
      final decoded = jsonDecode(utf8.decode(bytes));
      if (decoded is! Map<String, dynamic> ||
          !decoded.containsKey('schema_version') ||
          !decoded.containsKey('entries') ||
          decoded['entries'] is! List) {
        return const ImportResult(ImportOutcome.invalidFile);
      }
      parsed = decoded;
    } catch (_) {
      return const ImportResult(ImportOutcome.invalidFile);
    }

    final schemaVersion = parsed['schema_version'];
    if (schemaVersion is! int) {
      return const ImportResult(ImportOutcome.invalidFile);
    }
    if (schemaVersion > kBlocklistSchemaVersion) {
      return const ImportResult(ImportOutcome.unsupportedNewerVersion);
    }

    final rawEntries = parsed['entries'] as List;
    final validEntries = <ValidatedBlocklistEntry>[];
    var skippedInvalidCount = 0;
    for (final raw in rawEntries) {
      if (raw is! Map) {
        skippedInvalidCount++;
        continue;
      }
      final entryType = _parseEntryTypeOrNull(raw['entry_type']);
      final rawValue = raw['value'];
      if (entryType == null || rawValue is! String) {
        skippedInvalidCount++;
        continue;
      }
      try {
        final normalizedValue = _normalizer.validateAndNormalize(rawValue, entryType: entryType);
        validEntries.add(
          ValidatedBlocklistEntry(
            normalizedValue: normalizedValue,
            entryType: entryType,
            source: BlocklistSource.imported,
          ),
        );
      } on InvalidPhoneNumberFormatException {
        skippedInvalidCount++;
      }
    }

    final existing = await _blocklistService.getAll();
    final toInsert = validEntries.where((e) {
      return !existing.any(
        (ex) => ex.normalizedNumber == e.normalizedValue && ex.entryType == e.entryType,
      );
    }).toList();
    final skippedDuplicateCount = validEntries.length - toInsert.length;

    if (toInsert.isEmpty) {
      return ImportResult(
        ImportOutcome.success,
        skippedDuplicate: skippedDuplicateCount,
        skippedInvalid: skippedInvalidCount,
      );
    }

    try {
      await _blocklistService.mergeEntriesInTransaction(toInsert);
    } catch (_) {
      return ImportResult(ImportOutcome.writeError, skippedInvalid: skippedInvalidCount);
    }

    return ImportResult(
      ImportOutcome.success,
      added: toInsert.length,
      skippedDuplicate: skippedDuplicateCount,
      skippedInvalid: skippedInvalidCount,
    );
  }

  EntryType? _parseEntryTypeOrNull(Object? raw) {
    if (raw == 'EXACT') return EntryType.exact;
    if (raw == 'PREFIX') return EntryType.prefix;
    return null;
  }
}
