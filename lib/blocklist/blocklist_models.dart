/// Loai entry blocklist (LLD: `EntryType`, mo rong v1.3/FR-9) - EXACT (so cu the, FR-5) hoac
/// PREFIX (dau so, FR-9).
enum EntryType {
  exact,
  prefix;

  String get wireValue => switch (this) {
        EntryType.exact => 'EXACT',
        EntryType.prefix => 'PREFIX',
      };

  static EntryType fromWireValue(String value) {
    switch (value) {
      case 'EXACT':
        return EntryType.exact;
      case 'PREFIX':
        return EntryType.prefix;
      default:
        throw ArgumentError('EntryType không nhận diện được: $value');
    }
  }
}

/// Nguon them so (LLD: khop `BlocklistEntryDto.source`/`BlocklistSource`).
enum BlocklistSource {
  manual,
  autoConfirmed,
  imported;

  String get wireValue => switch (this) {
        BlocklistSource.manual => 'MANUAL',
        BlocklistSource.autoConfirmed => 'AUTO_CONFIRMED',
        BlocklistSource.imported => 'IMPORTED',
      };

  static BlocklistSource fromWireValue(String value) {
    switch (value) {
      case 'MANUAL':
        return BlocklistSource.manual;
      case 'AUTO_CONFIRMED':
        return BlocklistSource.autoConfirmed;
      case 'IMPORTED':
        return BlocklistSource.imported;
      default:
        throw ArgumentError('BlocklistSource không nhận diện được: $value');
    }
  }
}

/// Entry doc duoc tu bang `blocklist` (mo rong v1.3/FR-9: them [entryType]).
class BlocklistEntryDto {
  const BlocklistEntryDto({
    required this.id,
    required this.normalizedNumber,
    required this.entryType,
    required this.source,
    required this.addedAt,
  });

  final int id;
  final String normalizedNumber;
  final EntryType entryType;
  final BlocklistSource source;
  final DateTime addedAt;

  factory BlocklistEntryDto.fromRow(Map<String, Object?> row) {
    return BlocklistEntryDto(
      id: row['id'] as int,
      normalizedNumber: row['normalized_number'] as String,
      entryType: EntryType.fromWireValue(row['entry_type'] as String),
      source: BlocklistSource.fromWireValue(row['source'] as String),
      addedAt: DateTime.fromMillisecondsSinceEpoch(row['added_at'] as int),
    );
  }
}

/// Khop FR-5/AC3-AC5, FR-9/AC1-AC4: ket qua cua addNumber() - khong nem exception cho cac nhanh
/// nghiep vu binh thuong (format sai, gom ca "dau so qua ngan" / da ton tai), chi nem cho loi ky
/// thuat thuc su.
enum AddNumberOutcome { success, invalidFormat, alreadyExists }

class AddNumberResult {
  const AddNumberResult(this.outcome, {this.errorMessage});

  final AddNumberOutcome outcome;
  final String? errorMessage;
}

/// Entry DA validate/chuan hoa, san sang ghi vao DB (LLD: `ValidatedEntry`) - dung boi
/// `BlocklistImportService`/`BlocklistService.mergeEntriesInTransaction` (FR-10, Phase 7). KHAC
/// voi du lieu tho tu file import (`value`/`entryType` dang String CHUA DUOC TIN).
class ValidatedBlocklistEntry {
  const ValidatedBlocklistEntry({
    required this.normalizedValue,
    required this.entryType,
    required this.source,
  });

  final String normalizedValue;
  final EntryType entryType;
  final BlocklistSource source;
}
