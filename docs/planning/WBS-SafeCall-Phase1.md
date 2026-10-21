# Work Breakdown Structure: SafeCall Phase 1 (Android, local-only call-blocking)

## Nguồn
- Phân rã công việc + effort (best/likely/worst, person-day): `docs/planning/estimate.md`.
- Thứ tự Phase & phụ thuộc giữa Phase: `docs/design/HLD-SafeCall-Phase1.md`, mục "Phase breakdown".
- *Cập nhật (2026-10-21):* thêm **Phase 6** (FR-8/SCAM-DETECT-05, xác nhận lừa đảo lúc đổ chuông + ngắt cuộc
  gọi), ánh xạ từ "Phase 1.6" mới trong `estimate.md` (bổ sung cùng ngày, theo cascade SRS v2.0/HLD v1.1/LLD
  v1.1/ERD v1.1/UI-UX v1.1/Test Plan v1.1, ADR-005). Phase 1-5 cũ **giữ nguyên không chỉnh sửa**. Phase
  "Cross-cutting" cũ của tài liệu này (trước đây đánh số Phase 6) được **đổi số thành Phase 7** để Phase 6 mới
  (FR-8) chèn đúng vị trí theo thứ tự `estimate.md` (Phase 1.6 nằm giữa Phase 1.5 và mục Cross-cutting) —
  không có Task/Deliverable nào khác bị đổi số ngoài việc đổi nhãn Phase 6 → Phase 7 này.
- *Cập nhật (2026-10-21, lần 2):* thêm **Phase 7** (FR-9/BLOCK-03 chặn theo đầu số + FR-10/BLOCK-04
  export/import blocklist), ánh xạ từ "Phase 1.7" mới trong `estimate.md` (bổ sung cùng ngày, theo cascade
  SRS v3.0/HLD v1.2/LLD v1.3/ERD v1.2/Test Plan v1.2). Phase 1-6 cũ **giữ nguyên không chỉnh sửa**. Phase
  "Cross-cutting" của tài liệu này (trước đây đánh số Phase 7, sau lần cập nhật đầu đổi từ Phase 6) được
  **đổi số thành Phase 8** để Phase 7 mới (FR-9/FR-10) chèn đúng vị trí theo thứ tự `estimate.md` (Phase 1.7
  nằm giữa Phase 1.6 và mục Cross-cutting) — không có Task/Deliverable nào khác bị đổi số ngoài việc đổi nhãn
  Phase 7 → Phase 8 này.
- Tài liệu này CHỈ cấu trúc hóa lại dữ liệu đã có ở trên theo cây Phase → Deliverable → Task.
  Effort của mỗi Task là giá trị **likely-case nguyên bản** lấy từ `estimate.md`; **Deliverable là nhóm do
  agent này tạo ra để tổ chức các Task đã có theo thành phần kỹ thuật cùng nhóm** (không phải Task/Deliverable
  mới ngoài input — chỉ là cách gom nhóm các dòng đã có trong bảng ước lượng).
- Không có cấp Subtask: input (`estimate.md`) không phân rã Task nào xuống mức chi tiết hơn, nên không tự
  tách Subtask (kể cả 17 dòng việc của Phase 1.7, FR-9/FR-10 mới bổ sung).

Quy ước effort hiển thị mỗi Task: `Best / Likely / Worst (person-day)`. Số **in đậm** dùng trong tổng Phase/
Deliverable là tổng cộng (sum) của các Task con theo đúng 3 cột, để tiện đối chiếu ngược với `estimate.md`.

---

## Phase 1 — Nền tảng: Onboarding, quyền/role, schema dữ liệu chung

Phụ thuộc: Không phụ thuộc Phase nào khác — nền tảng cho mọi Phase sau (nguồn: HLD "Phase breakdown",
Phase 1.1: "không phụ thuộc thành phần nào khác").
Milestone/tiêu chí hoàn thành: Chưa xác định (input không nêu tiêu chí hoàn thành cụ thể cho Phase, chỉ nêu
quan hệ phụ thuộc).

### 1.1 Scaffolding & Bridge quyền/role nền tảng

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 1.1.1 | Project scaffolding (khởi tạo Flutter app, cấu trúc module Android native, Gradle/Manifest, khung MethodChannel/EventChannel) | 1 | 1.5 | 3 |
| 1.1.2 | RoleModule + PermissionsModule (Kotlin, xử lý ROLE_CALL_SCREENING + 4 runtime permission qua MethodChannel) | 1.5 | 2.5 | 4 |
| 1.1.3 | PermissionsBridge (Dart client MethodChannel) | 0.5 | 1 | 1.5 |
| **Tổng 1.1** | | **3** | **5** | **8.5** |

### 1.2 Onboarding/Permission UI

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 1.2.1 | OnboardingScreen/PermissionScreen UI (7 bước: welcome, rationale x4, denied-mandatory, banner danh bạ, summary, resume-check) | 2 | 3.5 | 6 |
| **Tổng 1.2** | | **2** | **3.5** | **6** |

### 1.3 Schema dữ liệu SQLite chung

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 1.3.1 | Schema DDL SQLite 2 phía (4 bảng + schema_migrations, WAL/PRAGMA, CREATE TABLE IF NOT EXISTS idempotent chống race) | 1.5 | 2.5 | 4 |
| **Tổng 1.3** | | **1.5** | **2.5** | **4** |

### 1.4 Kiểm thử Phase 1

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 1.4.1 | Unit test (trạng thái quyền, idempotency tạo schema) | 1 | 1.5 | 2.5 |
| 1.4.2 | Integration test (race "ai mở file trước" giữa Dart và native) | 0.5 | 1 | 2 |
| **Tổng 1.4** | | **1.5** | **2.5** | **4.5** |

**Tổng Phase 1 (1.1+1.2+1.3+1.4): Best 8 / Likely 13.5 / Worst 23** — khớp đúng với "Tổng Phase 1.1" trong
`estimate.md` (8 / 13.5 / 23).

Phụ thuộc nội bộ Phase 1 (suy luận kỹ thuật của agent, **chưa được input xác nhận trực tiếp** — cần người
dùng/system-architect xác nhận lại nếu cần chính xác tuyệt đối):
- 1.1.1 (scaffolding) phải xong trước 1.1.2, 1.1.3, 1.2.1, 1.3.1 — các task này đều cần khung module/bridge đã dựng.
- 1.3.1 (schema DDL) nên xong trước 1.4.2 (integration test race file SQLite) vì cần schema tồn tại để test.

---

## Phase 2 — Quản lý blocklist thủ công

Phụ thuộc (nguồn: HLD "Phase breakdown", Phase 1.2): "Phụ thuộc: schema SQLite từ 1.1; không phụ thuộc
`CallScreeningService` — có thể phát triển/test độc lập." → Phase 2 phụ thuộc Phase 1 (schema), độc lập với Phase 3.
Milestone/tiêu chí hoàn thành: Chưa xác định.

### 2.1 UI quản lý blocklist

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 2.1.1 | BlocklistScreen UI (list/add/delete, empty/error/loading, dialog cảnh báo contact AC8) | 1.5 | 2.5 | 4 |
| **Tổng 2.1** | | **1.5** | **2.5** | **4** |

### 2.2 Logic & service blocklist (Dart)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 2.2.1 | BlocklistService (Dart CRUD qua sqflite) | 0.5 | 1 | 1.5 |
| 2.2.2 | PhoneNumberNormalizer (Dart, validate + chuẩn hóa E.164) | 0.5 | 1 | 2 |
| **Tổng 2.2** | | **1** | **2** | **3.5** |

### 2.3 Kiểm thử Phase 2

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 2.3.1 | Unit test (addNumber 3 nhánh, normalizer bộ input mẫu) | 0.5 | 1 | 1.5 |
| 2.3.2 | Integration test (UNIQUE constraint thật, kiểm tra contact thật qua Contacts provider) | 0.5 | 1 | 1.5 |
| **Tổng 2.3** | | **1** | **2** | **3** |

**Tổng Phase 2 (2.1+2.2+2.3): Best 3.5 / Likely 6.5 / Worst 10.5** — khớp đúng với "Tổng Phase 1.2" trong
`estimate.md` (3.5 / 6.5 / 10.5).

Phụ thuộc nội bộ Phase 2 (suy luận kỹ thuật của agent, chưa được input xác nhận trực tiếp):
- 1.3.1 (schema DDL, Phase 1) phải xong trước 2.2.1 (BlocklistService cần bảng blocklist tồn tại).

---

## Phase 3 — Chặn real-time

Phụ thuộc (nguồn: HLD "Phase breakdown", Phase 1.3): "Phụ thuộc: role đã cấp (1.1) và bảng blocklist tồn tại."
Milestone/tiêu chí hoàn thành: Chưa xác định.

### 3.1 CallScreeningService & dữ liệu hỗ trợ (Kotlin)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 3.1.1 | CallScreeningService (Kotlin, quyết định chặn, fail-open bắt buộc khi timeout/exception) | 1 | 2 | 3.5 |
| 3.1.2 | BlocklistRepository (đọc, isBlocked) | 0.5 | 1 | 1.5 |
| 3.1.3 | PhoneNumberNormalizer (Kotlin, song song bản Dart) | 0.5 | 1 | 2 |
| **Tổng 3.1** | | **2** | **4** | **7** |

### 3.2 Kiểm thử Phase 3

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 3.2.1 | Unit test (mock timeout/exception, luôn fail-open) | 0.5 | 1 | 1.5 |
| 3.2.2 | Integration test (WAL/locking thật: ghi đồng thời + đo thời gian đọc dưới busy_timeout=1000 so với ngân sách ~1500ms) | 1 | 2 | 3.5 |
| **Tổng 3.2** | | **1.5** | **3** | **5** |

**Tổng Phase 3 (3.1+3.2): Best 3.5 / Likely 7 / Worst 12** — khớp đúng với "Tổng Phase 1.3" trong
`estimate.md` (3.5 / 7 / 12).

Phụ thuộc nội bộ/liên Phase:
- Role đã cấp ở 1.1.2 và bảng blocklist tồn tại (1.3.1, Phase 1) + schema blocklist Phase 2 — nguồn: HLD
  Phase breakdown (trực tiếp từ input).
- 3.1.3 (PhoneNumberNormalizer Kotlin) phải cho cùng output với 2.2.2 (PhoneNumberNormalizer Dart) — đây là
  một ràng buộc nhất quán được chính `estimate.md` nêu rõ ("rủi ro lệch kết quả"), không phải thứ tự trước-sau
  bắt buộc, chỉ là yêu cầu đồng bộ kết quả giữa hai task độc lập.

---

## Phase 4 — Phát hiện cuộc gọi kết thúc + prompt xác nhận

Phụ thuộc (nguồn: HLD "Phase breakdown", Phase 1.4): "Phụ thuộc: Phase 1.3 (bảng blocklist,
`BlocklistRepository`), Phase 1.1 (quyền READ_PHONE_STATE, POST_NOTIFICATIONS)." Câu hỏi mở ghi trong HLD:
"cơ chế kỹ thuật cụ thể cho NFR-4 (độ tin cậy khi background)" chưa được `low-level-designer` chốt — ảnh hưởng
trực tiếp đến worst-case của Task 4.1.1 (xem mục Bất thường/rủi ro).
Milestone/tiêu chí hoàn thành: Chưa xác định.

### 4.1 CallMonitorService & nguồn dữ liệu cuộc gọi

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 4.1.1 | CallMonitorService (foreground service, lắng nghe trạng thái cuộc gọi, cần xử lý khác biệt API 29 vs API 31+) | 3 | 5 | 9 |
| 4.1.2 | Lấy số gọi đến qua CallLog content provider (tách khỏi role) | 1 | 1.5 | 2.5 |
| 4.1.3 | evaluatePromptEligibility (8 nhánh, biên ngưỡng 2.9s/3.0s, tra cứu contact có xử lý lỗi) | 1 | 1.5 | 2.5 |
| **Tổng 4.1** | | **5** | **8** | **14** |

### 4.2 Luồng prompt xác nhận đảm bảo (notification + receiver + concurrency)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 4.2.1 | Notification xác nhận (heads-up, 2 action button Có/Không) | 1 | 1.5 | 2.5 |
| 4.2.2 | ScamConfirmationReceiver (BroadcastReceiver, kênh đảm bảo) | 0.5 | 1 | 1.5 |
| 4.2.3 | PendingPromptRegistry + resolvePromptAtomically (first-resolution-wins, ConcurrentHashMap.computeIfPresent) | 1 | 2 | 3.5 |
| 4.2.4 | pending_prompts durable fallback + phục hồi sau process kill (UPDATE ... WHERE resolved=0) | 1.5 | 2.5 | 4.5 |
| **Tổng 4.2** | | **4** | **7** | **12** |

### 4.3 Bridge & UI in-app xác nhận (tăng cường, chỉ foreground)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 4.3.1 | CallEventBridge (EventChannel) + ScamConfirmationDialog (Dart UI, xử lý ALREADY_RESOLVED/EXPIRED) | 1.5 | 2.5 | 4 |
| **Tổng 4.3** | | **1.5** | **2.5** | **4** |

### 4.4 Kiểm thử Phase 4

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 4.4.1 | Unit test (evaluatePromptEligibility mọi nhánh, resolvePromptAtomically 2 luồng đồng thời, cancelActivePromptFor) | 1 | 2 | 3 |
| 4.4.2 | Integration test (race 20+ lần lặp, phục hồi pending_prompts ở 3 mốc kill-timing, độ tin cậy CallLog/TelephonyCallback qua 2 API level) | 2 | 4 | 7 |
| **Tổng 4.4** | | **3** | **6** | **10** |

**Tổng Phase 4 (4.1+4.2+4.3+4.4): Best 13.5 / Likely 23.5 / Worst 40** — so với "Tổng Phase 1.4" trong
`estimate.md` (14 / 23.5 / 40.5): Likely khớp đúng (23.5); Best và Worst lệch 0.5 person-day. Đây là sai số
cộng dồn (rounding) có sẵn trong bảng gốc của `estimate.md` (ví dụ nhiều dòng 0.5 cộng lại ra số lẻ khác biệt
±0.5 tùy thứ tự cộng) — xem mục "Bất thường phát hiện" để biết chi tiết, KHÔNG tự làm tròn để khớp.

Phụ thuộc:
- Phase 4 phụ thuộc Phase 3 (bảng blocklist, `BlocklistRepository`) và Phase 1 (quyền READ_PHONE_STATE,
  POST_NOTIFICATIONS) — nguồn: HLD Phase breakdown (trực tiếp từ input).
- 4.1.1 (CallMonitorService) nên xong trước 4.1.2, 4.1.3, 4.2.1 (các task đọc trạng thái cuộc gọi/điều kiện
  đều cần service nền đã lắng nghe được) — suy luận kỹ thuật của agent, chưa được input xác nhận trực tiếp.

---

## Phase 5 — Lịch sử cuộc gọi

Phụ thuộc (nguồn: HLD "Phase breakdown", Phase 1.5): "Phụ thuộc: `CallMonitorService` (1.4) đã tồn tại; độc
lập kỹ thuật với 1.2/1.3 nhưng làm sau cùng theo ưu tiên nghiệp vụ."
Milestone/tiêu chí hoàn thành: Chưa xác định.

### 5.1 Repository & service lịch sử

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 5.1.1 | CallHistoryRepository (viết, tích hợp vào CallMonitorService của Phase 4) | 1 | 1.5 | 2.5 |
| 5.1.2 | CallHistoryService (Dart, đọc có phân trang) | 0.5 | 1 | 1.5 |
| **Tổng 5.1** | | **1.5** | **2.5** | **4** |

### 5.2 UI lịch sử cuộc gọi

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 5.2.1 | CallHistoryScreen UI (list phân trang, 3 loại badge trạng thái, 4 trạng thái màn hình) | 1 | 2 | 3 |
| **Tổng 5.2** | | **1** | **2** | **3** |

### 5.3 Kiểm thử Phase 5

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 5.3.1 | Unit/integration test (mapping badge, phân trang, tính nhất quán chỉ-đọc) | 0.5 | 1 | 1.5 |
| **Tổng 5.3** | | **0.5** | **1** | **1.5** |

**Tổng Phase 5 (5.1+5.2+5.3): Best 3 / Likely 5.5 / Worst 8.5** — khớp đúng với "Tổng Phase 1.5" trong
`estimate.md` (3 / 5.5 / 8.5).

Phụ thuộc:
- 4.1.1 (CallMonitorService, Phase 4) phải xong trước 5.1.1 — nguồn: `estimate.md`, căn cứ của Task 5.1.1
  ghi rõ "phụ thuộc trực tiếp vào CallMonitorService đã hoàn thành ở 1.4" (trực tiếp từ input, không phải suy
  luận của agent).

---

## Phase 6 — Xác nhận lừa đảo lúc đổ chuông + ngắt cuộc gọi (FR-8/SCAM-DETECT-05, mới)

Phụ thuộc (nguồn: HLD v1.1 "Phase breakdown", Phase 1.6): "Phase 1.4 phải hoàn thành trước (mở rộng chứ
không thay thế cơ chế đó). Cũng phụ thuộc Phase 1.1 và Phase 1.5." → ánh xạ sang số Phase của WBS này: Phase
6 phụ thuộc **Phase 4** (phải hoàn thành trước — không làm trước Phase 6), **Phase 1** và **Phase 5**. Đây là
phụ thuộc lấy trực tiếp từ HLD, không phải suy luận của agent.
Milestone/tiêu chí hoàn thành: Chưa xác định (Test Plan v1.1 có "Exit Criteria" riêng cho FR-8 được
`estimate.md` tham chiếu nhưng không trích dẫn chi tiết tiêu chí vào bảng ước lượng — không tự suy diễn nội
dung Exit Criteria vào đây).

### 6.1 Spike/PoC & RingingScamOverlay (cơ chế hiển thị lúc đổ chuông)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 6.1.1 | Spike/PoC chọn phương án hiển thị `RingingScamOverlay` (A - window overlay `SYSTEM_ALERT_WINDOW` vs B - full-screen-intent Notification), kiểm chứng độ tin cậy "đè đúng lúc" thực nghiệm trên 1-2 thiết bị trước khi code chính thức | 1 | 2 | 4 |
| 6.1.2 | `RingingScamOverlay` (Kotlin, MỚI - `show`/`onYesPressed`/`onNoPressed`/`dismiss`, tự đóng khi đổi trạng thái AC7, hiện lại nếu bị bỏ qua AC9, vòng đời độc lập Activity/Flutter engine) | 2 | 3.5 | 7 |
| **Tổng 6.1** | | **3** | **5.5** | **11** |

### 6.2 CallMonitorService & prompt atomicity mở rộng (lõi FR-8)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 6.2.1 | `CallMonitorService` mở rộng - lõi FR-8: `evaluateRingingPromptEligibility` (E3), `isStillRingingWithSameNumber` (tái xác thực NGAY TRƯỚC `endCall()`), `resolveRingingYes` (E4, map đủ 4 `CallTerminationOutcome`), `onCallStateChanged` lắng nghe RINGING sớm hơn | 3 | 5 | 9 |
| 6.2.2 | `resolvePromptAtomically` mở rộng nhánh `RINGING_OVERLAY` + `PendingPromptRegistry` mở rộng (`kind`, `setTerminationOutcome`, `findResolvedRingingPromptsFor`) + `flushPendingTerminationOutcomeIfAny` | 1.5 | 3 | 5 |
| **Tổng 6.2** | | **4.5** | **8** | **14** |

### 6.3 Quyền & Onboarding mở rộng (ANSWER_PHONE_CALLS)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 6.3.1 | `PermissionsModule` (Kotlin) + `PermissionsBridge` (Dart) mở rộng tham số `ANSWER_PHONE_CALLS` | 0.5 | 1 | 2 |
| 6.3.2 | `OnboardingScreen`/`PermissionScreen` mở rộng (bước rationale MỚI, tùy chọn, cho `ANSWER_PHONE_CALLS` theo NFR-2; KHÔNG kích hoạt resume-check khi bị từ chối, khác hành vi quyền Danh bạ) | 1 | 1.5 | 3 |
| **Tổng 6.3** | | **1.5** | **2.5** | **5** |

### 6.4 Lịch sử cuộc gọi & schema migration mở rộng

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 6.4.1 | `CallHistoryRepository` mở rộng (`setConfirmationSource`, `updateTerminationOutcome`) + `CallHistoryScreen` mở rộng badge (4 badge outcome mới suy ra từ `termination_outcome`, yêu cầu accessibility riêng) | 1 | 2 | 3.5 |
| 6.4.2 | Schema migration 2 phía (Dart sqflite + Kotlin SQLiteOpenHelper): thêm 2 cột `call_history` (`confirmation_source`, `termination_outcome`) + 2 cột `pending_prompts` (`kind`, `termination_outcome`) + đổi `pending_prompts.call_record_id` từ NOT NULL sang NULLABLE + 5 CHECK constraint mới + 1 index mới | 2 | 4 | 7 |
| **Tổng 6.4** | | **3** | **6** | **10.5** |

### 6.5 Kiểm thử FR-8

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 6.5.1 | Unit test FR-8 (`evaluateRingingPromptEligibility` mọi nhánh reason, `resolveRingingYes` đủ 4 outcome mock `telecomManager`, nhánh NO của `resolvePromptAtomically`/`RINGING_OVERLAY`, `flushPendingTerminationOutcomeIfAny` buffer set/flush) | 1 | 2 | 3.5 |
| 6.5.2 | Integration test race FR-8 (a)/(b) (fake `TelephonyCallback` timing-controlled) + race (c) (P1 `POST_CALL` bị cancel bởi P2 `RINGING` cùng số) + phục hồi `pending_prompts` mở rộng cho `kind=RINGING` (3 mốc kill-timing) | 2 | 4 | 7 |
| 6.5.3 | Vòng test thiết bị thật đa OEM (ma trận BẮT BUỘC 3 thiết bị: Samsung One UI, Xiaomi/HyperOS hoặc Oppo ColorOS, Pixel/AOSP-gần-chuẩn) — test AC1/AC5/AC10 + race (b) thủ công trên MỖI thiết bị, ghi nhận kết quả | 3 | 6 | 12 |
| 6.5.4 | Vòng fix phát sinh riêng theo OEM (áp dụng nếu 1/3 thiết bị Fail AC5/AC10 ở vòng trên) | 0 | 2 | 6 |
| **Tổng 6.5** | | **6** | **14** | **28.5** |

### 6.6 UAT & tài liệu kỹ thuật riêng FR-8

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 6.6.1 | UAT bổ sung riêng cho FR-8 (cảm nhận overlay/ngắt cuộc gọi, tách biệt UAT luồng onboarding/UI chung của Phase 1 gốc, chỉ sau khi vòng thiết bị thật đa OEM đã Pass) | 0.5 | 1 | 2 |
| 6.6.2 | Cập nhật tài liệu kỹ thuật riêng cho FR-8 (ghi chú vận hành "điểm gọi endCall() duy nhất" theo ADR-005, ghi chú migration schema 2 phía, đối chiếu docs FR-8 vs code thật) | 0.25 | 0.5 | 1 |
| **Tổng 6.6** | | **0.75** | **1.5** | **3** |

**Tổng Phase 6 (6.1+6.2+6.3+6.4+6.5+6.6): Best 18.75 / Likely 37.5 / Worst 72** — khớp đúng với "Tổng Phase
1.6" trong `estimate.md` (18.75 / 37.5 / 72, không có sai số cộng dồn như Phase 4). PERT-weighted theo
`estimate.md` ≈ 40.1 person-day (không tính lại ở đây vì đó là phép tính tổng hợp không phải effort cấp Task).

Phụ thuộc nội bộ Phase 6 (suy luận kỹ thuật của agent, **chưa được input xác nhận trực tiếp** — ngoại trừ
phụ thuộc cấp Phase đã nêu ở trên lấy trực tiếp từ HLD):
- 4.1.1 (`CallMonitorService` gốc, Phase 4) phải xong trước 6.2.1 — 6.2.1 mở rộng trực tiếp cùng class, không
  thay thế.
- 4.2.3/4.2.4 (`PendingPromptRegistry` + `pending_prompts` durable fallback, Phase 4) phải xong trước 6.2.2 —
  `estimate.md` tự mô tả 6.2.2 có tính chất "ADDITIVE/tái dùng hạ tầng có sẵn".
- 1.1.2 (`PermissionsModule`, Phase 1) phải xong trước 6.3.1; 1.2.1 (`OnboardingScreen`, Phase 1) phải xong
  trước 6.3.2 — cả hai Task 6.3.x đều mở rộng module/màn hình đã dựng ở Phase 1.
- 5.1.1/5.2.1 (`CallHistoryRepository`/`CallHistoryScreen`, Phase 5) phải xong trước 6.4.1; 1.3.1 (schema DDL,
  Phase 1) là nền cho 6.4.2 (migration cần schema gốc đã tồn tại để sửa đổi).
- 6.1.1 (spike/PoC) nên xong trước 6.1.2 (code chính thức `RingingScamOverlay`) — theo đúng mục đích của
  spike đã nêu trong `estimate.md` ("kiểm chứng... trước khi code chính thức").
- 6.1.2, 6.2.1, 6.2.2, 6.3.1 nên xong trước 6.5.1/6.5.2 (test cần code tồn tại); 6.5.1/6.5.2 nên xong trước
  6.5.3 — nguồn trực tiếp từ `estimate.md` ("Test Plan Entry Criteria bắt buộc nhóm này PASS trước khi bắt
  đầu Integration/System cho FR-8"); 6.5.3 phải xong trước 6.5.4 (fix chỉ phát sinh NẾU vòng thiết bị thật
  phát hiện lỗi) và trước 6.6.1 — nguồn trực tiếp từ `estimate.md` ("UAT riêng cho FR-8... đã hoàn tất SAU KHI
  vòng thiết bị thật đa OEM đã Pass").

---

## Phase 7 — Chặn theo đầu số + Export/Import blocklist (FR-9/BLOCK-03, FR-10/BLOCK-04, mới)

Phụ thuộc (nguồn: HLD v1.2 "Phase breakdown (bổ sung)", Phase 1.7): "Phụ thuộc: Phase 1.2 phải hoàn thành
trước (UI/data layer blocklist cơ bản). Cũng phụ thuộc Phase 1.3 (chặn real-time exact đã có — prefix là mở
rộng trực tiếp). KHÔNG phụ thuộc Phase 1.4/1.5/1.6 — độc lập về runtime, chỉ dùng chung bảng `blocklist`." →
ánh xạ sang số Phase của WBS này: Phase 7 phụ thuộc **Phase 2** và **Phase 3** (phải hoàn thành trước — không
làm trước Phase 7), độc lập về runtime với **Phase 4**, **Phase 5**, **Phase 6**. Đây là phụ thuộc lấy trực
tiếp từ HLD, không phải suy luận của agent.
Milestone/tiêu chí hoàn thành: Chưa xác định (Test Plan v1.2 có Exit Criteria riêng cho FR-9/FR-10 — bao gồm
sanity-check hiệu năng BẮT BUỘC Pass — được `estimate.md` tham chiếu nhưng không trích dẫn chi tiết tiêu chí
vào bảng ước lượng — không tự suy diễn nội dung Exit Criteria vào đây).

### 7.1 Blocklist CRUD & normalizer mở rộng (entryType)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.1.1 | `BlocklistService` (Dart) mở rộng FR-9 (CRUD theo `entryType`, validate ngưỡng 4 ký tự PREFIX) + `PhoneNumberNormalizer` 2 phía (Dart/Kotlin) thêm tham số `entryType` | 1 | 2 | 3.5 |
| 7.1.2 | `BlocklistRepository` (Kotlin, native) mở rộng FR-9 (`entryType`, `isBlockedExact`/`isBlockedWithDetails`/`getAllPrefixValues`/`getPrefixVersion`, `addToBlocklist` thêm tham số `entryType` mặc định `EXACT`) | 1 | 1.5 | 2.5 |
| **Tổng 7.1** | | **2** | **3.5** | **6** |

### 7.2 Chặn real-time theo đầu số (prefix matching, hot-path)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.2.1 | `BlocklistPrefixIndex` (Kotlin, MỚI - trie trong bộ nhớ, version-counter cross-process) | 1.5 | 3 | 6 |
| 7.2.2 | `CallScreeningService.onScreenCall` mở rộng (khớp CẢ exact VÀ prefix, thứ tự `respondToCall` TRƯỚC `insertCallRecord`, ghi lịch sử nhánh chặn real-time MỚI) | 1.5 | 3 | 5.5 |
| **Tổng 7.2** | | **3** | **6** | **11.5** |

### 7.3 Lịch sử cuộc gọi mở rộng (logic hiển thị loại chặn)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.3.1 | `CallHistoryRepository`/`CallHistoryEntryDto` mở rộng (`matchedEntryType`/`matchedPrefixValue`) + `CallHistoryService.deriveDisplayStatus` (Dart, suy ra 1-trong-5 trạng thái) | 1 | 2 | 3.5 |
| **Tổng 7.3** | | **1** | **2** | **3.5** |

### 7.4 Export/Import blocklist

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.4.1 | `BlocklistExportService` (Dart, MỚI - đọc qua `BlocklistService.getAll()`, serialize JSON kèm `schema_version`, ghi file tạm cache dir, gọi Share Sheet) | 1 | 2 | 3.5 |
| 7.4.2 | `BlocklistImportService` (Dart, MỚI - file picker, validate+parse JSON/`schema_version`, validate bảo mật input, `mergeEntriesInTransaction`, map đủ 5 trạng thái `ImportResult`) | 2 | 4 | 7.5 |
| **Tổng 7.4** | | **3** | **6** | **11** |

### 7.5 UI blocklist mở rộng

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.5.1 | `BlocklistScreen` UI mở rộng (chọn loại entry EXACT/PREFIX, validate inline, dialog cảnh báo khớp N contact riêng cho PREFIX, nhãn/chip phân biệt loại entry + nguồn "Nhập từ file", nút Export/Import, dialog kết quả 1-trong-5 `ImportResult`) | 1.5 | 3 | 5 |
| **Tổng 7.5** | | **1.5** | **3** | **5** |

### 7.6 Schema migration mở rộng

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.6.1 | Schema migration 2 phía: `blocklist.entry_type` + đổi UNIQUE đơn thành composite `(normalized_number, entry_type)`, bảng MỚI `blocklist_meta` (singleton + seed), 2 TRIGGER SQL (`prefix_version` insert/delete), 1 PARTIAL INDEX, CHECK mới trên `blocklist`, 2 cột mới + CHECK mới trên `call_history` | 1.5 | 3 | 5.5 |
| **Tổng 7.6** | | **1.5** | **3** | **5.5** |

### 7.7 Kiểm thử FR-9/FR-10

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.7.1 | Unit test theo LLD "Gợi ý điểm cần test" v1.3 (`BlocklistPrefixIndex.matchLongestPrefix`/`ensureFresh`, `onScreenCall` thứ tự gọi + ưu tiên EXACT, `BlocklistImportService`/`ExportService` gồm bảo mật input, `deriveDisplayStatus` đủ 5 nhánh) | 1.5 | 3 | 5 |
| 7.7.2 | Integration test (UNIQUE composite thật, 2 trigger `prefix_version` thật, partial index qua `EXPLAIN QUERY PLAN`, `mergeEntriesInTransaction` ACID qua lỗi ghi có kiểm soát, end-to-end FR-1/FR-9, hồi quy đa phiên bản schema) | 2 | 4 | 7 |
| 7.7.3 | Performance sanity-check hiệu năng tra cứu prefix (seed 10.000 entry PREFIX, đo thời gian `onScreenCall` trong ngân sách ~1500ms) — BẮT BUỘC theo Exit Criteria | 0.5 | 1 | 2 |
| 7.7.4 | Vòng fix phát sinh nếu sanity-check hiệu năng FAIL hoặc cơ chế version-counter cross-process không hoạt động đúng như thiết kế | 0 | 1.5 | 4 |
| 7.7.5 | Vòng test thiết bị thật: mô phỏng crash/gián đoạn giữa quá trình Import (FR-10/AC10) — 3-5 lần thử, quan sát blocklist có "nửa vời" không | 0.5 | 1.5 | 3 |
| 7.7.6 | Regression bắt buộc do sửa hot-path/chữ ký method dùng chung (FR-1/AC1-AC7 + bộ test fail-open cũ, FR-3/FR-5/FR-8 do `addToBlocklist` thêm tham số `entryType` mặc định, FR-2/FR-8 do `insertCallRecord` thêm 2 field mới) | 1 | 2 | 4 |
| **Tổng 7.7** | | **5.5** | **13** | **25** |

### 7.8 UAT & tài liệu kỹ thuật riêng FR-9/FR-10

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 7.8.1 | UAT bổ sung riêng cho FR-9/FR-10 (cảm nhận thêm đầu số, cảnh báo khớp N contact, trải nghiệm Export/Import qua Share Sheet/file picker thật) | 0.5 | 1 | 2 |
| 7.8.2 | Cập nhật tài liệu kỹ thuật riêng cho Phase 1.7 (ghi chú vận hành bảng/trigger mới, đối chiếu docs FR-9/FR-10 vs code thật) | 0.25 | 0.5 | 1 |
| **Tổng 7.8** | | **0.75** | **1.5** | **3** |

**Tổng Phase 7 (7.1+7.2+7.3+7.4+7.5+7.6+7.7+7.8): Best 18.25 / Likely 38 / Worst 70.5** — khớp đúng tuyệt đối
với "Tổng Phase 1.7" trong `estimate.md` (18.25 / 38 / 70.5), không có sai số cộng dồn (khác Phase 4, giống
Phase 6). PERT-weighted theo `estimate.md` ≈ 40.1 person-day (không tính lại ở đây vì đó là phép tính tổng
hợp không phải effort cấp Task).

Phụ thuộc nội bộ Phase 7 (suy luận kỹ thuật của agent, **chưa được input xác nhận trực tiếp** — ngoại trừ
phụ thuộc cấp Phase đã nêu ở trên lấy trực tiếp từ HLD):
- 2.2.1/2.2.2 (`BlocklistService`/`PhoneNumberNormalizer` Dart gốc, Phase 2) và 3.1.3 (`PhoneNumberNormalizer`
  Kotlin gốc, Phase 3) phải xong trước 7.1.1 — 7.1.1 mở rộng trực tiếp cùng class/method, không viết lại từ
  đầu (gần trực tiếp từ input: `estimate.md` tự mô tả "thêm 1 tham số + 1 nhánh ngưỡng mới, không phải viết
  lại từ đầu").
- 3.1.2 (`BlocklistRepository` Kotlin gốc, Phase 3) phải xong trước 7.1.2 — cùng lý do mở rộng trực tiếp.
- 7.6.1 (schema migration: cột `entry_type`, bảng `blocklist_meta`, trigger) nên xong trước 7.1.2, 7.2.1,
  7.2.2 — các Task này đọc/viết `entry_type`/`blocklist_meta` nên cần schema đã tồn tại.
- 7.1.1/7.1.2 nên xong trước 7.2.1 (`BlocklistPrefixIndex` cần đọc entry theo `entryType` đã có) và trước
  7.2.2.
- 3.1.1 (`CallScreeningService` gốc, Phase 3) phải xong trước 7.2.2 — 7.2.2 sửa trực tiếp method
  `onScreenCall` đã có từ Phase 3.
- 7.1.1 (`BlocklistService`) nên xong trước 7.4.1/7.4.2 — `estimate.md` tự nêu `BlocklistExportService`/
  `BlocklistImportService` "tái sử dụng HOÀN TOÀN `BlocklistService`" (gần trực tiếp từ input).
- 2.1.1 (`BlocklistScreen` UI gốc, Phase 2) phải xong trước 7.5.1 — mở rộng trực tiếp cùng màn hình.
- 5.1.1/5.2.1 (`CallHistoryRepository`/`CallHistoryScreen` gốc, Phase 5) phải xong trước 7.3.1 — 7.3.1 mở rộng
  trực tiếp cùng class/DTO. **Lưu ý:** HLD ghi Phase 1.7 "KHÔNG phụ thuộc Phase 1.4/1.5/1.6 — độc lập về
  runtime"; phụ thuộc Task-level này KHÔNG mâu thuẫn với câu đó (câu đó nói về hành vi RUNTIME — cơ chế chặn
  theo prefix hoạt động được mà không cần `CallMonitorService`/`CallHistoryService` của các Phase đó), nhưng
  ở mức code artifact, Task 7.3.1 vẫn sửa trực tiếp class đã tạo ở Phase 5 — xem thêm mục "Bất thường phát
  hiện".
- 7.2.1 (`BlocklistPrefixIndex`) phải xong trước 7.7.3 (performance sanity-check cần index tồn tại để đo).
- 7.7.3 FAIL là điều kiện kích hoạt 7.7.4 — nguồn trực tiếp từ `estimate.md` (dòng việc tự mô tả "nếu
  sanity-check hiệu năng FAIL... best case = 0 vì có khả năng Pass ngay, không phát sinh gì thêm").
- 7.4.2 (`BlocklistImportService`) phải xong trước 7.7.5 (test thiết bị thật mô phỏng crash giữa import).
- 7.1.1, 7.1.2, 7.2.2 (sửa hot-path/chữ ký method dùng chung) phải xong trước 7.7.6 — nguồn trực tiếp từ
  `estimate.md` ("3 lý do bắt buộc chạy lại": `onScreenCall` bị sửa trực tiếp; `addToBlocklist` thêm tham số
  có default; `insertCallRecord` thêm field mới).
- 7.7.1 đến 7.7.6 nên xong trước 7.8.1 (UAT) — theo nguyên tắc chung test trước UAT; khác Phase 6,
  `estimate.md` KHÔNG yêu cầu UAT Phase 7 phải chờ một vòng test thiết bị thật đa OEM cụ thể (Phase 7 không
  có ma trận OEM).

---

## Phase 8 — Cross-cutting: System/E2E, Security, Compatibility, UAT, Tài liệu

Phụ thuộc: Input (`estimate.md`/HLD) không nêu rõ Phase 8 phải xong sau toàn bộ Phase 1-7; tuy nhiên các Task
trong Phase 8 (system/E2E test theo FR-1/2/5/6/7, UAT, vòng fix tương thích) về bản chất kỹ thuật cần các
chức năng tương ứng ở Phase 1-5 đã dựng xong để test — đây là suy luận kỹ thuật của agent, chưa được input
xác nhận trực tiếp theo từng Task cụ thể. `estimate.md` không có dòng việc tương ứng dạng "Cross-cutting cho
FR-8" hoặc "Cross-cutting cho FR-9/FR-10" tách riêng (các Task cross-cutting của FR-8 và FR-9/FR-10 — test
thiết bị thật, UAT, tài liệu — đã nằm trong Phase 6 và Phase 7 ở trên, không lặp lại tại đây).
Milestone/tiêu chí hoàn thành: Chưa xác định (Test Plan có "Exit Criteria" được `estimate.md` tham chiếu
nhưng không trích dẫn chi tiết tiêu chí vào bảng ước lượng — không tự suy diễn nội dung Exit Criteria vào đây).

### 8.1 Kiểm thử System/E2E & thiết bị thật

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 8.1.1 | Script System/E2E (adb emu gsm call/accept/cancel trên 2 API level, kịch bản đủ FR-1/2/5/6/7) | 2 | 4 | 7 |
| 8.1.2 | Vòng test trên thiết bị thật + SIM thật (FR-1/FR-2) + vòng fix phát sinh | 1 | 2 | 4 |
| **Tổng 8.1** | | **3** | **6** | **11** |

### 8.2 Bảo mật & tương thích

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 8.2.1 | Security cơ bản (loại trừ allowBackup/auto-backup, xóa dữ liệu khi gỡ app, sanity SQL injection) | 0.5 | 1 | 2 |
| 8.2.2 | Vòng fix tương thích API 29 vs 31+ (ví dụ khác biệt POST_NOTIFICATIONS runtime từ API 33) | 1 | 2 | 4 |
| **Tổng 8.2** | | **1.5** | **3** | **6** |

### 8.3 UAT

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 8.3.1 | UAT (điều phối + fix phản hồi mức blocker) | 1 | 2 | 4 |
| **Tổng 8.3** | | **1** | **2** | **4** |

### 8.4 Tài liệu kỹ thuật

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 8.4.1 | Cập nhật tài liệu kỹ thuật (README dev setup, ghi chú vận hành DDL 2 phía, đối chiếu docs vs code thật) | 0.5 | 1 | 2 |
| **Tổng 8.4** | | **0.5** | **1** | **2** |

**Tổng Phase 8 (8.1+8.2+8.3+8.4): Best 6 / Likely 12 / Worst 23.**
Lưu ý cách gộp: `estimate.md` tách dòng "Cập nhật tài liệu kỹ thuật" (8.4.1) RA NGOÀI dòng "Tổng Cross-cutting"
(5.5 / 11 / 21, chỉ gồm 8.1-8.3). WBS này gộp cả 8.4 vào chung Phase 8 vì cùng tính chất cross-cutting —
không phải khác biệt số liệu, chỉ là cách trình bày khác với bảng gốc. Tổng Phase 8 (có 8.4) = Tổng
Cross-cutting gốc + dòng Docs gốc = (5.5+0.5) / (11+1) / (21+2) = 6 / 12 / 23 — khớp đúng. (Phase này lần lượt
được đánh số "Phase 6" trước bản cập nhật 2026-10-21 lần 1, rồi "Phase 7" sau bản cập nhật lần 1, và nay là
"Phase 8" sau bản cập nhật lần 2 — xem ghi chú đổi số ở mục "Nguồn".)

---

## Tổng hợp toàn Phase 1

| Phase | Best | Likely | Worst |
|---|---|---|---|
| 1 — Nền tảng | 8 | 13.5 | 23 |
| 2 — Blocklist thủ công | 3.5 | 6.5 | 10.5 |
| 3 — Chặn real-time | 3.5 | 7 | 12 |
| 4 — Phát hiện + prompt | 13.5 | 23.5 | 40 |
| 5 — Lịch sử cuộc gọi | 3 | 5.5 | 8.5 |
| 6 — Xác nhận lừa đảo lúc đổ chuông + ngắt cuộc gọi (FR-8, mới) | 18.75 | 37.5 | 72 |
| 7 — Chặn theo đầu số + Export/Import blocklist (FR-9/FR-10, mới) | 18.25 | 38 | 70.5 |
| 8 — Cross-cutting | 6 | 12 | 23 |
| **TỔNG** | **74.5** | **143.5** | **259.5** |

So với `estimate.md` ("TỔNG CỘNG TOÀN BỘ PHASE 1 (bao gồm Phase 1.6 và Phase 1.7)": Best 75 / Likely 143.5 /
Worst 260): Likely khớp đúng (143.5). Best lệch 0.5 (74.5 so với 75) và Worst lệch 0.5 (259.5 so với 260) —
**cùng nguồn gốc với sai số 0.5/0.5 đã nêu ở Phase 4 trước khi thêm Phase 6/Phase 7** (xem mục Bất thường):
cả Phase 6 (FR-8) và Phase 7 (FR-9/FR-10) tự thân đều khớp đúng tuyệt đối với dòng tổng tương ứng của
`estimate.md` (18.75/37.5/72 và 18.25/38/70.5), không phát sinh sai số mới — độ lệch 0.5/0.5 ở dòng TỔNG này
hoàn toàn là độ lệch cộng dồn cũ từ Phase 4, không phải do Phase 7 hoặc việc đổi số Phase 8 gây ra.
PERT-weighted ≈ 151.5 person-day (theo `estimate.md`, không tính lại ở đây vì đó là một phép tính tổng hợp
không phải effort cấp Task; trước khi có Phase 6, PERT-weighted cho Phase 1-5+8 ≈ 71 person-day; trước khi
có Phase 7, PERT-weighted cho Phase 1-6+8 ≈ 111.4 person-day — cả hai theo `estimate.md`).

---

## Giả định & ghi chú

- WBS này không bao gồm gán người thực hiện hay deadline/ngày cụ thể cho bất kỳ Task nào — việc lập lịch
  thực (ai làm, xong trước ngày nào) thuộc PM/`release-manager`.
- Effort mỗi Task là nguyên giá trị từ `estimate.md`, không tự ước lượng lại. Effort NÀY đã bao gồm: code
  review nội bộ thông thường, unit/integration/system/security/UAT test theo Test Plan, fix bug phát sinh
  cùng phase (nguồn: `estimate.md` mục "Giả định đi kèm"). Effort KHÔNG bao gồm: effort của các agent đã tạo
  tài liệu input (requirements-analyst/system-architect/low-level-designer/ui-ux-designer/test-plan-writer/
  db-schema-reviewer — kể cả vòng cascade FR-8 vừa thực hiện); chi phí QA thủ công lặp lại mỗi lần regression
  sau khi Phase 1 hoàn thành; chuẩn bị Play Store listing/release process; **riêng Phase 6 (FR-8)**: công sức
  review `exported`/intent-filter của `security-auditor` cho `RingingScamOverlay`/`ScamConfirmationReceiver`/
  `CallMonitorService` (LLD v1.1 tự ghi "NGOÀI PHẠM VI LLD", thuộc agent chuyên trách khác); **riêng Phase 7
  (FR-9/FR-10)**: lựa chọn plugin thật cho Share Sheet/file picker và review quyền của plugin đó (LLD v1.3 tự
  ghi "NGOÀI PHẠM VI LLD", cần `security-auditor` khi có code thật); effort UI cập nhật `CallHistoryScreen`
  cho 2 badge mới `BLOCKED_REALTIME_EXACT`/`BLOCKED_REALTIME_PREFIX` (UIUX v1.3 tự ghi nhận CHƯA cascade,
  ngoài phạm vi Task 7.3.1 — xem mục Ngoài phạm vi) (nguồn: `estimate.md`).
- Không có cấp Subtask trong tài liệu này vì `estimate.md` không phân rã Task nào xuống mức chi tiết hơn,
  kể cả 14 dòng việc của Phase 1.6 (FR-8) và 17 dòng việc của Phase 1.7 (FR-9/FR-10) mới bổ sung.
- Deliverable (1.1, 1.2, 2.1, 2.2..., 6.1-6.6..., 7.1-7.8...) là nhóm do agent này tạo để tổ chức các Task đã
  có theo thành phần kỹ thuật cùng nhóm (ví dụ "UI", "Service/Logic", "Kiểm thử") — không phải hạng mục công
  việc mới, chỉ là cách gom nhóm trình bày. Với Phase 6, 14 dòng việc của `estimate.md` được gom thành 6
  Deliverable (6.1 Spike/PoC & overlay, 6.2 CallMonitorService & prompt atomicity, 6.3 Quyền & Onboarding, 6.4
  Lịch sử cuộc gọi & schema migration, 6.5 Kiểm thử, 6.6 UAT & tài liệu). Với Phase 7, 17 dòng việc của
  `estimate.md` được gom thành 8 Deliverable (7.1 Blocklist CRUD & normalizer mở rộng, 7.2 Chặn real-time theo
  đầu số, 7.3 Lịch sử cuộc gọi mở rộng, 7.4 Export/Import blocklist, 7.5 UI blocklist mở rộng, 7.6 Schema
  migration mở rộng, 7.7 Kiểm thử, 7.8 UAT & tài liệu) — cả hai theo cùng nguyên tắc nhóm đã áp dụng cho
  Phase 1-5.
- Mọi phụ thuộc không trích trực tiếp từ HLD "Phase breakdown" hoặc Căn cứ của `estimate.md` đều được gắn
  nhãn rõ "suy luận kỹ thuật của agent, chưa được input xác nhận trực tiếp" — cần người dùng/system-architect
  xác nhận lại trước khi dùng để lập lịch thực.
- Rủi ro đã nêu trong `estimate.md` ảnh hưởng trực tiếp đến worst-case của Task cụ thể, không tự thêm vào
  effort:
  - Task 4.1.1 (CallMonitorService): LLD chưa chốt cơ chế dual-implementation API 29 vs 31+ cho
    TelephonyCallback — khuyến nghị dùng `low-level-designer` chốt trước khi bắt đầu Task này để thu hẹp
    khoảng Best-Worst. Rủi ro này lan sang Task 6.2.1 vì cùng class `CallMonitorService` được mở rộng.
  - Task 2.2.2 và 3.1.3 (PhoneNumberNormalizer Dart/Kotlin): rủi ro lệch output giữa 2 cài đặt độc lập, chỉ
    phát hiện ở Integration test 2.3.2/3.2.2.
  - Task 3.2.2 và 4.4.2 (Integration test WAL/race): hành vi thật của driver sqflite với PRAGMA
    WAL/busy_timeout chưa được xác nhận — có thể đẩy worst-case cao hơn nếu phát hiện lệch giả định thiết kế;
    rủi ro này cũng ảnh hưởng Task 6.4.2 (schema migration) vì chạy qua cùng driver.
  - **Task 6.1.1/6.1.2 (spike/PoC + `RingingScamOverlay`)**: LLD v1.1 "Rủi ro 3" CHƯA CHỐT tuyệt đối cơ chế
    hiển thị (Phương án A `SYSTEM_ALERT_WINDOW` vs B full-screen-intent Notification) — nếu phải đổi phương án
    SAU KHI đã code một phần theo phương án ban đầu, `estimate.md` tự nêu rủi ro này CHƯA được buffer đầy đủ
    vào worst-case hiện tại (worst-case giả định đổi phương án xảy ra ở giai đoạn spike, trước khi code chính
    thức) — khuyến nghị chốt cứng phương án qua `system-architect`/`low-level-designer` trước khi bắt đầu
    6.1.2 nếu muốn loại bỏ rủi ro này.
  - **Task 6.2.1, 6.5.3, 6.5.4 (endCall() và test đa OEM)**: hành vi `TelecomManager.endCall()` phụ thuộc
    telecom stack tùy biến theo OEM, CHƯA kiểm chứng thực nghiệm trên bất kỳ stack nào tại thời điểm ước
    lượng — đây là lý do chính khiến tỷ lệ worst/best của Phase 6 (72/18.75 ≈ 3.8x) cao hơn tỷ lệ trung bình
    của Phase 1-5 (≈ 3.1x), theo đánh giá của chính `estimate.md`.
  - **Task 6.4.2 (schema migration)**: đổi `pending_prompts.call_record_id` từ NOT NULL sang NULLABLE + thêm
    5 CHECK constraint mới là migration SQLite đầu tiên có thay đổi cấu trúc/ràng buộc của dự án (không chỉ
    ADD COLUMN đơn giản như Phase 1.1), chưa có tiền lệ trong chính dự án để đối chiếu tốc độ.
  - **Task 6.5.2/6.5.3 (race (a)/(b))**: `estimate.md`/Test Plan tự nhận cửa sổ race này "không thể kiểm soát
    chính xác bằng test double hay bấm tay" — ADR-005 chấp nhận có chủ đích rủi ro tồn đọng này, không kỳ
    vọng effort loại bỏ hoàn toàn, chỉ đảm bảo không crash/không ngắt nhầm số khác.
  - **Task 7.2.1 (`BlocklistPrefixIndex`)**: cơ chế version-counter cross-process phụ thuộc giả định CHƯA
    được `system-architect` xác nhận chính thức ("`CallScreeningService` chạy process riêng hay chung Flutter
    engine") — nếu cấu hình thật khác giả định, cơ chế `ensureFresh()` cần điều chỉnh, đẩy worst-case cao hơn.
  - **Task 7.4.2 (`BlocklistImportService`)**: quyết định luôn gán `source=IMPORTED`, không giữ `source` gốc
    từ file, CHƯA xác nhận với stakeholder (LLD v1.3 mục Giả định) — nếu đổi sau khi code, cần effort sửa
    thêm ngoài ước lượng hiện tại.
  - **Task 7.6.1 (schema migration Phase 1.7)**: lần đầu dự án có TRIGGER SQL + PARTIAL INDEX + bảng singleton
    (`blocklist_meta`) — không có tiền lệ trong chính dự án để đối chiếu tốc độ viết đúng ngay lần đầu, mức độ
    MỚI còn cao hơn migration NOT NULL→NULLABLE của Task 6.4.2 (trigger/partial index chưa dùng ở Phase 1-6).
  - Rủi ro hành vi thật của driver sqflite/SQLiteOpenHelper với PRAGMA WAL/busy_timeout (đã nêu ở Task 3.2.2/
    4.4.2/6.4.2) cũng lan sang Task 7.6.1/7.7.2 vì chạy qua cùng driver; Phase 7 còn thêm rủi ro write-write
    contention mới khi `mergeEntriesInTransaction` (Task 7.4.2) giữ write-lock lâu hơn mọi ghi đơn dòng trước
    đây (ERD v1.2 tự đánh giá "thấp về xác suất" nhưng "có thực về lý thuyết").
  - Rủi ro lệch output giữa 2 cài đặt `PhoneNumberNormalizer` độc lập (đã nêu ở Task 2.2.2/3.1.3) lan sang
    Task 7.1.1 vì nay còn phải nhất quán cả ngưỡng PREFIX (4 ký tự), không chỉ định dạng EXACT như trước.

## Bất thường phát hiện

- Phase 4: cộng trực tiếp 10 dòng Task gốc của Phase 1.4 trong `estimate.md` cho ra Best = 13.5 và
  Worst = 40 (Likely = 23.5, khớp). Tuy nhiên dòng "Tổng Phase 1.4" mà `estimate.md` tự ghi là
  **14 / 23.5 / 40.5** — cột Best và Worst của chính dòng tổng này lệch 0.5 so với tổng cộng dồn từ các dòng
  Task mà nó tổng hợp. WBS này giữ nguyên effort từng Task như gốc và trình bày tổng Phase 4 theo đúng tổng
  cộng dồn thực tế (13.5 / 23.5 / 40) — KHÔNG dùng số 14/40.5 đã ghi sẵn ở dòng tổng của `estimate.md` vì số
  đó không khớp với các dòng Task mà nó tự liệt kê. Sai số 0.5 này lan ra cả dòng "TỔNG CỘNG TOÀN BỘ PHASE 1"
  của `estimate.md` (38 / 68 / 117.5): cộng lại từ tất cả subtotal Phase chính xác cho 37.5 / 68 / 117.
  Không tự sửa lại `estimate.md` — chỉ nêu rõ bất thường này để người dùng/`project-estimator` đối chiếu lại.
- `estimate.md` tách dòng "Cập nhật tài liệu kỹ thuật" ra ngoài subtotal "Tổng Cross-cutting" (chỉ gồm 4 dòng
  đầu), trong khi WBS này gộp chung vào Phase 8 (Deliverable 8.4) vì cùng bản chất cross-cutting — không phải
  sai số, chỉ là khác cách trình bày; đã ghi rõ ở mục Phase 8.
- Rủi ro thiết kế chưa chốt: HLD tự xác nhận cơ chế dual-implementation API 29 vs 31+ cho TelephonyCallback
  (ảnh hưởng Task 4.1.1, lan sang Task 6.2.1) và cơ chế background reliability cho NFR-4 chưa được
  `low-level-designer` quyết định — đây là khoảng trống thiết kế đã biết, không phải lỗi của bảng ước lượng,
  nhưng ảnh hưởng trực tiếp đến độ tin cậy của effort Task 4.1.1/6.2.1.
- **Phase 6 (FR-8) không có sai số cộng dồn**: 14 dòng Task cộng trực tiếp cho đúng Best 18.75 / Likely 37.5
  / Worst 72, khớp tuyệt đối với dòng "Tổng Phase 1.6" mà `estimate.md` tự ghi — khác với Phase 4 (xem bên
  trên), ở đây không phát hiện bất thường số liệu.
- **Phase 7 (FR-9/FR-10) cũng không có sai số cộng dồn**: 17 dòng Task cộng trực tiếp cho đúng Best 18.25 /
  Likely 38 / Worst 70.5, khớp tuyệt đối với dòng "Tổng Phase 1.7" mà `estimate.md` tự ghi — giống Phase 6,
  khác Phase 4. Tuy nhiên vì Phase 8 (Cross-cutting, chịu ảnh hưởng gián tiếp từ sai số 0.5/0.5 đã có từ
  trước ở Phase 4) vẫn nằm trong dòng TỔNG toàn Phase 1 cùng Phase 6/Phase 7, dòng TỔNG cuối cùng
  (74.5 / 143.5 / 259.5) vẫn lệch 0.5/0.5 so với dòng "TỔNG CỘNG TOÀN BỘ PHASE 1 (bao gồm Phase 1.6 và Phase
  1.7)" của `estimate.md` (75 / 143.5 / 260) — lệch này 100% là do mang theo sai số cũ của Phase 4, không
  phải do Phase 7/Phase 8 hay việc đổi số Phase 8 gây ra.
- HLD v1.2 ghi Phase 1.7 "KHÔNG phụ thuộc Phase 1.4/1.5/1.6 — độc lập về runtime". Tuy nhiên Task 7.3.1
  (`CallHistoryRepository`/`CallHistoryEntryDto` mở rộng) sửa trực tiếp class đã tạo ở Phase 5 (Task 5.1.1/
  5.2.1) — đây KHÔNG phải mâu thuẫn số liệu, chỉ là khác bình diện (HLD nói về hành vi RUNTIME của cơ chế
  chặn theo prefix, không nói về việc chỉnh sửa code artifact của Task cross-phase); phụ thuộc Task-level này
  đã được ghi rõ là suy luận của agent trong mục phụ thuộc nội bộ Phase 7, cần người dùng/`system-architect`
  xác nhận lại nếu cần chính xác tuyệt đối khi lập lịch thực.
- Phát hiện ngoài WBS (ghi nhận lại theo `estimate.md`, không phải phát hiện mới của agent này): `estimate.md`
  tự ghi rằng trong quá trình cascade thiết kế FR-8, `ui-ux-designer` đã phát hiện 1 lỗ hổng thiết kế thật
  (buffer ban đầu của `PendingPromptRegistry` chỉ xử lý nhánh YES, bỏ sót nhánh NO gây mất dữ liệu) — đã được
  xử lý trong LLD v1.1 và phản ánh vào effort Task 6.2.2 ở trên, không cần hành động thêm từ WBS này.

## Ngoài phạm vi

- Gán người thực hiện và lập lịch ngày cụ thể cho từng Task/Phase — thuộc PM/`release-manager`.
- Chốt cơ chế dual-implementation API 29 vs 31+ cho TelephonyCallback (ảnh hưởng Task 4.1.1, 6.2.1) và cơ chế
  cụ thể cho NFR-4 — khuyến nghị dùng `low-level-designer` trước khi bắt đầu Phase 4/Phase 6.
- Nếu cần cập nhật lại effort sau khi hoàn thành Phase 1 (có dữ liệu lịch sử thật của chính team/repo), dùng
  `project-estimator` để hiệu chỉnh lại — `estimate.md` tự khuyến nghị điều này ở mục "Lưu ý quan trọng".
- **(Phase 6/FR-8)** Chốt cứng cơ chế hiển thị `RingingScamOverlay` (Phương án A vs B, LLD "Rủi ro 3") trước
  khi bắt đầu Task 6.1.2, nếu muốn tránh rủi ro phải code lại sau spike — khuyến nghị dùng
  `system-architect`/`low-level-designer`.
- **(Phase 6/FR-8)** Review cấu hình `exported`/intent-filter thật cho `RingingScamOverlay`/
  `ScamConfirmationReceiver`/`CallMonitorService` — LLD v1.1 tự ghi "NGOÀI PHẠM VI LLD", thuộc
  `security-auditor`, không nằm trong effort Task 6.x ở trên.
- **(Phase 6/FR-8)** Làm rõ ngưỡng số liệu/định lượng cho NFR-4 mở rộng (độ tin cậy hiển thị overlay đúng
  lúc) — Test Plan v1.1 tự nêu hiện chỉ test định tính; nếu cần tiêu chí pass/fail định lượng, dùng
  `requirements-analyst`.
- **(Phase 7/FR-9/FR-10)** Xác nhận cấu hình `android:process` thật cho `CallScreeningService` (ảnh hưởng cơ
  chế cross-process của `BlocklistPrefixIndex`, Task 7.2.1) — LLD v1.3 tự ghi cần `system-architect` xác nhận
  trước khi viết manifest thật; khuyến nghị xác nhận trước khi bắt đầu Task 7.2.1.
- **(Phase 7/FR-9/FR-10)** Xác nhận với stakeholder quyết định `BlocklistImportService` luôn gán
  `source=IMPORTED`, không giữ `source` gốc từ file (Task 7.4.2) — LLD v1.3 tự ghi "CHƯA xác nhận với
  stakeholder"; khuyến nghị dùng `requirements-analyst` trước khi code để tránh rework sau khi đã code xong.
- **(Phase 7/FR-9/FR-10)** Cập nhật `CallHistoryScreen` cho 2 badge mới (`BLOCKED_REALTIME_EXACT`/
  `BLOCKED_REALTIME_PREFIX`) — UIUX v1.3 tự ghi nhận CHƯA cascade, ngoài phạm vi Task 7.3.1 (Task này CHỈ gồm
  phần logic `deriveDisplayStatus`, không gồm UI). Khuyến nghị dùng `ui-ux-designer` cập nhật trước, sau đó
  chạy lại `project-estimator` để bổ sung effort UI riêng cho phần này.
- **(Phase 7/FR-9/FR-10)** Lựa chọn plugin thật cho Share Sheet/file picker (dùng ở Task 7.4.1/7.4.2) và
  review quyền của plugin đó — LLD v1.3 tự ghi "NGOÀI PHẠM VI LLD", thuộc `security-auditor`, không nằm
  trong effort Task 7.4.x ở trên.
