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
> **Cập nhật 2026-10-25:** nhận định gốc dưới đây ("không có file test code tự động nào") **không còn đúng
> 100%** — đã có 1 bộ golden-file test tự động cho `PhoneNumberNormalizer` (`test/blocklist/`,
> `android/app/src/test/kotlin/`, 58 case, đóng Risk R9). Giữ nguyên nhận định gốc bên dưới cho phần còn lại
> (toàn bộ FR khác vẫn 0 test tự động) và đánh dấu rõ ở từng dòng FR-5/FR-9 bị ảnh hưởng.
>
> **Cập nhật 2026-10-25 (lần 2 — đóng gap Knowledge Health c1/c4):** bổ sung bảng trace cho **7 NFR
> (NFR-1..NFR-7)** — trước đây RTM chỉ phủ 10 FR, khiến Traceability Coverage thật chỉ 10/17≈58.8%
> (xem `KNOWLEDGE-HEALTH-SAFECALL.md`). Mỗi dòng Code dưới đây đã **verify tồn tại thật qua `grep`/`find`**
> trên đĩa trước khi ghi vào bảng (không chép lại từ bộ nhớ/suy luận), theo đúng quy ước đã dùng cho 10 FR.
>
> **Nhận định gốc (vẫn đúng cho phần lớn hệ thống):** không có file test code tự động nào khác tồn tại trên
> đĩa (đã kiểm bằng Glob `**/test/**` và `**/androidTest/**` trước 2026-10-25 — chỉ khớp
> `docs/test/TestPlan-SafeCall-Phase1.md`). Toàn bộ "Test" trong bảng dưới đây là: (a) Test Plan — **kế
> hoạch**, không phải test case đã chạy; (b) nhật ký test tay thủ công qua emulator (ghi trong
> `DECISION-LOG-SAFECALL.md`); (c) *(mới)* 1 bộ golden-file test tự động, đã viết nhưng **chưa chạy qua
> toolchain thật** (`flutter test`/`gradle test` không có trong môi trường AI).

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
| HLD Phase 2; UIUX "Màn hình: BlocklistScreen"; ERD `blocklist` | `lib/blocklist/blocklist_screen.dart`, `blocklist_service.dart`, `blocklist_models.dart`, `phone_number_normalizer.dart` (Dart); `lib/contacts/contacts_bridge.dart` + `android/.../callscreening/ContactsRepository.kt`/`ContactsLookupModule.kt` (AC8 cảnh báo contact — **module mới không có trong LLD class table gốc, xem Phụ lục 8.4.1**) | ✅ **Tested-Pass** — verify tay thật trên emulator: thêm `0901234567` → chuẩn hoá đúng `+84901234567`, persist SQLite, hiển thị đúng chip loại/nguồn. AC8 (cảnh báo contact) và AC4-AC7 (lỗi định dạng, trùng, xoá, hủy xoá) **CHƯA** được verify tay riêng — chỉ luồng thêm-thành-công (AC3) đã xác nhận. **Bổ sung 2026-10-25:** phần chuẩn hoá số (`phone_number_normalizer.dart`) nay có thêm 🟡 golden-file test tự động 58 case (đầu số 6 nhà mạng VN thật) đối chiếu với bản Kotlin — đã viết, tự verify bằng oracle độc lập (58/58), **chưa chạy qua `flutter test` thật**. **Tái xác nhận 2026-10-25 (lần 2):** sau khi bump AGP 8.3.0→8.11.1/Kotlin 1.9.22→2.2.20/compileSdk 34→36 (để khớp Flutter stable hiện hành) và rebuild, luồng thêm entry EXACT được test tay lại lần nữa trên `emulator-5554` (`0987654321`→`+84987654321`) — vẫn đúng, không có regression từ các thay đổi toolchain. |

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
| HLD Phase 7; ERD v1.2 (`blocklist.entry_type`, bảng `blocklist_meta`, trigger `prefix_version`); UIUX "Luồng thêm entry... cập nhật v1.3 cho cả EXACT và PREFIX" | `android/.../callscreening/EntryType.kt`, `BlocklistPrefixIndex.kt`, `BlocklistRepository.kt` (mở rộng), `PhoneNumberNormalizer.kt` (2 phía); `lib/blocklist/blocklist_service.dart`/`blocklist_models.dart` (mở rộng), `blocklist_screen.dart` (UI chọn loại entry) | 🟡 **Code-only, not verified (một phần)** — **Cập nhật 2026-10-25 (lần 2):** luồng thêm entry loại "Đầu số" qua UI **NAY ĐÃ** được thao tác thử tay thật trên `emulator-5554` (API 34, cài lại `app-debug.apk` sau rebuild): chuyển tab "Đầu số" → placeholder đổi đúng ngữ cảnh → nhập `0999` → bấm "Thêm" → lưu đúng thành `+84999*`, hiển thị icon nhánh riêng biệt (khác icon điện thoại của entry EXACT) + badge "Đầu số", persist SQLite đúng, logcat sạch không FATAL/AndroidRuntime. Đây chỉ là input hợp lệ (4 ký tự) — **chưa** test case từ chối dưới ngưỡng AC2, chưa test cảnh báo trùng contact AC5. **Hành vi chặn cuộc gọi thật theo prefix vẫn Not-started** (giống FR-1 — cần `adb emu gsm call`, chưa làm) — đây mới là lý do giữ nguyên 🟡 ở mức FR, không nâng lên ✅, vì phần cốt lõi (matching/chặn thật) chưa chạm tới, chỉ mới xác nhận được luồng UI thêm entry. **Bổ sung 2026-10-25 (lần 1):** riêng phần chuẩn hoá đầu số (`PhoneNumberNormalizer`, `entryType=PREFIX`) có 6/58 case trong golden-file test mới (đóng Risk R9 một phần) — đã viết, chưa chạy qua toolchain thật. |

## FR-10 — Export/Import blocklist, SRS AC1-AC11

| Design | Code | Test |
|---|---|---|
| HLD Phase 7; UIUX "Export blocklist"/"Import blocklist"; ERD "Vì sao KHÔNG cần bảng mới cho lịch sử Export/Import"; LLD pseudocode E7 (`BlocklistImportService` → `source=IMPORTED`, **đã tự giải quyết qua chính pseudocode gốc, không phải giả định mới — xem Phụ lục 8.4.1**) | `lib/blocklist/blocklist_export_service.dart`, `blocklist_import_service.dart`, `blocklist_export_import_models.dart` | 🟡 **Code-only, not verified** — build OK sau khi nâng `file_picker` lên `^8.1.2` (Decision Log #2), nhưng Share Sheet export thật và chọn-file-import thật **chưa** được thao tác thử. AC10 (import gián đoạn giữa chừng) đặc biệt chưa test — đúng khớp WBS Task 7.7.5 "chưa chạy" trong `RISK-SAFECALL.md`. |

## Cross-cutting — Schema & Database (không phải 1 FR riêng, nền cho mọi FR trên)

| Design | Code | Test |
|---|---|---|
| ERD v1.2 đầy đủ (`blocklist`, `blocklist_meta`, `confirmed_safe`, `call_history`, `pending_prompts`, `schema_migrations`) | `lib/db/safecall_schema.dart`, `database_provider.dart`; `android/.../callscreening/db/SafeCallSchema.kt`, `SafeCallDatabaseHelper.kt`, `BlocklistReadException.kt`, `BlocklistWriteException.kt` | ✅ **Tested-Pass (một phần)** — tạo schema + CRUD cơ bản verify qua luồng FR-5; đã tìm và sửa lỗi PRAGMA-trong-transaction thật (`DECISION-LOG-SAFECALL.md` khối #4). Trigger `prefix_version` (FR-9) và cơ chế `mergeEntriesInTransaction` (FR-10) **chưa** verify riêng. |

## NFR-1 — Dữ liệu cá nhân bên thứ ba (xoá khi gỡ app, bảo vệ trên thiết bị, Export không mã hoá)

| Design | Code | Test |
|---|---|---|
| SRS §4 NFR-1; `ADR-003` (local-only blocklist) — **lưu ý: ADR-003 CHƯA được cập nhật để ghi nhận ngoại lệ Export qua Share Sheet**, xem `RISK-SAFECALL.md` R7 (gap đã tự ghi nhận, không phải phát hiện mới) | `android/app/src/main/AndroidManifest.xml` (`android:allowBackup="false"`, dòng 27 — verify thật qua `grep`, đảm bảo dữ liệu bị xoá khi gỡ app theo mặc định Android); `lib/blocklist/blocklist_export_service.dart` (export JSON không mã hoá qua Share Sheet) | 🟡 **Code-only, not verified** — `allowBackup=false` chưa có test xác nhận hành vi xoá dữ liệu thật khi gỡ app (chỉ là thuộc tính manifest, chưa thao tác thử gỡ-cài lại); Export qua Share Sheet thật **chưa** được thao tác thử (giống nhận định ở FR-10). |

## NFR-2 — Minh bạch với người dùng về lý do cần quyền nhạy cảm

| Design | Code | Test |
|---|---|---|
| SRS §4 NFR-2; UIUX "Màn hình: Onboarding + Permission" | `lib/onboarding/onboarding_flow_screen.dart`, `permission_step_config.dart` (nội dung giải thích từng quyền trước khi xin cấp) | ✅ **Tested-Pass** — dùng chung bằng chứng với FR-7 (verify tay thật luồng onboarding 7 bước trên emulator, xem `DECISION-LOG-SAFECALL.md`). Không tách riêng được phần "nội dung giải thích có đủ thuyết phục" vì đó là đánh giá UX định tính, ngoài phạm vi verify tay nhị phân. |

## NFR-3 — Rủi ro chặn nhầm (cảnh báo contact) + hành động không thể hoàn tác (FR-8)

| Design | Code | Test |
|---|---|---|
| SRS §4 NFR-3; FR-5/AC8 (cảnh báo khi chặn contact); FR-9/AC5 (cảnh báo tương tự cho prefix) | `android/.../callscreening/ContactsRepository.kt`, `ContactsLookupModule.kt` (tra cứu + cảnh báo contact); `CallMonitorService.kt` (`attemptEndCall` — hành động không thể hoàn tác của FR-8) | ⬜ **Not-started / chưa verify riêng** — AC8 (cảnh báo contact) **chưa** được verify tay riêng (đã ghi ở dòng FR-5 gốc); `endCall()` **chưa được gọi thử lần nào** (đã ghi ở dòng FR-8 gốc). NFR này cộng dồn 2 rủi ro chưa kiểm chứng của chính FR-5/FR-8, không phải phát hiện test mới. |

## NFR-4 — Độ tin cậy phát hiện trạng thái cuộc gọi (API 29 vs 31+), overlay đúng lúc, hiệu năng matching

| Design | Code | Test |
|---|---|---|
| SRS §4 NFR-4; `RISK-SAFECALL.md` R1 (độ tin cậy phát hiện), R2 (overlay OEM), R6 (hiệu năng matching) | `android/.../callscreening/monitor/PhoneStateListenerObserver.kt`, `TelephonyCallbackObserver.kt` (2 implementation riêng API 29 vs 31+ — **đã verify tồn tại thật qua `find`**); `BlocklistPrefixIndex.kt` (trie hiệu năng matching) | ⬜ **Not-started** — R1: chưa có test hồi quy trên cả 2 API level; WBS Task 7.7.3 (sanity-check 10.000 entry, xem `docs/planning/WBS-SafeCall-Phase1.md` dòng 390): **chưa chạy**, theo đúng `RISK-SAFECALL.md` R6. |

## NFR-5 — Hiệu năng (thời gian phản hồi, thời gian tải màn hình)

| Design | Code | Test |
|---|---|---|
| **Deferred to Phase 2** (SRS §4 v3.1 — stakeholder xác nhận trực tiếp 2026-10-25, KHÔNG chốt ngưỡng cho Phase 1; đây là quyết định tường minh, khác với "chưa xác định" bỏ sót trước đó) | Không có code riêng cho yêu cầu này ngoài phần trùng với NFR-4 (matching) — không gắn thêm code để tránh suy diễn một NFR đã cố ý deferred | **N/A** — không verify NFR đã bị defer có chủ đích, không phải thiếu tiêu chí do bỏ sót. |

## NFR-6 — Khả năng mở rộng (blocklist/lịch sử cuộc gọi lớn)

| Design | Code | Test |
|---|---|---|
| **Đã xác định** (SRS §4 v3.1 — stakeholder chốt 2026-10-25: **10.000 bản ghi mỗi loại**, dùng chung ngân sách WBS Task 7.7.3) | `BlocklistPrefixIndex.kt` (trie) — cùng cơ chế với NFR-4, nay chính thức phục vụ cả 2 NFR vì đã có mục tiêu số liệu rõ ràng để đo | ⬜ **Not-started** — WBS Task 7.7.3 (sanity-check 10.000 entry) **chưa chạy**; khi chạy, kết quả đóng cả NFR-4 và NFR-6 cùng lúc (cùng 1 test, 2 NFR được xác nhận). |

## NFR-7 — Tuân thủ pháp lý/quy định dữ liệu cá nhân (Nghị định 13/2023/NĐ-CP)

| Design | Code | Test |
|---|---|---|
| **Đã xác định khung áp dụng** (SRS §4 v3.1 — stakeholder xác nhận 2026-10-25: Nghị định 13/2023/NĐ-CP, Việt Nam, áp dụng cơ bản) | Không có code riêng viết MỚI cho NFR-7 — nhưng thiết kế hiện có (`ADR-003` local-only, `NFR-2` consent khi xin quyền) đã đáp ứng một phần tinh thần Nghị định 13 (tối thiểu hoá thu thập, minh bạch với người dùng) dù không được thiết kế CHỦ ĐÍCH cho khung pháp lý này | ⬜ **Not-started** — chưa có review đối chiếu TỪNG điều khoản Nghị định 13 với thiết kế hiện tại; việc xác nhận khung áp dụng (hôm nay) không tương đương việc đã review xong. |

## NFR "mồ côi" — không giấu (khác loại mồ côi với FR)

**Cập nhật 2026-10-25:** sau khi stakeholder xác nhận trực tiếp, chỉ còn **NFR-5 mồ côi TOÀN DIỆN** (không
thiết kế số liệu, không code, không test — nhưng nay là do quyết định tường minh "deferred", không phải bỏ
sót). NFR-6 và NFR-7 không còn orphan theo nghĩa cũ: cả hai nay có quyết định Design rõ ràng, chỉ còn thiếu
Test (giống nhóm FR "mồ côi test" thông thường, không phải mồ côi toàn diện). NFR-4 & NFR-6 **dùng chung 1
cơ chế code** (`BlocklistPrefixIndex.kt`) — ghi nhận rõ để không tính 2 lần thành tích "có code" cho 2 NFR
khác nhau; tương tự, 1 lần chạy Task 7.7.3 sẽ đóng CẢ HAI test cùng lúc, không phải 2 lần test riêng.

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
