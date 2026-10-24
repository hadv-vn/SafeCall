# Requirements Traceability Matrix — SAFECALL

> Theo Build Playbook bước [10]. Nối Requirement (SRS v3.0, FR-1..FR-10) ↔ Design (HLD/LLD/ERD/UIUX) ↔ Code
> (đường dẫn thật, đã kiểm tồn tại qua Glob trước khi ghi vào bảng) ↔ Test (trạng thái THẬT, không bịa).
>
> **Quy ước trạng thái test** (xem ghi chú cuối file để biết lý do):
> - ✅ **Tested-Pass** — đã verify tay thật qua emulator, quan sát kết quả đúng.
> - 🟡 **Code-only, not verified** — code đã viết, build thành công, nhưng CHƯA thao tác thử để xác nhận hành
>   vi đúng.
> - ⬜ **Not-started** — thuộc WBS nhưng chưa thực hiện (ví dụ vòng test thiết bị thật đa OEM).
>
> **Không có file test code tự động nào tồn tại trên đĩa** (đã kiểm bằng Glob `**/test/**` và
> `**/androidTest/**` — chỉ khớp `docs/test/TestPlan-SafeCall-Phase1.md`, không có thư mục test code nào
> trong `lib/` hay `android/`). Toàn bộ "Test" trong bảng dưới đây là: (a) Test Plan — **kế hoạch**, không
> phải test case đã chạy; (b) nhật ký test tay thủ công qua emulator (ghi trong `DECISION-LOG-SAFECALL.md`).

## FR-1 — Chặn real-time (exact + prefix), SRS AC1-AC7

| Design | Code | Test |
|---|---|---|
| HLD "Phase breakdown" Phase 3 + Phase 7; ERD bảng `blocklist` (cột `entry_type`, UNIQUE composite `(normalized_number, entry_type)`); LLD pseudocode gốc cho `onScreenCall` (base, trước khi mở rộng FR-9) | `android/.../callscreening/CallScreeningService.kt` (`onScreenCall`), `android/.../callscreening/db/BlocklistRepository.kt` (`isBlockedExact`/`isBlockedWithDetails`), `android/.../callscreening/BlocklistPrefixIndex.kt`, `android/.../callscreening/PhoneNumberNormalizer.kt` | 🟡 **Code-only, not verified** — build OK, nhưng không có thao tác gọi/giả lập cuộc gọi thật nào được thực hiện trong phiên này (`adb emu gsm call` chưa dùng). AC6 (ưu tiên blocklist over contact) và AC7 (prefix override contact) đặc biệt chưa kiểm. |

## FR-2 — Phát hiện cuộc gọi kết thúc + hỏi xác nhận, SRS AC1-AC6

| Design | Code | Test |
|---|---|---|
| HLD Phase 4; LLD **pseudocode E1 gốc đã MẤT** (xem Phụ lục 8.4.1 LLD — logic tái dựng từ SRS/ERD, không phải chép nguyên bản ý định gốc); ERD bảng `call_history`, `pending_prompts` | `android/.../callscreening/monitor/CallMonitorService.kt`, `PromptEligibilityEvaluator.kt`, `PromptEligibility.kt`, `ScamConfirmationNotifier.kt`, `ScamConfirmationReceiver.kt`, `PendingPromptRegistry.kt`, `CallEventStreamHandler.kt`; Dart: `lib/call_events/call_event_bridge.dart`, `lib/call_events/scam_confirmation_dialog.dart` | ⬜ **Not-started** — cần trạng thái cuộc gọi thật (kết nối rồi kết thúc) để kích hoạt; chưa mô phỏng. |

## FR-3 — Xác nhận "Có" → thêm blocklist, SRS AC1-AC2

| Design | Code | Test |
|---|---|---|
| LLD **pseudocode E2 gốc đã MẤT** (tái dựng); ERD `blocklist` UNIQUE constraint (chống trùng, AC2) | `PendingPromptRegistry.kt` (`resolvePromptAtomically`, nhánh YES), `BlocklistRepository.kt` (insert), `android/.../callscreening/db/CallHistoryRepository.kt` (`recordUserConfirmation` — **gộp 2 method LLD liệt kê riêng thành 1 UPDATE, xem Phụ lục 8.4.1 LLD**) | ⬜ **Not-started** — phụ thuộc FR-2 chạy được trước. |

## FR-4 — Xác nhận "Không" / bỏ qua, SRS AC1-AC3

| Design | Code | Test |
|---|---|---|
| LLD **pseudocode gốc đã MẤT** (tái dựng) | `PendingPromptRegistry.kt` (nhánh NO + `cancelActivePromptFor` — **không set `resolved_action=NULL` khi `resolved=1`, xem Phụ lục 8.4.1 LLD**), `CallHistoryRepository.kt` | ⬜ **Not-started** — cùng lý do FR-2/FR-3. |

## FR-5 — Quản lý blocklist thủ công, SRS AC1-AC8

| Design | Code | Test |
|---|---|---|
| HLD Phase 2; UIUX "Màn hình: BlocklistScreen"; ERD `blocklist` | `lib/blocklist/blocklist_screen.dart`, `blocklist_service.dart`, `blocklist_models.dart`, `phone_number_normalizer.dart` (Dart); `lib/contacts/contacts_bridge.dart` + `android/.../callscreening/ContactsRepository.kt`/`ContactsLookupModule.kt` (AC8 cảnh báo contact — **module mới không có trong LLD class table gốc, xem Phụ lục 8.4.1**) | ✅ **Tested-Pass** — verify tay thật trên emulator: thêm `0901234567` → chuẩn hoá đúng `+84901234567`, persist SQLite, hiển thị đúng chip loại/nguồn. AC8 (cảnh báo contact) và AC4-AC7 (lỗi định dạng, trùng, xoá, hủy xoá) **CHƯA** được verify tay riêng — chỉ luồng thêm-thành-công (AC3) đã xác nhận. |

## FR-6 — Lịch sử cuộc gọi, SRS AC1-AC3

| Design | Code | Test |
|---|---|---|
| HLD Phase 5; ERD `call_history`; LLD pseudocode E8 (`deriveDisplayStatus`) | `lib/call_history/call_history_service.dart`, `call_history_models.dart`, `call_history_screen.dart`; `android/.../callscreening/db/CallHistoryRepository.kt` | ⬜ **Not-started** — màn hình được code nhưng chưa có dữ liệu cuộc gọi thật nào (FR-1/FR-2/FR-8 chưa chạy) để hiển thị/verify badge đúng. |

## FR-7 — Onboarding và cấp quyền, SRS AC1-AC5

| Design | Code | Test |
|---|---|---|
| HLD Phase 1; UIUX "Màn hình: Onboarding + Permission" | `lib/onboarding/onboarding_flow_screen.dart`, `onboarding_status.dart`, `permission_step_config.dart`, `contacts_banner.dart`; `lib/permissions/permissions_bridge.dart`, `permission_keys.dart`; `android/.../callscreening/PermissionsModule.kt`, `PermissionKeys.kt`, `RoleModule.kt` | ✅ **Tested-Pass** — verify tay thật: `RoleManager` system dialog, 4 runtime permission dialog tuần tự, bước `SYSTEM_ALERT_WINDOW` settings-redirect (sau khi sửa 1 bug Dart logic — xem `DECISION-LOG-SAFECALL.md`, lỗi không ghi riêng khối vì không phải lỗi build/crash). AC4 (phát hiện quyền bị thu hồi khi mở lại app) **chưa** verify riêng. |

## FR-8 — Xác nhận lừa đảo lúc đổ chuông + ngắt cuộc gọi, SRS AC1-AC10

| Design | Code | Test |
|---|---|---|
| HLD Phase 6; UIUX "Màn hình/Overlay MỚI: RingingScamOverlay"; ADR-005/ADR-007; LLD **pseudocode E3/E4 gốc đã MẤT** (`evaluateRingingPromptEligibility`/`resolveRingingYes` tái dựng từ SRS/ERD/UIUX — **đây là hard-stop bị vi phạm, xem `DECISION-LOG-SAFECALL.md` khối #5**) | `android/.../callscreening/monitor/RingingScamOverlay.kt`, `RingingEligibility.kt`, `RingingCallNumberHolder.kt` (**giải pháp phát sinh cho giới hạn privacy API 31+, không có trong LLD — xem Phụ lục 8.4.1**), `CallTerminationOutcome.kt`, `CallMonitorService.kt` (mở rộng `attemptEndCall`/single call-site theo ADR-005) | ⬜ **Not-started** — chưa mô phỏng cuộc gọi đang đổ chuông; hành động `endCall()` thật **chưa được gọi thử lần nào**. Đây là luồng rủi ro cao nhất chưa test (hành động không thể hoàn tác, NFR-3). |

## FR-9 — Chặn theo đầu số (prefix), SRS AC1-AC8

| Design | Code | Test |
|---|---|---|
| HLD Phase 7; ERD v1.2 (`blocklist.entry_type`, bảng `blocklist_meta`, trigger `prefix_version`); UIUX "Luồng thêm entry... cập nhật v1.3 cho cả EXACT và PREFIX" | `android/.../callscreening/EntryType.kt`, `BlocklistPrefixIndex.kt`, `BlocklistRepository.kt` (mở rộng), `PhoneNumberNormalizer.kt` (2 phía); `lib/blocklist/blocklist_service.dart`/`blocklist_models.dart` (mở rộng), `blocklist_screen.dart` (UI chọn loại entry) | 🟡 **Code-only, not verified** — build OK; luồng thêm entry loại "Đầu số" qua UI **chưa** được thao tác thử (chỉ luồng EXACT ở FR-5 đã test). Hành vi chặn thật theo prefix: Not-started (giống FR-1). |

## FR-10 — Export/Import blocklist, SRS AC1-AC11

| Design | Code | Test |
|---|---|---|
| HLD Phase 7; UIUX "Export blocklist"/"Import blocklist"; ERD "Vì sao KHÔNG cần bảng mới cho lịch sử Export/Import"; LLD pseudocode E7 (`BlocklistImportService` → `source=IMPORTED`, **đã tự giải quyết qua chính pseudocode gốc, không phải giả định mới — xem Phụ lục 8.4.1**) | `lib/blocklist/blocklist_export_service.dart`, `blocklist_import_service.dart`, `blocklist_export_import_models.dart` | 🟡 **Code-only, not verified** — build OK sau khi nâng `file_picker` lên `^8.1.2` (Decision Log #2), nhưng Share Sheet export thật và chọn-file-import thật **chưa** được thao tác thử. AC10 (import gián đoạn giữa chừng) đặc biệt chưa test — đúng khớp WBS Task 7.7.5 "chưa chạy" trong `RISK-SAFECALL.md`. |

## Cross-cutting — Schema & Database (không phải 1 FR riêng, nền cho mọi FR trên)

| Design | Code | Test |
|---|---|---|
| ERD v1.2 đầy đủ (`blocklist`, `blocklist_meta`, `confirmed_safe`, `call_history`, `pending_prompts`, `schema_migrations`) | `lib/db/safecall_schema.dart`, `database_provider.dart`; `android/.../callscreening/db/SafeCallSchema.kt`, `SafeCallDatabaseHelper.kt`, `BlocklistReadException.kt`, `BlocklistWriteException.kt` | ✅ **Tested-Pass (một phần)** — tạo schema + CRUD cơ bản verify qua luồng FR-5; đã tìm và sửa lỗi PRAGMA-trong-transaction thật (`DECISION-LOG-SAFECALL.md` khối #4). Trigger `prefix_version` (FR-9) và cơ chế `mergeEntriesInTransaction` (FR-10) **chưa** verify riêng. |

## WBS Task liên quan CHƯA thực hiện (ảnh hưởng trực tiếp tới cột Test ở trên)

Lấy từ `WBS-SafeCall-Phase1.md`, khớp với `RISK-SAFECALL.md`: Task 6.5.3 (test đa OEM thật, FR-8), 7.7.3
(performance sanity-check prefix), 7.7.5 (test crash giữa import, FR-10/AC10), 4.4.2/6.5.2 (integration test
race có kiểm soát), 8.1.x (System/E2E script `adb emu gsm call`), 8.2.1/8.2.2 đã làm MỘT PHẦN qua rà soát thủ
công (xem Phụ lục 8.4.1 LLD — không phát hiện SQL injection, `allowBackup=false`, 1 điểm `endCall()` duy
nhất), 8.3.1 (UAT) — đều **chưa thực hiện**.

## Story/FR "mồ côi" — không giấu

Theo đúng yêu cầu Build Playbook ("RTM không còn story mồ côi, nếu còn phải NÊU RÕ"), các FR sau có code đầy
đủ nhưng **0 test thật** (không phải "1 phần" như FR-1/FR-9/FR-10, mà hoàn toàn chưa chạm tới):

- **FR-2, FR-3, FR-4** — toàn bộ luồng prompt-sau-cuộc-gọi, 0 lần verify tay.
- **FR-6** — màn hình lịch sử cuộc gọi, 0 dữ liệu thật để hiển thị nên 0 lần verify tay.
- **FR-8** — toàn bộ luồng overlay-lúc-đổ-chuông + `endCall()`, 0 lần verify tay. **Đây là FR có rủi ro cao
  nhất đang mồ côi** (hành động không thể hoàn tác + 2 hàm lõi logic bị tái dựng do mất pseudocode gốc —
  cộng dồn 2 lớp rủi ro chưa kiểm chứng).

Không có FR nào thiếu CODE hoàn toàn — toàn bộ 10 FR đều có code path tương ứng đã build thành công. "Mồ côi"
ở đây nghĩa là mồ côi TEST, không phải mồ côi code.
