package com.safecall.callscreening

/**
 * Ly do allow/block KHONG qua blocklist exact/prefix (LLD v1.4, FR-11/FR-12). Cot rieng
 * `call_history.decision_reason` - KHONG overload `EntryType`/`matched_entry_type` (ERD v1.3:
 * "vi sao decision_reason la cot MOI rieng, khong overload matched_entry_type").
 *
 * Invariant (ERD v1.3 CHECK `ck_call_history_decision_reason_*`):
 * - [CONTACTS_PRIORITY] (ALLOW, FR-11) <=> was_blocked=0 VA matched_entry_type IS NULL.
 * - [BLOCK_TOGGLE_UNKNOWN]/[BLOCK_TOGGLE_ANONYMOUS]/[BLOCK_TOGGLE_INTERNATIONAL] (BLOCK, FR-12)
 *   <=> was_blocked=1 VA matched_entry_type IS NULL.
 */
enum class ScreeningDecisionReason {
    CONTACTS_PRIORITY,
    BLOCK_TOGGLE_UNKNOWN,
    BLOCK_TOGGLE_ANONYMOUS,
    BLOCK_TOGGLE_INTERNATIONAL,
}
