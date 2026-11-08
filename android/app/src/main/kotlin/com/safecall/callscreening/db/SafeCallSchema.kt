package com.safecall.callscreening.db

/**
 * DDL schema v1 - nguon su that: docs/design/ERD-SafeCall-Phase1.md (v1.2), muc "DDL minh hoa".
 *
 * QUAN TRONG: danh sach CREATE_STATEMENTS nay PHAI duoc phan chieu GIONG NHAU TUYET DOI voi
 * lib/db/safecall_schema.dart (ca hai phia Dart/Kotlin cung tao/mo MOT file SQLite chung) - neu
 * sua o day, PHAI sua dong thoi ben kia (xem ERD muc "Chien luoc migration").
 *
 * Danh dau DRAFT: ERD con ghi "van DRAFT, can qua db-schema-reviewer truoc khi dung lam migration
 * that" - DDL nay dung tam cho Phase 1 implementation, chua qua review chinh thuc.
 */
object SafeCallSchema {

    const val SCHEMA_VERSION = 2

    /** Thiet lap PRAGMA cho connection DOC (ngan - CallScreeningService.onScreenCall). */
    const val PRAGMA_BUSY_TIMEOUT_READ_MS = 1000

    /** Thiet lap PRAGMA cho cac connection KHAC (UI Dart, CallMonitorService/ScamConfirmationReceiver). */
    const val PRAGMA_BUSY_TIMEOUT_OTHER_MS = 3000

    val CREATE_STATEMENTS: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS blocklist (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            normalized_number TEXT    NOT NULL,
            entry_type        TEXT    NOT NULL,
            source            TEXT    NOT NULL,
            added_at          INTEGER NOT NULL,
            CONSTRAINT uq_blocklist_number_type UNIQUE (normalized_number, entry_type),
            CONSTRAINT ck_blocklist_source CHECK (source IN ('MANUAL', 'AUTO_CONFIRMED', 'IMPORTED')),
            CONSTRAINT ck_blocklist_entry_type CHECK (entry_type IN ('EXACT', 'PREFIX')),
            CONSTRAINT ck_blocklist_number_format CHECK (
                (entry_type = 'EXACT'  AND normalized_number LIKE '+%' AND length(normalized_number) BETWEEN 8 AND 16)
                OR
                (entry_type = 'PREFIX' AND normalized_number LIKE '+%' AND length(normalized_number) BETWEEN 5 AND 16)
            )
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_blocklist_added_at ON blocklist (added_at DESC)",
        """
        CREATE INDEX IF NOT EXISTS idx_blocklist_prefix_values
            ON blocklist (normalized_number)
            WHERE entry_type = 'PREFIX'
        """,
        """
        CREATE TABLE IF NOT EXISTS blocklist_meta (
            id                           INTEGER PRIMARY KEY,
            prefix_version               INTEGER NOT NULL DEFAULT 0,
            contacts_priority_enabled    INTEGER NOT NULL DEFAULT 1,
            block_unknown_enabled        INTEGER NOT NULL DEFAULT 0,
            block_anonymous_enabled      INTEGER NOT NULL DEFAULT 0,
            block_international_enabled  INTEGER NOT NULL DEFAULT 0,
            CONSTRAINT ck_blocklist_meta_singleton CHECK (id = 1),
            CONSTRAINT ck_blocklist_meta_contacts_priority_enabled CHECK (contacts_priority_enabled IN (0,1)),
            CONSTRAINT ck_blocklist_meta_block_unknown_enabled CHECK (block_unknown_enabled IN (0,1)),
            CONSTRAINT ck_blocklist_meta_block_anonymous_enabled CHECK (block_anonymous_enabled IN (0,1)),
            CONSTRAINT ck_blocklist_meta_block_international_enabled CHECK (block_international_enabled IN (0,1))
        )
        """,
        "INSERT OR IGNORE INTO blocklist_meta (id, prefix_version) VALUES (1, 0)",
        """
        CREATE TRIGGER IF NOT EXISTS trg_blocklist_prefix_version_insert
        AFTER INSERT ON blocklist
        WHEN NEW.entry_type = 'PREFIX'
        BEGIN
            UPDATE blocklist_meta SET prefix_version = prefix_version + 1 WHERE id = 1;
        END
        """,
        """
        CREATE TRIGGER IF NOT EXISTS trg_blocklist_prefix_version_delete
        AFTER DELETE ON blocklist
        WHEN OLD.entry_type = 'PREFIX'
        BEGIN
            UPDATE blocklist_meta SET prefix_version = prefix_version + 1 WHERE id = 1;
        END
        """,
        """
        CREATE TABLE IF NOT EXISTS confirmed_safe (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            normalized_number TEXT    NOT NULL,
            confirmed_at      INTEGER NOT NULL,
            CONSTRAINT uq_confirmed_safe_number UNIQUE (normalized_number),
            CONSTRAINT ck_confirmed_safe_number_format CHECK (
                normalized_number LIKE '+%' AND length(normalized_number) BETWEEN 8 AND 16
            )
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS call_history (
            id                   INTEGER PRIMARY KEY AUTOINCREMENT,
            raw_number           TEXT,
            normalized_number    TEXT,
            direction            TEXT    NOT NULL,
            was_answered         INTEGER NOT NULL,
            was_blocked          INTEGER NOT NULL DEFAULT 0,
            was_prompted         INTEGER NOT NULL DEFAULT 0,
            user_response        TEXT,
            confirmation_source  TEXT,
            termination_outcome  TEXT,
            matched_entry_type   TEXT,
            matched_prefix_value TEXT,
            decision_reason      TEXT,
            started_at           INTEGER NOT NULL,
            ended_at             INTEGER NOT NULL,
            duration_seconds     INTEGER NOT NULL,
            CONSTRAINT ck_call_history_direction CHECK (direction IN ('INCOMING')),
            CONSTRAINT ck_call_history_was_answered CHECK (was_answered IN (0,1)),
            CONSTRAINT ck_call_history_was_blocked CHECK (was_blocked IN (0,1)),
            CONSTRAINT ck_call_history_was_prompted CHECK (was_prompted IN (0,1)),
            CONSTRAINT ck_call_history_user_response CHECK (
                user_response IS NULL OR user_response IN ('YES', 'NO')
            ),
            CONSTRAINT ck_call_history_response_requires_prompt CHECK (
                user_response IS NULL OR was_prompted = 1
            ),
            CONSTRAINT ck_call_history_number_format CHECK (
                normalized_number IS NULL
                OR (normalized_number LIKE '+%' AND length(normalized_number) BETWEEN 8 AND 16)
            ),
            CONSTRAINT ck_call_history_time_order CHECK (ended_at >= started_at),
            CONSTRAINT ck_call_history_duration CHECK (duration_seconds >= 0),
            CONSTRAINT ck_call_history_confirmation_source CHECK (
                confirmation_source IS NULL OR confirmation_source IN ('NOTIFICATION', 'DIALOG', 'RINGING_OVERLAY')
            ),
            CONSTRAINT ck_call_history_response_requires_source CHECK (
                (user_response IS NULL AND confirmation_source IS NULL)
                OR
                (user_response IS NOT NULL AND confirmation_source IS NOT NULL)
            ),
            CONSTRAINT ck_call_history_termination_outcome CHECK (
                termination_outcome IS NULL
                OR termination_outcome IN ('SUCCESS', 'NOT_APPLICABLE_CALL_ENDED', 'NOT_APPLICABLE_STATE_CHANGED', 'FAILED_EXCEPTION')
            ),
            CONSTRAINT ck_call_history_termination_requires_ringing_yes CHECK (
                termination_outcome IS NULL
                OR (confirmation_source = 'RINGING_OVERLAY' AND user_response = 'YES')
            ),
            CONSTRAINT ck_call_history_matched_entry_type CHECK (
                matched_entry_type IS NULL OR matched_entry_type IN ('EXACT', 'PREFIX')
            ),
            CONSTRAINT ck_call_history_matched_prefix_value CHECK (
                (matched_entry_type = 'PREFIX' AND matched_prefix_value IS NOT NULL)
                OR
                ((matched_entry_type IS NULL OR matched_entry_type = 'EXACT') AND matched_prefix_value IS NULL)
            ),
            CONSTRAINT ck_call_history_matched_type_requires_blocked CHECK (
                matched_entry_type IS NULL OR was_blocked = 1
            ),
            CONSTRAINT ck_call_history_decision_reason CHECK (
                decision_reason IS NULL
                OR decision_reason IN ('CONTACTS_PRIORITY', 'BLOCK_TOGGLE_UNKNOWN', 'BLOCK_TOGGLE_ANONYMOUS', 'BLOCK_TOGGLE_INTERNATIONAL')
            ),
            CONSTRAINT ck_call_history_decision_reason_excludes_matched_entry_type CHECK (
                decision_reason IS NULL OR matched_entry_type IS NULL
            ),
            CONSTRAINT ck_call_history_decision_reason_contacts_priority CHECK (
                decision_reason IS NULL OR decision_reason != 'CONTACTS_PRIORITY' OR was_blocked = 0
            ),
            CONSTRAINT ck_call_history_decision_reason_block_toggle CHECK (
                decision_reason IS NULL
                OR decision_reason NOT IN ('BLOCK_TOGGLE_UNKNOWN', 'BLOCK_TOGGLE_ANONYMOUS', 'BLOCK_TOGGLE_INTERNATIONAL')
                OR was_blocked = 1
            )
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_call_history_ended_at ON call_history (ended_at DESC)",
        "CREATE INDEX IF NOT EXISTS idx_call_history_normalized_number ON call_history (normalized_number)",
        """
        CREATE TABLE IF NOT EXISTS pending_prompts (
            prompt_id            TEXT    PRIMARY KEY,
            normalized_number    TEXT    NOT NULL,
            kind                 TEXT    NOT NULL,
            call_record_id       INTEGER,
            notification_id      INTEGER NOT NULL,
            resolved             INTEGER NOT NULL DEFAULT 0,
            resolved_action      TEXT,
            resolved_by          TEXT,
            resolved_at          INTEGER,
            termination_outcome  TEXT,
            created_at           INTEGER NOT NULL,
            CONSTRAINT fk_pending_prompts_call_record
                FOREIGN KEY (call_record_id) REFERENCES call_history (id) ON DELETE CASCADE,
            CONSTRAINT ck_pending_prompts_resolved CHECK (resolved IN (0,1)),
            CONSTRAINT ck_pending_prompts_action CHECK (
                resolved_action IS NULL OR resolved_action IN ('YES', 'NO')
            ),
            CONSTRAINT ck_pending_prompts_resolved_by CHECK (
                resolved_by IS NULL OR resolved_by IN ('NOTIFICATION', 'DIALOG', 'RINGING_OVERLAY')
            ),
            CONSTRAINT ck_pending_prompts_resolution_consistency CHECK (
                (resolved = 0 AND resolved_action IS NULL AND resolved_by IS NULL AND resolved_at IS NULL)
                OR
                (resolved = 1 AND resolved_action IS NOT NULL AND resolved_by IS NOT NULL AND resolved_at IS NOT NULL)
            ),
            CONSTRAINT ck_pending_prompts_number_format CHECK (
                normalized_number LIKE '+%' AND length(normalized_number) BETWEEN 8 AND 16
            ),
            CONSTRAINT ck_pending_prompts_kind CHECK (kind IN ('POST_CALL', 'RINGING')),
            CONSTRAINT ck_pending_prompts_call_record_required_for_post_call CHECK (
                (kind = 'POST_CALL' AND call_record_id IS NOT NULL) OR (kind = 'RINGING')
            ),
            CONSTRAINT ck_pending_prompts_termination_outcome CHECK (
                termination_outcome IS NULL
                OR (resolved = 1 AND resolved_action = 'YES' AND resolved_by = 'RINGING_OVERLAY')
            )
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_pending_prompts_call_record_id ON pending_prompts (call_record_id)",
        "CREATE INDEX IF NOT EXISTS idx_pending_prompts_number_resolved ON pending_prompts (normalized_number, resolved)",
        "CREATE INDEX IF NOT EXISTS idx_pending_prompts_kind_resolved ON pending_prompts (kind, resolved)",
        """
        CREATE TABLE IF NOT EXISTS schema_migrations (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            version     INTEGER NOT NULL,
            applied_at  INTEGER NOT NULL,
            applied_by  TEXT    NOT NULL,
            CONSTRAINT ck_schema_migrations_applied_by CHECK (applied_by IN ('DART', 'NATIVE'))
        )
        """
    )

    /**
     * v1 -> v2 (bo sung FR-11/FR-12/FR-9 cot moi, ADR-008 muc "Tieu cuc/trade-off" da canh bao
     * truoc ma chua ai lam): CREATE_STATEMENTS da duoc sua de them cot moi NHUNG do SCHEMA_VERSION
     * khong doi, SQLiteOpenHelper se KHONG goi lai onCreate() hay onUpgrade() cho database da ton
     * tai tu truoc - cac cot moi se KHONG xuat hien, lam getSettingsFlags() nem loi "no such
     * column" -> fail-open sai cho pipeline PREFIX/BLOCK-TOGGLE (bug thuc te da gap).
     *
     * Moi [ColumnMigration] gan voi DUNG 1 cot - [SafeCallDatabaseHelper.onUpgrade] PHAI kiem tra
     * cot da ton tai chua (qua PRAGMA table_info) truoc khi chay [alterStatement] (khong chay vo
     * dieu kien). Bat buoc vi mot so DB thuc te (da tao bang onCreate() tu ban build TRUOC KHI
     * SCHEMA_VERSION duoc bump o day nhung SAU KHI CREATE_STATEMENTS da duoc sua them cot moi) da
     * co san cac cot nay o version=1 - chay ALTER TABLE ADD COLUMN vo dieu kien se loi "duplicate
     * column name", lam onUpgrade that bai va dong ca database (crash thuc te da gap tren thiet bi
     * test, KHONG phai gia dinh).
     *
     * Han che da biet: SQLite KHONG cho ALTER TABLE them CONSTRAINT nhieu-cot vao bang da ton tai
     * (phai CREATE bang moi + copy du lieu neu can dung het rang buoc). Phase 1 chap nhan bo qua 3
     * CHECK nhieu-cot cua `decision_reason` (ck_call_history_decision_reason_excludes_matched_entry_type/
     * _contacts_priority/_block_toggle) tren DB da migrate - tinh dung dan van duoc dam bao o tang
     * ung dung (CallScreeningService chi ghi cac to hop hop le), CHECK don-cot van giu nguyen.
     */
    data class ColumnMigration(val table: String, val column: String, val alterStatement: String)

    val MIGRATION_V1_TO_V2: List<ColumnMigration> = listOf(
        ColumnMigration(
            "blocklist_meta", "contacts_priority_enabled",
            "ALTER TABLE blocklist_meta ADD COLUMN contacts_priority_enabled INTEGER NOT NULL DEFAULT 1 CHECK (contacts_priority_enabled IN (0,1))",
        ),
        ColumnMigration(
            "blocklist_meta", "block_unknown_enabled",
            "ALTER TABLE blocklist_meta ADD COLUMN block_unknown_enabled INTEGER NOT NULL DEFAULT 0 CHECK (block_unknown_enabled IN (0,1))",
        ),
        ColumnMigration(
            "blocklist_meta", "block_anonymous_enabled",
            "ALTER TABLE blocklist_meta ADD COLUMN block_anonymous_enabled INTEGER NOT NULL DEFAULT 0 CHECK (block_anonymous_enabled IN (0,1))",
        ),
        ColumnMigration(
            "blocklist_meta", "block_international_enabled",
            "ALTER TABLE blocklist_meta ADD COLUMN block_international_enabled INTEGER NOT NULL DEFAULT 0 CHECK (block_international_enabled IN (0,1))",
        ),
        ColumnMigration(
            "call_history", "decision_reason",
            """
            ALTER TABLE call_history ADD COLUMN decision_reason TEXT
                CHECK (decision_reason IS NULL OR decision_reason IN ('CONTACTS_PRIORITY', 'BLOCK_TOGGLE_UNKNOWN', 'BLOCK_TOGGLE_ANONYMOUS', 'BLOCK_TOGGLE_INTERNATIONAL'))
            """,
        ),
    )
}
