package com.safecall.callscreening.monitor

/** Ket qua lenh `TelecomManager.endCall()` (ADR-005) - khop CHECK `ck_call_history_termination_outcome`/ERD. */
enum class CallTerminationOutcome {
    SUCCESS,
    NOT_APPLICABLE_CALL_ENDED,
    NOT_APPLICABLE_STATE_CHANGED,
    FAILED_EXCEPTION,
}
