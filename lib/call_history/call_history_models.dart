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
      startedAt: DateTime.fromMillisecondsSinceEpoch(row['started_at'] as int),
      endedAt: DateTime.fromMillisecondsSinceEpoch(row['ended_at'] as int),
      durationSeconds: row['duration_seconds'] as int,
    );
  }
}

/// 5 trang thai hien thi FR-6/AC2 (LLD pseudocode E8, `deriveDisplayStatus`) - `confirmedYes`/
/// `confirmedNo` la 2 nhanh con cua "CONFIRMED_AFTER_CALL" (gop lam phang thanh 2 gia tri rieng,
/// nhat quan voi cach Phase 5 da lam, thay vi 1 gia tri + sub-label rieng).
///
/// QUAN TRONG: `blockedAtRinging` CHI khop khi `terminationOutcome == SUCCESS` (dung E8 nguyen
/// van) - 3 outcome FR-8 con lai (NOT_APPLICABLE_CALL_ENDED/STATE_CHANGED/FAILED_EXCEPTION) co
/// `userResponse='YES'` nen RƠI VAO `confirmedYes` theo E8 - UIUX "badge phu" (4 outcome) la MOT
/// CHIP THONG TIN RIENG hien thi THEM ben canh badge chinh (xem [ringingOutcomeBadgeLabel] trong
/// call_history_screen.dart), KHONG thay the/gop vao 1 enum value duy nhat - day la sua lai cach
/// hieu ban dau cua agent o Phase 6 (da gan badge chinh = ca 4 outcome, SAI so voi E8).
enum CallHistoryDisplayStatus {
  blockedRealtimeExact,
  blockedRealtimePrefix,
  blockedAtRinging,
  confirmedYes,
  confirmedNo,
  normal,
}
