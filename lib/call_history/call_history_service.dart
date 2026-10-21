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

  /// Suy ra 1-trong-5 trang thai hien thi, dung nguyen van pseudocode E8 cua LLD v1.3
  /// (`deriveDisplayStatus`) - thu tu kiem tra CO Y NGHIA: nhanh `was_blocked` (quyet dinh tai
  /// screening, bat bien) PHAI xet truoc nhanh FR-8/FR-2 (quyet dinh trong/sau cuoc goi).
  CallHistoryDisplayStatus deriveDisplayStatus(CallHistoryEntryDto entry) {
    if (entry.wasBlocked && entry.matchedEntryType == 'EXACT') {
      return CallHistoryDisplayStatus.blockedRealtimeExact;
    }
    if (entry.wasBlocked && entry.matchedEntryType == 'PREFIX') {
      return CallHistoryDisplayStatus.blockedRealtimePrefix;
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
}
