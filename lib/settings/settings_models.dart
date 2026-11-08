/// 4 co cau hinh FR-11 (CONTACTS-PRIORITY) / FR-12 (BLOCK-TOGGLE), luu trong `blocklist_meta`
/// (ADR-008). LLD: `SettingsFlags`/`SettingsService.getFlags()`/`setFlag()`.
class SettingsFlags {
  const SettingsFlags({
    required this.contactsPriorityEnabled,
    required this.blockUnknownEnabled,
    required this.blockAnonymousEnabled,
    required this.blockInternationalEnabled,
  });

  /// Gia tri default dung khi insert ban dau (ADR-008): CONTACTS-PRIORITY BAT, 3 BLOCK-TOGGLE TAT.
  const SettingsFlags.defaults()
      : contactsPriorityEnabled = true,
        blockUnknownEnabled = false,
        blockAnonymousEnabled = false,
        blockInternationalEnabled = false;

  final bool contactsPriorityEnabled;
  final bool blockUnknownEnabled;
  final bool blockAnonymousEnabled;
  final bool blockInternationalEnabled;

  factory SettingsFlags.fromRow(Map<String, Object?> row) {
    return SettingsFlags(
      contactsPriorityEnabled: (row['contacts_priority_enabled'] as int) == 1,
      blockUnknownEnabled: (row['block_unknown_enabled'] as int) == 1,
      blockAnonymousEnabled: (row['block_anonymous_enabled'] as int) == 1,
      blockInternationalEnabled: (row['block_international_enabled'] as int) == 1,
    );
  }

  SettingsFlags copyWith({
    bool? contactsPriorityEnabled,
    bool? blockUnknownEnabled,
    bool? blockAnonymousEnabled,
    bool? blockInternationalEnabled,
  }) {
    return SettingsFlags(
      contactsPriorityEnabled: contactsPriorityEnabled ?? this.contactsPriorityEnabled,
      blockUnknownEnabled: blockUnknownEnabled ?? this.blockUnknownEnabled,
      blockAnonymousEnabled: blockAnonymousEnabled ?? this.blockAnonymousEnabled,
      blockInternationalEnabled: blockInternationalEnabled ?? this.blockInternationalEnabled,
    );
  }

  bool valueOf(SettingsFlag flag) => switch (flag) {
        SettingsFlag.contactsPriority => contactsPriorityEnabled,
        SettingsFlag.blockUnknown => blockUnknownEnabled,
        SettingsFlag.blockAnonymous => blockAnonymousEnabled,
        SettingsFlag.blockInternational => blockInternationalEnabled,
      };

  SettingsFlags withValue(SettingsFlag flag, bool value) => switch (flag) {
        SettingsFlag.contactsPriority => copyWith(contactsPriorityEnabled: value),
        SettingsFlag.blockUnknown => copyWith(blockUnknownEnabled: value),
        SettingsFlag.blockAnonymous => copyWith(blockAnonymousEnabled: value),
        SettingsFlag.blockInternational => copyWith(blockInternationalEnabled: value),
      };
}

/// 1 trong 4 co - khop ten cot `blocklist_meta` (ADR-008).
enum SettingsFlag {
  contactsPriority,
  blockUnknown,
  blockAnonymous,
  blockInternational;

  String get columnName => switch (this) {
        SettingsFlag.contactsPriority => 'contacts_priority_enabled',
        SettingsFlag.blockUnknown => 'block_unknown_enabled',
        SettingsFlag.blockAnonymous => 'block_anonymous_enabled',
        SettingsFlag.blockInternational => 'block_international_enabled',
      };
}
