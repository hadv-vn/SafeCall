import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

import 'safecall_schema.dart';

/// Mo/tao CHUNG mot file SQLite voi phia native (xem [kDatabaseFileName]) - singleton don gian,
/// dung bang BlocklistService/CallHistoryService/BlocklistExportService/BlocklistImportService.
class DatabaseProvider {
  DatabaseProvider._();

  static final DatabaseProvider instance = DatabaseProvider._();

  Database? _database;

  Future<Database> get database async {
    final existing = _database;
    if (existing != null) return existing;
    final opened = await _open();
    _database = opened;
    return opened;
  }

  Future<Database> _open() async {
    final databasesPath = await getDatabasesPath();
    final path = p.join(databasesPath, kDatabaseFileName);
    return openDatabase(
      path,
      version: kSchemaVersion,
      onConfigure: (db) async {
        // Sua loi phat hien khi test tren thiet bi that: sqflite tu dong boc onCreate trong 1
        // transaction (BEGIN...COMMIT) - PRAGMA journal_mode/busy_timeout/foreign_keys deu KHONG
        // duoc phep (hoac bi am tham bo qua) khi dang trong transaction. PHAI dat o onConfigure
        // (chay TRUOC moi transaction, moi lan mo connection) - khong duoc dat trong onCreate.
        await db.execute('PRAGMA journal_mode = WAL');
        await db.execute('PRAGMA synchronous = NORMAL');
        await db.execute('PRAGMA busy_timeout = $kPragmaBusyTimeoutOtherMs');
        await db.execute('PRAGMA foreign_keys = ON');
      },
      onCreate: (db, version) async {
        for (final statement in createStatements) {
          await db.execute(statement);
        }
        await db.insert('schema_migrations', {
          'version': kSchemaVersion,
          'applied_at': DateTime.now().millisecondsSinceEpoch,
          'applied_by': 'DART',
        });
      },
      onUpgrade: (db, oldVersion, newVersion) async {
        // Chua co migration nao vuot qua v1 trong Phase 1 - placeholder cho Phase sau.
      },
    );
  }
}
