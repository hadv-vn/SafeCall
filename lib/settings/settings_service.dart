import '../db/database_provider.dart';
import 'settings_models.dart';

/// Doc/viet 4 co FR-11/FR-12 qua sqflite vao `blocklist_meta` (LLD: `SettingsService`, ADR-008).
/// Dung CHUNG file SQLite voi `BlocklistService` - khong qua SharedPreferences (ADR-008 quyet
/// dinh 1, tranh khong nhat quan cross-process voi `CallScreeningService`).
class SettingsService {
  SettingsService({DatabaseProvider? databaseProvider})
      : _databaseProvider = databaseProvider ?? DatabaseProvider.instance;

  final DatabaseProvider _databaseProvider;

  /// FR-11/AC1, FR-12/AC1 - point-read singleton `blocklist_meta` (id=1), hang luon ton tai tu
  /// `createStatements`/migration. Khong fallback ngam dinh khi loi - de exception noi len tren
  /// cho UI hien thi dung trang thai Error (khac fail-open o tang native Kotlin, chi ap dung khi
  /// DOC DE RA QUYET DINH CHAN, khong ap dung cho hien thi UI - UIUX muc "Trang thai man hinh").
  Future<SettingsFlags> getFlags() async {
    final db = await _databaseProvider.database;
    final rows = await db.query('blocklist_meta', where: 'id = 1', limit: 1);
    return SettingsFlags.fromRow(rows.first);
  }

  /// FR-11/AC5, FR-12/AC8 - doi 1 co, ap dung ngay, khong co nut Luu rieng.
  Future<void> setFlag(SettingsFlag flag, bool value) async {
    final db = await _databaseProvider.database;
    await db.update(
      'blocklist_meta',
      {flag.columnName: value ? 1 : 0},
      where: 'id = 1',
    );
  }
}
