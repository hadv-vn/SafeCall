/// Entry doc tu bang `call_history` (LLD: `CallHistoryEntryDto`, mo rong v1.3 FR-6/FR-9).
class CallHistoryEntryDto {
  const CallHistoryEntryDto({
    required this.id,
    required this.rawNumber,
    required this.normalizedNumber,
    required this.wasAnswered,
    required this.wasBlocked,
    required this.wasPrompted,
    required this.userResponse,
    required this.confirmationSource,
    required this.terminationOutcome,
    required this.matchedEntryType,
    required this.matchedPrefixValue,
    required this.decisionReason,
    required this.startedAt,
    required this.endedAt,
    required this.durationSeconds,
  });

  final int id;
  final String? rawNumber;
  final String? normalizedNumber;
  final bool wasAnswered;
  final bool wasBlocked;
  final bool wasPrompted;

  /// 'YES' / 'NO' / null.
  final String? userResponse;

  /// 'NOTIFICATION' / 'DIALOG' / 'RINGING_OVERLAY' / null (FR-8).
  final String? confirmationSource;

  /// 'SUCCESS' / 'NOT_APPLICABLE_CALL_ENDED' / 'NOT_APPLICABLE_STATE_CHANGED' / 'FAILED_EXCEPTION'
  /// / null - chi co gia tri khi confirmationSource='RINGING_OVERLAY' va userResponse='YES' (FR-8).
  final String? terminationOutcome;

  /// 'EXACT' / 'PREFIX' / null - chi co gia tri khi wasBlocked=true (FR-9, Phase 7).
  final String? matchedEntryType;

  /// Dau so THUC SU da khop (dai nhat) - chi co gia tri khi matchedEntryType='PREFIX' (FR-9).
  final String? matchedPrefixValue;

  /// 'CONTACTS_PRIORITY' / 'BLOCK_TOGGLE_UNKNOWN' / 'BLOCK_TOGGLE_ANONYMOUS' /
  /// 'BLOCK_TOGGLE_INTERNATIONAL' / null (FR-11/FR-12, v1.3) - loai tru lan nhau voi
  /// [matchedEntryType] (khong bao gio ca 2 cung non-null). 'CONTACTS_PRIORITY' chi khi
  /// wasBlocked=false; 3 gia tri BLOCK_TOGGLE_* chi khi wasBlocked=true.
  final String? decisionReason;
  final DateTime startedAt;
  final DateTime endedAt;
  final int durationSeconds;

  factory CallHistoryEntryDto.fromRow(Map<String, Object?> row) {
    return CallHistoryEntryDto(
      id: row['id'] as int,
      rawNumber: row['raw_number'] as String?,
      normalizedNumber: row['normalized_number'] as String?,
      wasAnswered: (row['was_answered'] as int) == 1,
      wasBlocked: (row['was_blocked'] as int) == 1,
      wasPrompted: (row['was_prompted'] as int) == 1,
      userResponse: row['user_response'] as String?,
      confirmationSource: row['confirmation_source'] as String?,
      terminationOutcome: row['termination_outcome'] as String?,
      matchedEntryType: row['matched_entry_type'] as String?,
      matchedPrefixValue: row['matched_prefix_value'] as String?,
      decisionReason: row['decision_reason'] as String?,
      startedAt: DateTime.fromMillisecondsSinceEpoch(row['started_at'] as int),
      endedAt: DateTime.fromMillisecondsSinceEpoch(row['ended_at'] as int),
      durationSeconds: row['duration_seconds'] as int,
    );
  }
}

/// Trang thai hien thi FR-6/AC2 (LLD pseudocode E8, `deriveDisplayStatus`) - `confirmedYes`/
/// `confirmedNo` la 2 nhanh con cua "CONFIRMED_AFTER_CALL" (gop lam phang thanh 2 gia tri rieng,
/// nhat quan voi cach Phase 5 da lam, thay vi 1 gia tri + sub-label rieng).
///
/// QUAN TRONG: `blockedAtRinging` CHI khop khi `terminationOutcome == SUCCESS` (dung E8 nguyen
/// van) - 3 outcome FR-8 con lai (NOT_APPLICABLE_CALL_ENDED/STATE_CHANGED/FAILED_EXCEPTION) co
/// `userResponse='YES'` nen RƠI VAO `confirmedYes` theo E8 - UIUX "badge phu" (4 outcome) la MOT
/// CHIP THONG TIN RIENG hien thi THEM ben canh badge chinh (xem [ringingOutcomeBadgeLabel] trong
/// call_history_screen.dart), KHONG thay the/gop vao 1 enum value duy nhat - day la sua lai cach
/// hieu ban dau cua agent o Phase 6 (da gan badge chinh = ca 4 outcome, SAI so voi E8).
///
/// Bo sung v1.3 (FR-11/FR-12, cascade sau khi CallScreeningService co pipeline chan qua
/// BLOCK-TOGGLE): 3 gia tri `blockedToggle*` moi, suy ra tu `decisionReason` KHI `wasBlocked=true`
/// VA `matchedEntryType=null` (chan khong qua blocklist exact/prefix). Rieng `CONTACTS_PRIORITY`
/// (wasBlocked=FALSE, chi la ly do KHONG chan) KHONG phai 1 trong cac gia tri enum nay - cung
/// nguyen tac "badge phu" nhu [ringingOutcomeBadgeLabel]: 1 cuoc goi binh thuong/da hoi xac nhan
/// VA duoc contact uu tien bo qua prefix van la `normal`/`confirmedYes`/`confirmedNo` o badge
/// chinh, chi co them 1 chip phu giai thich ly do (xem [contactsPriorityBadgeLabel]).
enum CallHistoryDisplayStatus {
  blockedRealtimeExact,
  blockedRealtimePrefix,
  blockedToggleUnknown,
  blockedToggleAnonymous,
  blockedToggleInternational,
  blockedAtRinging,
  confirmedYes,
  confirmedNo,
  normal,
}
