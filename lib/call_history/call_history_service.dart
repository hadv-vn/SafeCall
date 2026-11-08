import '../db/database_provider.dart';
import 'call_history_models.dart';

/// Doc lich su cuoc goi, chi doc, co phan trang (LLD: `CallHistoryService`, `getHistory(limit,
/// offset)`, `deriveDisplayStatus(entry)`).
class CallHistoryService {
  CallHistoryService({DatabaseProvider? databaseProvider})
      : _databaseProvider = databaseProvider ?? DatabaseProvider.instance;

  final DatabaseProvider _databaseProvider;

  Future<List<CallHistoryEntryDto>> getHistory({required int limit, required int offset}) async {
    final db = await _databaseProvider.database;
    final rows = await db.query(
      'call_history',
      orderBy: 'ended_at DESC',
      limit: limit,
      offset: offset,
    );
    return rows.map(CallHistoryEntryDto.fromRow).toList(growable: false);
  }

  /// Suy ra trang thai hien thi, dung nguyen van pseudocode E8 cua LLD v1.3 (`deriveDisplayStatus`)
  /// - thu tu kiem tra CO Y NGHIA: nhanh `was_blocked` (quyet dinh tai screening, bat bien) PHAI
  /// xet truoc nhanh FR-8/FR-2 (quyet dinh trong/sau cuoc goi).
  ///
  /// Bo sung v1.3 (FR-11/FR-12, LLD v1.4): 3 nhanh `decisionReason=BLOCK_TOGGLE_*` duoc xet NGAY
  /// SAU 2 nhanh EXACT/PREFIX - cung thuoc nhom "chan real-time, khong qua FR-8/FR-2", chi khac
  /// nguon ly do chan (ngoai blocklist). KHONG the nham voi 2 nhanh tren vi `matchedEntryType`
  /// luon null khi `decisionReason` non-null (ERD v1.3 CHECK loai tru lan nhau) - truoc khi sua,
  /// nhanh nay se vo tinh roi xuong `normal` vi `wasBlocked=true` nhung khong co `matchedEntryType`
  /// (dung bug LLD v1.4 da phat hien va sua o tang insert, nay dong tiep o tang doc/hien thi).
  CallHistoryDisplayStatus deriveDisplayStatus(CallHistoryEntryDto entry) {
    if (entry.wasBlocked && entry.matchedEntryType == 'EXACT') {
      return CallHistoryDisplayStatus.blockedRealtimeExact;
    }
    if (entry.wasBlocked && entry.matchedEntryType == 'PREFIX') {
      return CallHistoryDisplayStatus.blockedRealtimePrefix;
    }
    if (entry.wasBlocked && entry.decisionReason == 'BLOCK_TOGGLE_UNKNOWN') {
      return CallHistoryDisplayStatus.blockedToggleUnknown;
    }
    if (entry.wasBlocked && entry.decisionReason == 'BLOCK_TOGGLE_ANONYMOUS') {
      return CallHistoryDisplayStatus.blockedToggleAnonymous;
    }
    if (entry.wasBlocked && entry.decisionReason == 'BLOCK_TOGGLE_INTERNATIONAL') {
      return CallHistoryDisplayStatus.blockedToggleInternational;
    }
    if (entry.confirmationSource == 'RINGING_OVERLAY' && entry.terminationOutcome == 'SUCCESS') {
      return CallHistoryDisplayStatus.blockedAtRinging;
    }
    if (entry.wasPrompted && entry.userResponse != null) {
      return entry.userResponse == 'YES'
          ? CallHistoryDisplayStatus.confirmedYes
          : CallHistoryDisplayStatus.confirmedNo;
    }
    return CallHistoryDisplayStatus.normal;
  }

  /// Badge PHU, doc lap voi [deriveDisplayStatus] (UIUX: "Badge outcome ngắt gọi lúc đổ chuông...
  /// hiển thị badge phụ SAU" - khong phai 1 trong 5 gia tri chinh, hien thi THEM moi khi
  /// `termination_outcome` khac null, bao gom ca 3 outcome KHONG mapping toi `blockedAtRinging`
  /// o tren). Tra null khi khong ap dung.
  String? ringingOutcomeBadgeLabel(CallHistoryEntryDto entry) {
    switch (entry.terminationOutcome) {
      case 'SUCCESS':
        return 'Đã chặn & đã ngắt cuộc gọi';
      case 'NOT_APPLICABLE_CALL_ENDED':
        return 'Đã chặn — cuộc gọi tự kết thúc trước';
      case 'NOT_APPLICABLE_STATE_CHANGED':
        return 'Đã chặn — KHÔNG ngắt được (đã bắt máy hoặc thiếu điều kiện)';
      case 'FAILED_EXCEPTION':
        return 'Đã chặn — lỗi khi ngắt cuộc gọi';
      default:
        return null;
    }
  }

  /// Badge PHU cho FR-11 (CONTACTS-PRIORITY) - doc lap voi [deriveDisplayStatus], cung nguyen tac
  /// voi [ringingOutcomeBadgeLabel]: `decisionReason='CONTACTS_PRIORITY'` KHONG phai 1 trong cac
  /// gia tri enum chinh (vi ban chat la "KHONG chan", van la 1 cuoc goi binh thuong/da hoi xac
  /// nhan o badge chinh) - chi them 1 chip giai thich LY DO khong bi chan du khop mot dau so dang
  /// chan, de nguoi dung khong hieu nham la loi. Tra null khi khong ap dung.
  String? contactsPriorityBadgeLabel(CallHistoryEntryDto entry) {
    if (entry.decisionReason != 'CONTACTS_PRIORITY') return null;
    return 'Không chặn — số này có trong danh bạ của bạn';
  }
}
