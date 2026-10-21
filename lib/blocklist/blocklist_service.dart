import 'package:sqflite/sqflite.dart';

import '../db/database_provider.dart';
import 'blocklist_models.dart';
import 'phone_number_normalizer.dart';

/// CRUD blocklist qua sqflite (LLD: `BlocklistService`). Mo rong v1.3 (FR-9/7.1.1): ho tro ca
/// EXACT va PREFIX qua tham so [EntryType]; mo rong (FR-10/7.4.x): `mergeEntriesInTransaction`
/// cho Import.
///
/// Quyet dinh quan trong (LLD muc "Luong 3"): INSERT TRUC TIEP + bat loi UNIQUE, KHONG
/// SELECT-roi-INSERT - tranh race TOCTOU voi `ScamConfirmationReceiver` (native) ghi dong thoi
/// vao cung bang `blocklist` (FR-3/AC2, FR-9 dung CUNG co che).
class BlocklistService {
  BlocklistService({
    DatabaseProvider? databaseProvider,
    PhoneNumberNormalizer? normalizer,
  })  : _databaseProvider = databaseProvider ?? DatabaseProvider.instance,
        _normalizer = normalizer ?? PhoneNumberNormalizer();

  final DatabaseProvider _databaseProvider;
  final PhoneNumberNormalizer _normalizer;

  /// FR-5/AC1, FR-9/AC6: tra CA EXACT va PREFIX, sap xep added_at DESC.
  Future<List<BlocklistEntryDto>> getAll() async {
    final db = await _databaseProvider.database;
    final rows = await db.query('blocklist', orderBy: 'added_at DESC');
    return rows.map(BlocklistEntryDto.fromRow).toList(growable: false);
  }

  /// FR-5/AC3-AC5, FR-9/AC1-AC4: khong nem exception cho "sai dinh dang"/"da ton tai" - day la
  /// nhanh nghiep vu binh thuong, tra ve qua [AddNumberResult]. Chi loi ky thuat thuc su (DB loi
  /// khac UNIQUE) moi duoc nem tiep len tren.
  Future<AddNumberResult> addNumber(
    String rawInput, {
    EntryType entryType = EntryType.exact,
    BlocklistSource source = BlocklistSource.manual,
  }) async {
    String normalized;
    try {
      normalized = _normalizer.validateAndNormalize(rawInput, entryType: entryType);
    } on InvalidPhoneNumberFormatException catch (e) {
      return AddNumberResult(AddNumberOutcome.invalidFormat, errorMessage: e.message);
    }

    final db = await _databaseProvider.database;
    try {
      await db.insert('blocklist', {
        'normalized_number': normalized,
        'entry_type': entryType.wireValue,
        'source': source.wireValue,
        'added_at': DateTime.now().millisecondsSinceEpoch,
      });
      return const AddNumberResult(AddNumberOutcome.success);
    } on DatabaseException catch (e) {
      if (e.isUniqueConstraintError()) {
        return AddNumberResult(
          AddNumberOutcome.alreadyExists,
          errorMessage: entryType == EntryType.prefix
              ? 'Đầu số này đã có trong danh sách chặn'
              : 'Số này đã có trong danh sách chặn.',
        );
      }
      rethrow;
    }
  }

  /// FR-5/AC6, FR-9/AC7: xoa theo id - AC7/AC8 (huy truoc khi xac nhan) la trach nhiem cua UI
  /// (khong goi method nay cho toi khi nguoi dung xac nhan xoa).
  Future<void> removeNumber(int id) async {
    final db = await _databaseProvider.database;
    await db.delete('blocklist', where: 'id = ?', whereArgs: [id]);
  }

  /// FR-10/AC10 - goi boi `BlocklistImportService` voi danh sach entry DA validate. 1 transaction
  /// SQL DUY NHAT (`INSERT OR IGNORE` moi dong) - tan dung ACID co san cua SQLite, khong can
  /// rollback tu viet (LLD muc "Co che nhat quan khi import gian doan").
  Future<void> mergeEntriesInTransaction(List<ValidatedBlocklistEntry> entries) async {
    final db = await _databaseProvider.database;
    await db.transaction((txn) async {
      for (final entry in entries) {
        await txn.insert(
          'blocklist',
          {
            'normalized_number': entry.normalizedValue,
            'entry_type': entry.entryType.wireValue,
            'source': entry.source.wireValue,
            'added_at': DateTime.now().millisecondsSinceEpoch,
          },
          conflictAlgorithm: ConflictAlgorithm.ignore,
        );
      }
    });
  }
}
