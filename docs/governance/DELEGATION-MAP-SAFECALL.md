# Delegation Map — SAFECALL

> Theo Workbook EX-06 (*"bài quan trọng nhất"*). ≥15 task lấy từ `WBS-SafeCall-Phase1.md`, mỗi task gán: nhóm
> 5-loại · mức ủy quyền L0–L5 · ai gỡ chốt · Leash A/A+ (A ≈ L3, A+ ≈ L4) · lý do 1 dòng.
>
> **Ghi chú quan trọng (retroactive):** map này được viết SAU KHI Phase 1-8 đã build+test xong, không phải
> trước khi build như quy trình chuẩn đòi hỏi. Vì vậy map này có vai trò kép: (1) chuẩn hoá ủy quyền cho các
> Phase còn lại (test đa OEM thật, release); (2) **đối chiếu ngược** — chỉ ra Phase nào đã build THỰC TẾ chạy
> ở Leash nào, có khớp với Leash mà map này gán hay không. Các dòng lệch được đánh dấu rõ ⚠️ — đây chính là 2
> gap governance đã ghi trong `DECISION-LOG-SAFECALL.md` khối #3, #4.

## Bảng Delegation Map

| WBS | Task | Nhóm 5-loại | Mức L0–L5 | Ai gỡ chốt | Leash | Lý do |
|---|---|---|---|---|---|---|
| 1.1.1 | Project scaffolding (Flutter app, module Android native, Gradle/Manifest, khung channel) | AI-do | L3 | AI tự đóng khi build xanh | **A** | Rủi ro thấp, chỉ cấu trúc code/khung dự án, không chạm dữ liệu/quyền thật. Thực tế đã chạy đúng ở A. |
| 1.1.2 | RoleModule + PermissionsModule (Kotlin, xử lý `ROLE_CALL_SCREENING` + 4 runtime permission) | AI-review | L3 nominal (⚠️ đáng lẽ **A+**) | Người spot-check trước khi cài lên thiết bị thật | **A** *(thực tế)* / nên là **A+** | Chạm quyền hệ thống Android — sai cấu hình permission gây crash thật (đã xảy ra ở Task 4.1.1 tương tự, xem Decision Log #3). Lẽ ra phải để A+ chờ duyệt manifest trước khi build-và-cài, nhưng thực tế chạy thẳng ở A. |
| 1.2.1 | OnboardingScreen/PermissionScreen UI (7 bước) | AI-do | L3 | AI tự đóng khi test tay UI pass | **A** | UI thuần, không hazard, lỗi dễ thấy ngay khi test tay — đã test tay xác nhận hoạt động đúng (sau khi sửa 1 bug Dart logic). |
| 1.3.1 | Schema DDL SQLite 2 phía (Dart + Kotlin, 4 bảng + `schema_migrations`, PRAGMA) | AI-review | L3 nominal (⚠️ đáng lẽ **A+**) | Người review schema + chạy thật trước khi coi Done | **A** *(thực tế)* / nên là **A+** | Chạm toàn bộ tầng dữ liệu — lỗi PRAGMA-trong-transaction đã thực sự làm hỏng mọi màn hình đọc DB (Decision Log #4). Thay đổi DB-layer nên luôn A+ vì ảnh hưởng lan rộng không kiểm soát được bằng test cục bộ. |
| 2.1.1 | BlocklistScreen UI (list/add/delete, dialog cảnh báo contact) | AI-do | L3 | AI tự đóng khi test tay pass | **A** | UI CRUD thuần, đã test tay thành công (thêm số `0901234567` → `+84901234567`). |
| 2.2.1 | BlocklistService (Dart CRUD qua sqflite) | AI-do | L3 | AI tự đóng khi unit test pass | **A** | Logic CRUD cục bộ, rủi ro thấp, dễ rollback (xoá/sửa lại được). |
| 3.1.1 | `CallScreeningService` (Kotlin, quyết định chặn cuộc gọi, fail-open bắt buộc) | Human-review | L3 bounded | Người review kỹ logic fail-open trước khi release | **A+** | Hot-path chặn cuộc gọi thật — sai logic có thể khiến KHÔNG chặn được số lừa đảo (fail-open) hoặc chặn nhầm số hợp lệ. Đây là tính năng lõi của toàn app, hazard cao dù "chỉ đọc" blocklist. |
| 4.1.1 | `CallMonitorService` (foreground service, lắng nghe trạng thái cuộc gọi) | AI-review | L3 nominal (⚠️ đã xảy ra **A+**) | Người duyệt permission/manifest trước khi cài lên thiết bị thật | **A** *(thực tế)* / nên là **A+** | Đây chính là Task gây crash `SecurityException` thật (Decision Log #3) — minh chứng trực tiếp cho lý do task loại này cần A+. |
| 4.2.3 | `PendingPromptRegistry` + `resolvePromptAtomically` (first-resolution-wins, concurrency) | Human-review | L3 bounded | Người review logic atomic trước khi coi Done | **A+** | Race condition sai → có thể hỏi xác nhận 2 lần cho cùng cuộc gọi hoặc mất phản hồi người dùng — khó phát hiện bằng test tay thông thường, cần review logic kỹ. |
| 6.1.2 | `RingingScamOverlay` (hiển thị đè màn hình lúc đổ chuông) | Human-review | L3 bounded | Người duyệt trải nghiệm + hành vi overlay trước khi release | **A+** | Đè lên ứng dụng khác đang mở — hazard UX (có thể chặn tương tác người dùng với app khác) + liên quan trực tiếp tới hành động không thể hoàn tác ở Task 6.2.1 ngay sau đây. |
| 6.2.1 | `CallMonitorService` mở rộng FR-8: `resolveRingingYes`, gọi `endCall()` | Human-review | **L4** (Operate workflow, có kiểm soát) | Người đích thân duyệt logic trước khi release — không chỉ spot-check | **A+** | Hành động ngắt cuộc gọi là **KHÔNG THỂ HOÀN TÁC** (SRS NFR-3) — mức hazard cao nhất trong toàn bộ SafeCall. Đây là 1 trong 2 Task duy nhất của dự án đáng để xem xét L4 thay vì L3, và tuyệt đối không được lên L5. |
| 6.4.2 | Schema migration 2 phía cho FR-8 (thêm cột `call_history`/`pending_prompts`, đổi NOT NULL→NULLABLE, CHECK constraint mới) | Human-review | L3 bounded | Người review migration trước khi release (không rollback được sau khi user đã cài) | **A+** | Thay đổi schema migration trên dữ liệu NGƯỜI DÙNG ĐÃ CÓ — sai sót không thể sửa ngược dễ dàng sau khi phát hành, khác hẳn schema DDL ban đầu (1.3.1) vốn chưa có dữ liệu thật nào để mất. |
| 7.2.1 | `BlocklistPrefixIndex` (trie in-memory, version-counter cross-process) | Human-review | L3 bounded | Người review + chạy sanity-check hiệu năng trước khi coi Done | **A+** | Sai cấu trúc trie hoặc cache không refresh đúng → ảnh hưởng chặn real-time của TOÀN BỘ entry loại prefix cùng lúc, không phải lỗi cục bộ 1 bản ghi. |
| 7.4.2 | `BlocklistImportService` (file picker, parse JSON, validate, `mergeEntriesInTransaction`) | Human-review | L3 bounded | Người review riêng phần validate input (an toàn khi parse file từ nguồn ngoài) | **A+** | Nhận file từ bên ngoài thiết bị (qua file picker) — mặt tấn công khác với các Task còn lại (input không tin cậy), cộng thêm rủi ro dữ liệu "nửa vời" nếu transaction sai (FR-10/AC10). |
| 7.6.1 | Schema migration FR-9/FR-10 (UNIQUE composite, bảng `blocklist_meta`, 2 trigger SQL) | Human-review | L3 bounded | Người review migration + trigger logic trước khi release | **A+** | Cùng lý do 6.4.2 — migration trên dữ liệu người dùng đã có, cộng thêm trigger SQL (logic ẩn, khó debug nếu sai, ảnh hưởng ngầm tới mọi write sau đó). |
| 8.2.1 | Security cơ bản (loại trừ `allowBackup`, xoá dữ liệu khi gỡ app, sanity SQL injection) | Human-review | **L2** (Recommend — cố ý giữ thấp) | Người đích thân duyệt, không spot-check | **A+** | **Cố ý giữ ở L2 dù AI hoàn toàn có thể tự thực hiện việc này** — đây là cổng bảo mật cuối cùng trước release, không được để AI tự đánh giá "đủ an toàn" cho chính code AI viết ra (tự chấm bài). |
| 8.3.1 | UAT (điều phối + fix phản hồi mức blocker) | Human-do | **L1** (Draft) | Người dùng thật quyết định Pass/Fail | **A+** | Cần phản hồi từ NGƯỜI DÙNG THẬT, không phải AI mô phỏng — AI chỉ được tổng hợp/tóm tắt phản hồi, không được tự kết luận "đạt UAT". |
| 6.5.3 | Vòng test thiết bị thật đa OEM (Samsung/Xiaomi/Oppo, Pixel) | AI-không-nên | **L0** (Observe) | Người cầm thiết bị thật thực hiện, AI chỉ tổng hợp kết quả | — *(không áp dụng Leash — AI không thực thi được)* | Cần thiết bị vật lý thật đa hãng — nằm ngoài khả năng AI hoàn toàn, không phải vấn đề mức ủy quyền mà là giới hạn năng lực thực thi. |
| — | Release / publish lên Play Store (ngoài WBS, nhưng là bước tất yếu) | Human-do | **L1** | Người đích thân bấm nút release | **A+** | 🔒 Luật cứng #1 của Operating Model: AI không tự push/merge/release trong MỌI trường hợp, không ngoại lệ. |
| — | *(Bổ sung 2026-10-25)* Tạo keystore + điền `android/key.properties` (giá trị secret thật) | Human-do | **L1** | Người tự chạy `keytool`, tự nhập mật khẩu | **A+** | 🔒 Luật cứng #2: AI không chạm/tạo/biết secret (R14). AI chỉ chuẩn bị cơ chế gradle đọc file, không tự điền giá trị — xem `docs/release/RELEASE-SIGNING-SAFECALL.md`. |
| — | *(Bổ sung 2026-10-25)* Cấu hình gradle đọc `signingConfigs.release` từ `key.properties` (`android/app/build.gradle`) | AI-do | L3 | AI tự viết, người đọc lại diff trước khi dùng | **A** | Chỉ là code đọc property file — KHÔNG tự tạo/biết giá trị secret (tách biệt rõ với dòng ngay trên). Sai chỉ gây lỗi build, không rò rỉ gì. |
| — | *(Bổ sung 2026-10-25)* Golden-file test đối chiếu `PhoneNumberNormalizer` Dart ↔ Kotlin (`test/`, `android/.../src/test/`) — đóng Risk R9 | AI-do | L3 | AI tự viết + tự verify bằng oracle Python độc lập (58/58 khớp); người tự chạy `flutter test`/`gradle test` thật để xác nhận cuối (môi trường AI không có 2 toolchain này) | **A** | Test code không chạm production runtime — sai chỉ làm test đỏ, không ảnh hưởng người dùng thật. Đúng mức A, không cần A+ dù chạm logic dual-implementation (khác 1.3.1/4.1.1 vì đây không phải code production, không tự chạy trên thiết bị thật). |

## Giải trình bắt buộc (≥1 dòng lý do cho mỗi task ≥ L3)

Đã ghi trực tiếp trong cột "Lý do" ở bảng trên cho từng task. Tổng hợp lại các task ở mức L3 trở lên và vì sao:
tất cả đều vì chạm **quyền hệ thống** (1.1.2, 4.1.1), **toàn bộ tầng dữ liệu/migration** (1.3.1, 6.4.2, 7.6.1),
**hot-path chặn cuộc gọi lõi** (3.1.1), **hành động không thể hoàn tác** (6.2.1 — mức cao nhất, cận L4), hoặc
**mặt tấn công từ input ngoài không tin cậy** (7.4.2).

## Task cố ý giữ ở L1/L2 dù AI "làm được"

- **8.2.1 (Security cơ bản) → L2.** AI hoàn toàn có khả năng tự viết và tự kiểm code loại trừ `allowBackup`,
  xoá dữ liệu, sanity SQL injection — nhưng đây là cổng bảo mật cuối, không để AI tự chấm bài chính code mình
  viết ra. Người phải đích thân duyệt.
- **8.3.1 (UAT) → L1.** AI có thể tự generate kịch bản test và tự "đóng vai" người dùng để đánh giá — nhưng
  UAT đúng nghĩa đòi hỏi phản hồi từ người dùng thật, không phải AI mô phỏng cảm nhận người dùng.

## Đối chiếu với thực tế build (phần quan trọng nhất của map này)

2 dòng ⚠️ ở trên (1.1.2/4.1.1 chạm quyền hệ thống, và 1.3.1 chạm DB-layer) là nơi **Leash thực tế đã chạy** (A)
**lệch** với Leash **đáng lẽ phải gán** (A+) theo chính map này. Đây không phải lỗi hồi tố để đổ lỗi — đây là
bằng chứng cụ thể cho thấy: nếu Delegation Map này tồn tại TRƯỚC khi build (đúng thứ tự chuẩn của Build
Playbook bước [6] trước bước [7]/[8]), 2 sự cố ghi trong `DECISION-LOG-SAFECALL.md` khối #3 và #4 **lẽ ra có
cơ hội được chặn ở cổng người-duyệt-manifest/người-duyệt-schema TRƯỚC khi cài lên thiết bị thật**, thay vì chỉ
được bắt SAU khi đã crash/hỏng dữ liệu. Bài học áp dụng cho các Phase/Task còn lại (test OEM thật, release):
Delegation Map phải đi TRƯỚC bước build, không phải sau.

## Sign-off retroactive A+ cho 3 gap ⚠️ (2026-10-25)

Người dùng (vai trò stakeholder/người duyệt) đã tự xem lại bằng chứng kỹ thuật thật cho từng gap — không phải
duyệt bừa theo mô tả — rồi xác nhận **chấp nhận đóng cả 3 (retroactive)** qua `AskUserQuestion`:

| WBS | Bằng chứng đã xem trước khi duyệt | Quyết định |
|---|---|---|
| 1.1.2 | `PermissionsModule.kt` xử lý đủ 3 trạng thái NOT_REQUESTED/DENIED/GRANTED; test FR-7 Tested-Pass; không có incident cụ thể ghi nhận (chỉ flag theo loại rủi ro) | ✅ Đóng retroactive |
| 1.3.1 | Verify lại code thật 2026-10-25: PRAGMA vẫn đặt đúng trong `onConfigure` ở cả `SafeCallDatabaseHelper.kt` (Kotlin) và `database_provider.dart` (Dart), fix từ Decision Log #4 còn nguyên sau lần bump AGP/Kotlin | ✅ Đóng retroactive |
| 4.1.1 | Verify lại manifest thật 2026-10-25: `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` còn nguyên, fix từ Decision Log #3 không bị mất | ✅ Đóng retroactive |

**Lưu ý quan trọng — đóng ≠ xoá lịch sử:** cả 3 dòng ⚠️ ở bảng Delegation Map phía trên **vẫn giữ nguyên**,
vì sự thật "Leash thực tế đã chạy ở A trong khi đáng lẽ A+" không đổi — đó là lịch sử, sign-off không viết
lại lịch sử. Điều thay đổi là: 3 gap này nay có **bằng chứng review thật của người (không phải AI tự chấm)**,
nên không còn là rủi ro "chưa ai biết/chưa ai xem" — `RISK-SAFECALL.md` R13 cập nhật tương ứng. Quy tắc cho
Task còn lại (test OEM, release) không đổi: mọi task chạm Layer 0 (xem `MODULEMAP-SAFECALL.md`) vẫn phải tự
động xét A+ TRƯỚC khi build, không chờ review retroactive như lần này.
