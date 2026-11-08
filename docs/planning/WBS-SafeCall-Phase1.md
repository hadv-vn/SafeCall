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
- *Cập nhật (2026-11-07):* thêm **Phase 9** (FR-11/CONTACTS-PRIORITY, ánh xạ từ "Phase 1.9" mới trong
  `estimate.md`), **Phase 10** (FR-12/BLOCK-TOGGLE, ánh xạ từ "Phase 1.10") và **Phase 11**
  (FR-13/PERMISSION-CHECK, ánh xạ từ "Phase 1.11") — bổ sung cùng ngày trong `estimate.md`, theo cascade
  SRS v6.1/HLD v1.3/LLD v1.4/ERD v1.3/UIUX v1.7/TestPlan v1.3. Phase 1-8 cũ **giữ nguyên không chỉnh sửa về nội
  dung/effort** (Phase 8 ở đây là Phase Cross-cutting hiện có, đã chứa Deliverable 8.5 cho 3 task retroactive
  2026-10-25 — KHÔNG phải một "Phase 1.8" riêng biệt nào khác). Phase "Cross-cutting" của tài liệu này (đang
  đánh số Phase 8, sau 2 lần đổi số trước đó từ Phase 6 → Phase 7 → Phase 8) được **đổi số thành Phase 12** để
  3 Phase mới (9/10/11) chèn đúng vị trí theo thứ tự `estimate.md` (Phase 1.9/1.10/1.11 trong `estimate.md`
  nằm giữa mục "Phase 1.8 — Bổ sung retroactive" và mục "Cross-cutting"; trong WBS này, "Phase 1.8 retroactive"
  tương ứng Deliverable 8.5/nay 12.5, không phải một Phase số riêng, nên 3 Phase mới chèn ngay trước Phase
  Cross-cutting, tức trước Phase 8 cũ/Phase 12 mới) — không có Task/Deliverable nào khác bị đổi số ngoài việc
  đổi nhãn Phase 8 → Phase 12 này.
  Lưu ý: HLD v1.3 gọi 3 phase này là "Phase 1.8/1.9/1.10" (tự ghi nhận trùng số với "Phase 1.8" retroactive đã
  có ở `estimate.md`/WBS, giao việc đổi số cho bước cascade estimate/WBS — xem HLD mục "Phase breakdown (bổ
  sung)" và "Lưu ý đánh số phase"); `estimate.md` đã đặt lại thành "Phase 1.9/1.10/1.11", và WBS này ánh xạ
  tiếp 1-1 thành **Phase 9/10/11**.
- Tài liệu này CHỈ cấu trúc hóa lại dữ liệu đã có ở trên theo cây Phase → Deliverable → Task.
  Effort của mỗi Task là giá trị **likely-case nguyên bản** lấy từ `estimate.md`; **Deliverable là nhóm do
  agent này tạo ra để tổ chức các Task đã có theo thành phần kỹ thuật cùng nhóm** (không phải Task/Deliverable
  mới ngoài input — chỉ là cách gom nhóm các dòng đã có trong bảng ước lượng).
- Không có cấp Subtask: input (`estimate.md`) không phân rã Task nào xuống mức chi tiết hơn, nên không tự
  tách Subtask (kể cả 17 dòng việc của Phase 1.7 và 35 dòng việc của Phase 1.9/1.10/1.11, FR-9/FR-10/FR-11/
  FR-12/FR-13 mới bổ sung).

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
| 6.1.1 | Spike/PoC chọn phương án hiển thị `RingingScamOverlay` (A - window overlay `SYSTEM_ALERT_WINDOW` vs B - full-screen-intent Notification), kiểm chứng độ tin cậy "đè đúng lúc" thực nghiệm trên 1-2 thiết bị trước khi code chính thức | ~~1~~ **0.5** | ~~2~~ **1** | ~~4~~ **2** |
| 6.1.2 | `RingingScamOverlay` (Kotlin, MỚI - `show`/`onYesPressed`/`onNoPressed`/`dismiss`, tự đóng khi đổi trạng thái AC7, hiện lại nếu bị bỏ qua AC9, vòng đời độc lập Activity/Flutter engine) | 2 | 3.5 | 7 |
| **Tổng 6.1** | | ~~**3**~~ **2.5** | ~~**5.5**~~ **4.5** | ~~**11**~~ **9** |

> **✍️ PM-edit (2026-11-06):** Task 6.1.1 hạ từ 1/2/4 xuống 0.5/1/2 — lý do và chi tiết ở `estimate.md` (ADR-007 đã chốt cứng Phương án A từ 2026-10-21, spike nay chỉ còn xác nhận triển khai, không còn ra quyết định kiến trúc).

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

**Tổng Phase 6 (6.1+6.2+6.3+6.4+6.5+6.6): Best ~~18.75~~ 18.25 / Likely ~~37.5~~ 36.5 / Worst ~~72~~ 70** —
khớp với "Tổng Phase 1.6" trong `estimate.md` sau PM-edit 2026-11-06 (18.25 / 36.5 / 70). PERT-weighted theo
`estimate.md` ≈ ~~40.1~~ **39.0 person-day** (không tính lại ở đây vì đó là phép tính tổng hợp không phải effort cấp Task).

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

## Phase 9 — Cơ chế ưu tiên Danh bạ (FR-11/CONTACTS-PRIORITY, mới)

Phụ thuộc (nguồn: HLD v1.3 "Phase breakdown (bổ sung)", mục "Phase 1.8" — HLD tự ghi nhận số "Phase 1.8" này
trùng với "Phase 1.8" retroactive đã có ở `estimate.md`/WBS, không liên quan FR-11/12/13, nên ở `estimate.md`
đã đặt lại thành "Phase 1.9" và ở WBS này ánh xạ tiếp thành Phase 9 — xem mục Nguồn): "Phụ thuộc: Phase 1.7
phải hoàn thành trước (prefix matching FR-9 đã có — CONTACTS-PRIORITY là ngoại lệ trực tiếp của nó).
`ContactsRepository`/`ContactsLookupModule` đã có sẵn từ cascade FR-5/FR-9, chỉ cần thêm người gọi mới." →
ánh xạ sang số Phase của WBS này: HLD "Phase 1.7" = **Phase 7** của WBS này (FR-9/BLOCK-03, chặn theo đầu số).
Phase 9 phụ thuộc **Phase 7** (phải hoàn thành trước — không làm trước Phase 9). Đây là phụ thuộc lấy trực
tiếp từ HLD, không phải suy luận của agent.
Milestone/tiêu chí hoàn thành: Chưa xác định (TestPlan v1.3 có Exit Criteria riêng cho FR-11/FR-12/FR-13 được
`estimate.md` tham chiếu nhưng không trích dẫn chi tiết tiêu chí vào bảng ước lượng — không tự suy diễn nội
dung Exit Criteria vào đây).

### 9.1 Settings UI & Service + đọc cờ CONTACTS-PRIORITY (Dart + Kotlin)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 9.1.1 | `SettingsScreen` (Dart, MỚI — 1 toggle CONTACTS-PRIORITY, mặc định BẬT, không dialog xác nhận khi đổi theo UIUX v1.7 Giả định 23) | 0.5 | 1 | 2 |
| 9.1.2 | `SettingsService` (Dart, MỚI — `getFlags()`/`setFlag(flag, value)` CRUD đơn giản qua `sqflite` vào `blocklist_meta`) | 0.25 | 0.5 | 1 |
| 9.1.3 | `BlocklistRepository.getSettingsFlags()` (Kotlin, MỚI) + data class `SettingsFlags` (đọc gộp cả 4 cờ cùng lúc trong 1 round-trip — 1 cờ dùng ngay ở Phase 9, 3 cờ còn lại viết sẵn, dùng ở Phase 10) | 0.5 | 1 | 1.5 |
| **Tổng 9.1** | | **1.25** | **2.5** | **4.5** |

### 9.2 CallScreeningService & CallMonitorService mở rộng (lõi FR-11)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 9.2.1 | `CallScreeningService` — tái cấu trúc hot-path `screenSafely` thành pipeline nhiều bước (giữ đúng bước EXACT/PREFIX hiện có, thêm bước CONTACTS-PRIORITY: gọi `ContactsRepository.isNumberInContacts` in-process lần đầu từ class này, enum MỚI `ScreeningDecisionReason`, field `decisionReason` mới trên `ScreeningResult`) | 2 | 3.5 | 6 |
| 9.2.2 | `RingingCallNumberHolder` (mở rộng field `decisionReason`) + `CallMonitorService.handleCallEnded()` (mang `decisionReason` từ nhánh CONTACTS_PRIORITY-allow tới `call_history`, đọc TRƯỚC `clear()`) | 0.5 | 1 | 2 |
| **Tổng 9.2** | | **2.5** | **4.5** | **8** |

### 9.3 Lịch sử cuộc gọi & schema migration mở rộng

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 9.3.1 | `CallHistoryRepository`/`CallRecordDraft` — thêm field `decisionReason` (tiếp nối `matchedEntryType`/`matchedPrefixValue`/`wasBlocked` đã có) | 0.25 | 0.5 | 1 |
| 9.3.2 | Migration schema 2 phía (`SafeCallSchema.kt` + `safecall_schema.dart`): `blocklist_meta` +1 cột (`contacts_priority_enabled INTEGER NOT NULL DEFAULT 1`) + `call_history` +1 cột (`decision_reason TEXT`) kèm ĐỦ BỘ CHECK constraint cho TOÀN BỘ enum 4 giá trị (viết 1 lần, dùng ngay ở Phase 10 cho 3 giá trị `BLOCK_TOGGLE_*`, không cần sửa lại) | 1 | 2 | 3.5 |
| **Tổng 9.3** | | **1.25** | **2.5** | **4.5** |

### 9.4 Kiểm thử FR-11

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 9.4.1 | Unit test (`screenSafely` bước EXACT/PREFIX/CONTACTS-PRIORITY × ON/OFF × is-contact true/false; round-trip FR-11: thêm contact → gọi số thuộc 1 PREFIX đã chặn khớp đúng contact → xác nhận allow + `decisionReason=CONTACTS_PRIORITY` ở `call_history` SAU KHI cuộc gọi kết thúc) | 1 | 2 | 3.5 |
| 9.4.2 | Integration test (CHECK constraint mới trên `call_history` thật — đặc biệt ràng buộc loại trừ `decision_reason`/`matched_entry_type` — và 1 cột mới trên `blocklist_meta` thật) | 1 | 2 | 3.5 |
| 9.4.3 | **Regression chung cả 3 phase (9/10/11)**: viết test xác nhận `CallRecordDraft.wasBlocked` là field tường minh (ĐỌC CODE THẬT xác nhận lỗi suy diễn mà LLD v1.4 mô tả "phát hiện và sửa" ĐÃ ĐƯỢC FIX SẴN trong code hiện có — KHÔNG cần sửa code, CHỈ cần viết test regression + chạy lại bộ test cũ FR-1..FR-10 liên quan `call_history`) | 0.5 | 1 | 2 |
| **Tổng 9.4** | | **2.5** | **5** | **9** |

### 9.5 UAT & tài liệu kỹ thuật riêng FR-11

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 9.5.1 | UAT nhỏ riêng CONTACTS-PRIORITY (cảm nhận bật/tắt, round-trip với contact thật) | 0.25 | 0.5 | 1 |
| 9.5.2 | Cập nhật tài liệu kỹ thuật riêng cho Phase 1.9 | 0.25 | 0.5 | 1 |
| **Tổng 9.5** | | **0.5** | **1** | **2** |

**Tổng Phase 9 (9.1+9.2+9.3+9.4+9.5): Best 8 / Likely 15.5 / Worst 28** — khớp đúng tuyệt đối với "Tổng Phase
1.9" trong `estimate.md` (8 / 15.5 / 28), không có sai số cộng dồn (giống Phase 6/Phase 7, khác Phase 4).
PERT-weighted theo `estimate.md` ≈ 16.3 person-day (không tính lại ở đây vì đó là phép tính tổng hợp không
phải effort cấp Task).

Phụ thuộc nội bộ Phase 9 (suy luận kỹ thuật của agent, **chưa được input xác nhận trực tiếp** — ngoại trừ
phụ thuộc cấp Phase đã nêu ở trên lấy trực tiếp từ HLD):
- 7.6.1 (schema migration Phase 7, tạo bảng `blocklist_meta`) phải xong trước 9.1.3 và 9.3.2 — cả hai Task này
  đọc/viết cột trên bảng `blocklist_meta` đã được tạo ở Phase 7.
- 7.2.2 (`CallScreeningService.onScreenCall` mở rộng EXACT/PREFIX, Phase 7) phải xong trước 9.2.1 —
  `estimate.md` tự mô tả 9.2.1 "giữ đúng bước EXACT/PREFIX hiện có" trước khi thêm bước CONTACTS-PRIORITY.
- 5.1.1 (`CallHistoryRepository` gốc, Phase 5) và 7.3.1 (mở rộng `matchedEntryType`/`matchedPrefixValue`,
  Phase 7) phải xong trước 9.3.1 — `estimate.md` tự mô tả 9.3.1 "tiếp nối" các field đã có.
- `ContactsRepository`/`ContactsLookupModule` (đã có sẵn từ cascade FR-5/FR-9 theo mô tả của `estimate.md`,
  không gắn với một Task riêng cụ thể trong Phase 2/Phase 7 của WBS này) là tiền đề kỹ thuật cho 9.2.1 — ghi
  nhận như bối cảnh, không ép vào một mã WBS cụ thể vì input không tách dòng việc riêng cho chính
  `ContactsRepository`.
- 9.1.1, 9.1.2, 9.1.3 nên xong trước 9.2.1 (pipeline đọc cờ `contacts_priority_enabled` qua `SettingsFlags`).
- 9.2.1, 9.2.2 nên xong trước 9.3.1 (ghi `decisionReason` vào lịch sử cần nguồn giá trị từ pipeline/holder).
- 9.1-9.3 nên xong trước 9.4 (test cần code tồn tại); 9.4 nên xong trước 9.5 (UAT sau test, theo nguyên tắc
  chung — khác Phase 6, `estimate.md` không yêu cầu UAT Phase 9 chờ một vòng test thiết bị thật đa OEM cụ thể).

---

## Phase 10 — Bật/Tắt chế độ chặn nâng cao (FR-12/BLOCK-TOGGLE, mới, phụ thuộc Phase 9)

Phụ thuộc (nguồn: HLD v1.3 "Phase breakdown (bổ sung)", mục "Phase 1.9" — ánh xạ: HLD "Phase 1.8" = Phase 9
của WBS này, HLD "Phase 1.9" = Phase 10 của WBS này, HLD "Phase 1.10" = Phase 11 của WBS này): "Phụ thuộc:
Phase 1.8 phải hoàn thành trước — tái dùng CÙNG màn hình Settings và CÙNG biến `isContact` đã tính ở bước 4.
Không phụ thuộc Phase 1.10." → Phase 10 phụ thuộc **Phase 9** (phải hoàn thành trước — không làm trước Phase
10); KHÔNG phụ thuộc **Phase 11**. Đây là phụ thuộc lấy trực tiếp từ HLD, không phải suy luận của agent.
Milestone/tiêu chí hoàn thành: Chưa xác định.

### 10.1 Settings UI & Service mở rộng (3 công tắc độc lập)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 10.1.1 | `SettingsScreen` mở rộng (3 switch độc lập thêm vào CÙNG màn hình đã dựng ở Phase 9 — "Chặn tất cả số lạ"/"Chặn số ẩn danh"/"Chặn số quốc tế", mặc định TẮT; dòng cảnh báo text riêng cho "Chặn tất cả số lạ") | 0.5 | 1 | 2 |
| 10.1.2 | `SettingsService` mở rộng (3 flag thêm, cùng pattern `setFlag` đã có) | 0.25 | 0.5 | 1 |
| **Tổng 10.1** | | **0.75** | **1.5** | **3** |

### 10.2 Normalizer & CallScreeningService mở rộng (lõi FR-12)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 10.2.1 | `PhoneNumberNormalizer.isInternational` (Dart VÀ Kotlin, cả 2 phía — MỚI) | 0.5 | 1 | 1.5 |
| 10.2.2 | `CallScreeningService` mở rộng — bước sớm số ẩn danh (`handlePresentation` RESTRICTED/UNKNOWN/PAYPHONE, đọc cờ `block_anonymous_enabled`, return NGAY dừng pipeline) + bước cuối tái dùng `isContact` đã tính ở Phase 9 (2 nhánh `BLOCK_TOGGLE_UNKNOWN`/`BLOCK_TOGGLE_INTERNATIONAL`) | 1.5 | 3 | 5.5 |
| **Tổng 10.2** | | **2** | **4** | **7** |

### 10.3 Schema migration mở rộng

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 10.3.1 | Migration schema 2 phía: `blocklist_meta` +3 cột (`block_unknown_enabled`/`block_anonymous_enabled`/`block_international_enabled`, mỗi cột `INTEGER NOT NULL DEFAULT 0`) — KHÔNG cần sửa lại CHECK `decision_reason` (đã viết đủ bộ cho cả 4 giá trị enum ở Phase 9) | 0.5 | 1 | 2 |
| **Tổng 10.3** | | **0.5** | **1** | **2** |

### 10.4 Kiểm thử FR-12

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 10.4.1 | Unit test (`screenSafely` đủ ma trận 6 bước × giá trị cờ × is-contact × is-international; nhánh ẩn danh short-circuit — mock/spy xác nhận `isBlockedExact`/`ContactsRepository` gọi = 0 lần) | 1.5 | 3 | 5 |
| 10.4.2 | Integration test (3 cờ mới trên `blocklist_meta` thật; bộ dữ liệu biên chuẩn hóa quốc tế — input thô có/không dấu `+`; case số vừa lạ vừa quốc tế) | 1 | 2 | 3.5 |
| 10.4.3 | Vòng test thiết bị thật đa OEM (TÁI DÙNG matrix 3 thiết bị đã có từ Phase 6 — 2 hạng mục MỚI: AC3/AC4 phát hiện ẩn danh/quốc tế qua Caller ID thật, AC6 khớp đồng thời 2 điều kiện) | 1 | 2.5 | 5 |
| 10.4.4 | Vòng fix phát sinh nếu phát hiện `handlePresentation`/mã quốc gia KHÔNG đáng tin cậy trên 1+ OEM/carrier | 0 | 1.5 | 4 |
| **Tổng 10.4** | | **3.5** | **9** | **17.5** |

### 10.5 UAT & tài liệu kỹ thuật riêng FR-12

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 10.5.1 | UAT nhỏ riêng BLOCK-TOGGLE (cảm nhận 3 công tắc, dòng cảnh báo text "chặn số lạ") | 0.25 | 0.5 | 1 |
| 10.5.2 | Cập nhật tài liệu kỹ thuật riêng cho Phase 1.10 | 0.25 | 0.5 | 1 |
| **Tổng 10.5** | | **0.5** | **1** | **2** |

**Tổng Phase 10 (10.1+10.2+10.3+10.4+10.5): Best 7.25 / Likely 16.5 / Worst 31.5** — khớp đúng tuyệt đối với
"Tổng Phase 1.10" trong `estimate.md` (7.25 / 16.5 / 31.5), không có sai số cộng dồn. PERT-weighted theo
`estimate.md` ≈ 17.5 person-day (không tính lại ở đây).

Phụ thuộc nội bộ Phase 10 (suy luận kỹ thuật của agent, **chưa được input xác nhận trực tiếp** — ngoại trừ
phụ thuộc cấp Phase đã nêu ở trên lấy trực tiếp từ HLD):
- 9.1.1/9.1.2 (`SettingsScreen`/`SettingsService` gốc, Phase 9) phải xong trước 10.1.1/10.1.2 — HLD: "tái dùng
  CÙNG màn hình Settings đã dựng" (gần trực tiếp từ input).
- 9.2.1 (`CallScreeningService` pipeline, Phase 9) phải xong trước 10.2.2 — `estimate.md` tự mô tả 10.2.2
  "Tiếp nối trực tiếp việc tái cấu trúc đã làm ở Phase 1.9 (ADDITIVE, không phải viết lại lần 2)" và tái dùng
  biến `isContact` đã tính ở Phase 9 bước 4 (gần trực tiếp từ input/HLD).
- 9.3.2 (migration Phase 9, viết đủ bộ CHECK `decision_reason` cho 4 giá trị enum) phải xong trước 10.3.1 —
  `estimate.md` tự nêu 10.3.1 "KHÔNG cần sửa lại CHECK decision_reason (đã viết đủ bộ ở Phase 1.9)".
- 7.1.1 (`PhoneNumberNormalizer` Dart, Phase 7) và 3.1.3 (`PhoneNumberNormalizer` Kotlin, Phase 3) phải xong
  trước 10.2.1 — mở rộng trực tiếp cùng class 2 phía.
- 6.5.3 (vòng test thiết bị thật đa OEM, Phase 6) phải xong trước 10.4.3 — `estimate.md` tự nêu 10.4.3 "TÁI
  DÙNG matrix 3 thiết bị đã có từ Phase 1.6" (gần trực tiếp từ input).
- 10.4.2 FAIL là điều kiện kích hoạt 10.4.4 — nguồn trực tiếp từ `estimate.md` ("best=0 nếu phát hiện đúng
  ngay trên cả 3 thiết bị, không phát sinh gì thêm").
- 10.1-10.3 nên xong trước 10.4; 10.4 nên xong trước 10.5.

---

## Phase 11 — Kiểm tra trạng thái Quyền định kỳ (FR-13/PERMISSION-CHECK, mới, độc lập)

Phụ thuộc (nguồn: HLD v1.3 "Phase breakdown (bổ sung)", mục "Phase 1.10"): "Phụ thuộc: KHÔNG phụ thuộc Phase
1.8/1.9 — hoàn toàn độc lập về runtime. Phụ thuộc gián tiếp vào `PermissionsModule`/`RoleModule`/
`PermissionKeys` đã có (Phase 0) cho DANH SÁCH quyền cần kiểm tra." → ánh xạ sang số Phase của WBS này: Phase
11 KHÔNG phụ thuộc **Phase 9**/**Phase 10** (có thể chạy song song, chèn/test độc lập); phụ thuộc gián tiếp
**Phase 1** (nơi `PermissionsModule`/`RoleModule`/`PermissionKeys` được dựng — HLD gọi nền tảng này là "Phase
0", WBS này không có "Phase 0" nên việc ánh xạ "Phase 0" → **Phase 1** là suy luận ánh xạ của agent, vì HLD
không định nghĩa rõ số phase này tương ứng số nào trong WBS). Phần "KHÔNG phụ thuộc Phase 1.8/1.9" lấy trực
tiếp từ HLD; phần ánh xạ "Phase 0" là suy luận của agent, cần xác nhận lại nếu cần chính xác tuyệt đối.
Milestone/tiêu chí hoàn thành: Chưa xác định.

### 11.1 Refactor PermissionStatusChecker (tách khỏi PermissionsModule/RoleModule)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 11.1.1 | `PermissionStatusChecker` (Kotlin, MỚI — tách object `isGranted`/`isRoleHeld`/`areNotificationsEffectivelyEnabled` từ `PermissionsModule`/`RoleModule`) + refactor `PermissionsModule.statusOf()`/`RoleModule.isRoleHeld()` gọi lại qua object mới | 1 | 2 | 3.5 |
| **Tổng 11.1** | | **1** | **2** | **3.5** |

### 11.2 Hạ tầng WorkManager & Application (mới)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 11.2.1 | Gradle dependency `androidx.work:work-runtime-ktx` (`android/app/build.gradle`) | 0.1 | 0.25 | 0.5 |
| 11.2.2 | `SafeCallApplication` (Kotlin, MỚI — extends `android.app.Application` thuần, `onCreate()` tạo notification channel + enqueue `PeriodicWorkRequest` 24h `KEEP`) + sửa `AndroidManifest.xml` (`android:name="${applicationName}"` → `.SafeCallApplication`) | 0.5 | 1 | 2 |
| 11.2.3 | `PermissionCheckWorker` (Kotlin, MỚI — `androidx.work.Worker`, `doWork()` lặp 6 `PermissionCheckStep`, `notify()`/`cancel()` theo `notificationId` cố định 9001-9006, `PendingIntent` `FLAG_IMMUTABLE` kèm `EXTRA_PERMISSION_STEP_ID`) | 1.5 | 3 | 5 |
| 11.2.4 | 6 nội dung notification (title/text theo UIUX v1.7 đã viết sẵn — chỉ còn việc implement) | 0.25 | 0.5 | 1 |
| **Tổng 11.2** | | **2.35** | **4.75** | **8.5** |

### 11.3 Cơ chế resume đúng bước (MainActivity/RootDecider)

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 11.3.1 | Cơ chế resume đúng bước cho 2 quyền tùy chọn (`MainActivity` thêm `EXTRA_PERMISSION_STEP_ID` + override `onNewIntent`; case mới `"getInitialPermissionStepId"` trên `PERMISSIONS_CHANNEL`; `PermissionsBridge.getInitialPermissionStepId()`; `RootDecider` gọi check này ƯU TIÊN TRƯỚC `_runResumeCheck()` mandatory-only hiện có, cả cold start và warm resume; validate `stepId` lạ từ Intent `exported=true` không tin tưởng mù quáng) | 1.5 | 3 | 5.5 |
| **Tổng 11.3** | | **1.5** | **3** | **5.5** |

### 11.4 Kiểm thử FR-13

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 11.4.1 | Unit test (`PermissionStatusChecker` với `Context` giả; REGRESSION xác nhận `PermissionsModule`/`RoleModule` KHÔNG đổi hành vi sau refactor; `PermissionCheckWorker.doWork` mock thiếu 1/6/cả 6 quyền; "quyền cấp lại ở lần sau → cancel, không post lại") | 1.5 | 3 | 5 |
| 11.4.2 | Integration test (tap notification → `MainActivity.onNewIntent` → `getInitialPermissionStepId()` trả đúng `stepId` 1 lần, lần đọc thứ 2 trả `null`; ép chạy Worker qua `WorkManagerTestInitHelper` CHỈ cho test LOGIC, không phải độ tin cậy lịch trình thật) | 1 | 2 | 3.5 |
| 11.4.3 | Vòng test thiết bị thật đa OEM (TÁI DÙNG matrix 3 thiết bị Phase 6 — độ trễ `WorkManager` dưới Doze/App Standby THẬT, tối thiểu 1 lượt màn hình tắt + không sạc + không di chuyển vài giờ liên tục MỖI thiết bị, kết hợp thu hồi 1 quyền qua Cài đặt hệ thống trước khi vào Doze) | 1.5 | 3 | 6 |
| 11.4.4 | Vòng fix phát sinh nếu `WorkManager` không chạy đáng tin cậy trên 1+ OEM (ví dụ OEM tùy biến diệt tiến trình nền quá mức) | 0 | 2 | 5 |
| **Tổng 11.4** | | **4** | **10** | **19.5** |

### 11.5 UAT & tài liệu kỹ thuật riêng FR-13

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 11.5.1 | UAT nhỏ riêng PERMISSION-CHECK (cảm nhận thông báo nhắc cấp quyền, điều hướng đúng bước khi tap) | 0.25 | 0.5 | 1 |
| 11.5.2 | Cập nhật tài liệu kỹ thuật riêng cho Phase 1.11 | 0.25 | 0.5 | 1 |
| **Tổng 11.5** | | **0.5** | **1** | **2** |

**Tổng Phase 11 (11.1+11.2+11.3+11.4+11.5): Best 9.35 / Likely 20.75 / Worst 39** — khớp đúng tuyệt đối với
"Tổng Phase 1.11" trong `estimate.md` (9.35 / 20.75 / 39), không có sai số cộng dồn. PERT-weighted theo
`estimate.md` ≈ 21.9 person-day (không tính lại ở đây).

Phụ thuộc nội bộ Phase 11 (suy luận kỹ thuật của agent, **chưa được input xác nhận trực tiếp** — ngoại trừ
phụ thuộc cấp Phase đã nêu ở trên, phần "KHÔNG phụ thuộc Phase 9/10" lấy trực tiếp từ HLD):
- 1.1.2 (`PermissionsModule`/`RoleModule` gốc, Phase 1) phải xong trước 11.1.1 — 11.1.1 refactor tách object từ
  2 class đã có ở Phase 1.
- 11.1.1 (`PermissionStatusChecker`) phải xong trước 11.2.3 (`PermissionCheckWorker.doWork` dùng checker mới
  để lặp qua 6 `PermissionCheckStep`).
- 11.2.2 (`SafeCallApplication`, sửa Manifest) phải xong trước 11.2.3 — `onCreate()` của Application là nơi
  `enqueue` `PeriodicWorkRequest` cho Worker.
- 1.1.1 (scaffolding, Phase 1) và 1.2.1 (`OnboardingScreen`, Phase 1) phải xong trước 11.3.1 — `estimate.md`
  xác nhận cơ chế resume hiện có (`MainActivity`/`RootDecider`) được dựng từ Phase 1, 11.3.1 mở rộng trực tiếp
  cơ chế đó.
- 11.1-11.3 nên xong trước 11.4 (test cần code tồn tại); 11.4 nên xong trước 11.5.
- Phase 11 không phụ thuộc Phase 9/10 — có thể triển khai và test song song với 2 Phase đó (trực tiếp từ HLD:
  "độc lập, có thể chạy song song với Phase 1.8/1.9").

---

## Phase 12 — Cross-cutting: System/E2E, Security, Compatibility, UAT, Tài liệu

Phụ thuộc: Input (`estimate.md`/HLD) không nêu rõ Phase 12 phải xong sau toàn bộ Phase 1-11; tuy nhiên các
Task trong Phase 12 (system/E2E test theo FR-1/2/5/6/7, UAT, vòng fix tương thích) về bản chất kỹ thuật cần
các chức năng tương ứng ở Phase 1-5 đã dựng xong để test — đây là suy luận kỹ thuật của agent, chưa được input
xác nhận trực tiếp theo từng Task cụ thể. `estimate.md` không có dòng việc tương ứng dạng "Cross-cutting cho
FR-8", "Cross-cutting cho FR-9/FR-10" hay "Cross-cutting cho FR-11/FR-12/FR-13" tách riêng (các Task
cross-cutting của FR-8, FR-9/FR-10 và FR-11/FR-12/FR-13 — test thiết bị thật, UAT, tài liệu — đã nằm trong
Phase 6, Phase 7 và Phase 9/10/11 ở trên, không lặp lại tại đây).
Milestone/tiêu chí hoàn thành: Chưa xác định (Test Plan có "Exit Criteria" được `estimate.md` tham chiếu
nhưng không trích dẫn chi tiết tiêu chí vào bảng ước lượng — không tự suy diễn nội dung Exit Criteria vào đây).

### 12.1 Kiểm thử System/E2E & thiết bị thật

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 12.1.1 | Script System/E2E (adb emu gsm call/accept/cancel trên 2 API level, kịch bản đủ FR-1/2/5/6/7) | 2 | 4 | 7 |
| 12.1.2 | Vòng test trên thiết bị thật + SIM thật (FR-1/FR-2) + vòng fix phát sinh | 1 | 2 | 4 |
| **Tổng 12.1** | | **3** | **6** | **11** |

### 12.2 Bảo mật & tương thích

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 12.2.1 | Security cơ bản (loại trừ allowBackup/auto-backup, xóa dữ liệu khi gỡ app, sanity SQL injection) | 0.5 | 1 | 2 |
| 12.2.2 | Vòng fix tương thích API 29 vs 31+ (ví dụ khác biệt POST_NOTIFICATIONS runtime từ API 33) | 1 | 2 | 4 |
| **Tổng 12.2** | | **1.5** | **3** | **6** |

### 12.3 UAT

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 12.3.1 | UAT (điều phối + fix phản hồi mức blocker) | 1 | 2 | 4 |
| **Tổng 12.3** | | **1** | **2** | **4** |

### 12.4 Tài liệu kỹ thuật

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 12.4.1 | Cập nhật tài liệu kỹ thuật (README dev setup, ghi chú vận hành DDL 2 phía, đối chiếu docs vs code thật) | 0.5 | 1 | 2 |
| **Tổng 12.4** | | **0.5** | **1** | **2** |

### 12.5 Release signing & governance bổ sung (✍️ PM-edit, 2026-11-06 — ghi nhận hồi tố việc đã làm thật 2026-10-25)

> Nguồn: `DELEGATION-MAP-SAFECALL.md` (3 dòng "Bổ sung 2026-10-25") + `estimate.md` mục "Phase 1.8". 3 task này
> đã được thực hiện thật ngày 2026-10-25 nhưng bị bỏ sót khỏi WBS gốc — rubric EX-02 chấm "Fail" vì thiếu bằng
> chứng PM bắt được gap này; PM (hadv-vn) xác nhận bổ sung trực tiếp vào đây ngày 2026-11-06.

| WBS | Task | Best | Likely | Worst |
|---|---|---|---|---|
| 12.5.1 | Tạo keystore + điền `android/key.properties` (Human-do, giá trị secret thật) | 0.1 | 0.25 | 0.5 |
| 12.5.2 | Cấu hình gradle đọc `signingConfigs.release` từ `key.properties` (AI-do) | 0.1 | 0.25 | 0.5 |
| 12.5.3 | Golden-file test `PhoneNumberNormalizer` Dart↔Kotlin (58 case, đóng Risk R9) | 0.5 | 1 | 2 |
| **Tổng 12.5** | | **0.7** | **1.5** | **3** |

**Tổng Phase 12 (12.1+12.2+12.3+12.4+12.5): Best ~~6~~ 6.7 / Likely ~~12~~ 13.5 / Worst ~~23~~ 26.**
Lưu ý cách gộp: `estimate.md` tách dòng "Cập nhật tài liệu kỹ thuật" (12.4.1) RA NGOÀI dòng "Tổng Cross-cutting"
(5.5 / 11 / 21, chỉ gồm 12.1-12.3). WBS này gộp cả 12.4 vào chung Phase 12 vì cùng tính chất cross-cutting —
không phải khác biệt số liệu, chỉ là cách trình bày khác với bảng gốc. Tổng Phase 12 (có 12.4) = Tổng
Cross-cutting gốc + dòng Docs gốc = (5.5+0.5) / (11+1) / (21+2) = 6 / 12 / 23 — khớp đúng. (Phase này lần lượt
được đánh số "Phase 6" trước bản cập nhật 2026-10-21 lần 1, rồi "Phase 7" sau bản cập nhật lần 1, "Phase 8"
sau bản cập nhật lần 2, và nay là "Phase 12" sau bản cập nhật 2026-11-07 — xem ghi chú đổi số ở mục "Nguồn".)

---

## Tổng hợp toàn Phase 1

| Phase | Best | Likely | Worst |
|---|---|---|---|
| 1 — Nền tảng | 8 | 13.5 | 23 |
| 2 — Blocklist thủ công | 3.5 | 6.5 | 10.5 |
| 3 — Chặn real-time | 3.5 | 7 | 12 |
| 4 — Phát hiện + prompt | 13.5 | 23.5 | 40 |
| 5 — Lịch sử cuộc gọi | 3 | 5.5 | 8.5 |
| 6 — Xác nhận lừa đảo lúc đổ chuông + ngắt cuộc gọi (FR-8, mới) | ~~18.75~~ 18.25 | ~~37.5~~ 36.5 | ~~72~~ 70 |
| 7 — Chặn theo đầu số + Export/Import blocklist (FR-9/FR-10, mới) | 18.25 | 38 | 70.5 |
| 9 — Cơ chế ưu tiên Danh bạ (FR-11, mới) | 8 | 15.5 | 28 |
| 10 — Bật/Tắt chế độ chặn nâng cao (FR-12, mới) | 7.25 | 16.5 | 31.5 |
| 11 — Kiểm tra trạng thái Quyền định kỳ (FR-13, mới) | 9.35 | 20.75 | 39 |
| 12 — Cross-cutting + Release signing (12.5 PM-edit 2026-11-06) | ~~6~~ 6.7 | ~~12~~ 13.5 | ~~23~~ 26 |
| **TỔNG** | ~~**74.5**~~ **99.3** | ~~**143.5**~~ **196.75** | ~~**259.5**~~ **359** |

So với `estimate.md` ("TỔNG CỘNG TOÀN BỘ PHASE 1 (bao gồm Phase 1.6, 1.7, 1.8, 1.9, 1.10, 1.11)" sau bổ sung
2026-11-07: Best 99.8 / Likely 196.75 / Worst 359.5): Likely khớp đúng (196.75). Best lệch 0.5 (99.3 so với
99.8) và Worst lệch 0.5 (359 so với 359.5) — **vẫn cùng nguồn gốc với sai số cộng dồn 0.5/0.5 đã nêu ở Phase 4
từ các lần cập nhật trước** (xem mục Bất thường), KHÔNG phải do việc thêm Phase 9/10/11 hoặc đổi số Phase 8
→ 12 gây ra — cả hai Phase 9, Phase 10 và Phase 11 tự thân đều khớp đúng tuyệt đối với `estimate.md` (không
có sai số cộng dồn riêng, giống Phase 6/Phase 7).
PERT-weighted ≈ **207.55 person-day** (tính trực tiếp từ dòng TỔNG của WBS này: (99.3+4×196.75+359)/6 =
1245.3/6 = 207.55 — khác biệt nhỏ so với 207.7 của `estimate.md` do cùng sai số cộng dồn 0.5/0.5 nêu trên,
không phải sai số tính PERT mới; trước khi có Phase 6, PERT-weighted cho Phase 1-5+Cross-cutting ≈ 71
person-day; trước khi có Phase 7, ≈ 111.4 person-day; trước PM-edit 2026-11-06, ≈ 151.5/151.4 person-day;
trước khi có Phase 9/10/11 (tức dòng TỔNG 74.7/144/260.5 của bản trước) ≈ 151.9 person-day — tất cả theo
`estimate.md`/WBS tương ứng).

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
  ngoài phạm vi Task 7.3.1 — xem mục Ngoài phạm vi); **riêng Phase 9/10 (FR-11/FR-12)**: cập nhật
  `CallHistoryScreen`/`CallHistoryService.deriveDisplayStatus` để hiển thị các `decisionReason` mới
  (`CONTACTS_PRIORITY`/`BLOCK_TOGGLE_UNKNOWN`/`BLOCK_TOGGLE_ANONYMOUS`/`BLOCK_TOGGLE_INTERNATIONAL`) — HLD v1.3
  và TestPlan v1.3 tự xác nhận NGOÀI PHẠM VI lần cập nhật này (cùng loại gap đã có từ Phase 7, vẫn CHƯA đóng);
  xác nhận khả thi kỹ thuật phát hiện số ẩn danh/quốc tế đáng tin cậy (SRS 7.1, cần `system-architect`/
  `low-level-designer` xác nhận riêng cho Task 10.2.2); thiết kế nội dung copy UI/notification cho Phase 11
  (UIUX v1.7 đã viết sẵn, chỉ còn effort implement đã tính trong Task 11.2.4) (nguồn: `estimate.md`).
- Không có cấp Subtask trong tài liệu này vì `estimate.md` không phân rã Task nào xuống mức chi tiết hơn,
  kể cả 14 dòng việc của Phase 1.6 (FR-8), 17 dòng việc của Phase 1.7 (FR-9/FR-10) và 35 dòng việc của Phase
  1.9/1.10/1.11 (FR-11/FR-12/FR-13) mới bổ sung.
- Deliverable (1.1, 1.2, 2.1, 2.2..., 6.1-6.6..., 7.1-7.8..., 9.1-9.5..., 10.1-10.5..., 11.1-11.5...) là nhóm
  do agent này tạo để tổ chức các Task đã có theo thành phần kỹ thuật cùng nhóm (ví dụ "UI", "Service/Logic",
  "Kiểm thử") — không phải hạng mục công việc mới, chỉ là cách gom nhóm trình bày. Với Phase 6, 14 dòng việc
  của `estimate.md` được gom thành 6 Deliverable (6.1 Spike/PoC & overlay, 6.2 CallMonitorService & prompt
  atomicity, 6.3 Quyền & Onboarding, 6.4 Lịch sử cuộc gọi & schema migration, 6.5 Kiểm thử, 6.6 UAT & tài
  liệu). Với Phase 7, 17 dòng việc của `estimate.md` được gom thành 8 Deliverable (7.1 Blocklist CRUD &
  normalizer mở rộng, 7.2 Chặn real-time theo đầu số, 7.3 Lịch sử cuộc gọi mở rộng, 7.4 Export/Import
  blocklist, 7.5 UI blocklist mở rộng, 7.6 Schema migration mở rộng, 7.7 Kiểm thử, 7.8 UAT & tài liệu). Với
  Phase 9 (12 dòng việc), Phase 10 (11 dòng việc) và Phase 11 (12 dòng việc), mỗi Phase được gom thành 5
  Deliverable theo cùng khuôn mẫu (Settings/Service hoặc hạ tầng MỚI → lõi hot-path mở rộng → schema/lịch sử
  hoặc resume → Kiểm thử → UAT & tài liệu) — tất cả theo cùng nguyên tắc nhóm đã áp dụng cho Phase 1-5.
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
  - **Task 9.2.1, 10.2.2 (`CallScreeningService.screenSafely` thiết kế lại 2 lần liên tiếp)**: TestPlan v1.3
    Exit Criteria tự xác nhận pipeline "bị THIẾT KẾ LẠI TOÀN BỘ, không phải chỉ thêm nhánh" — hot-path nhạy
    cảm nhất của hệ thống (bắt buộc fail-open), sửa 2 lần liên tiếp trên CÙNG method ở Phase 9 rồi Phase 10 —
    đẩy rộng worst-case cả hai Task, bắt buộc test lại toàn bộ AC1-AC9 của FR-1.
  - **Task 10.2.2, 10.4.4 (phát hiện số ẩn danh/mã quốc gia qua Telecom API)**: SRS 7.1 câu hỏi mở "độ tin cậy
    phát hiện qua `handlePresentation`/Telecom API... cần `system-architect`/`low-level-designer` xác nhận khả
    thi" CHƯA được đóng tại thời điểm ước lượng này — đây là lý do chính khiến tỷ lệ worst/best của Phase 10
    (31.5/7.25 ≈ 4.3x) rộng nhất trong 3 Phase 9/10/11, theo đánh giá của chính `estimate.md`.
  - **Task 11.1.1 (refactor `PermissionStatusChecker` tách khỏi `PermissionsModule`/`RoleModule`)**: sửa code
    ĐANG HOẠT ĐỘNG, đã qua nhiều gate A+ theo governance — rủi ro regression dù LLD v1.4 xác nhận "không đổi
    hành vi/tri-state"; bắt buộc có dòng test regression riêng (Task 11.4.1), không coi là tự động đúng chỉ vì
    "chỉ là refactor".
  - **Task 11.4.3, 11.4.4 (`WorkManager` lần đầu dùng trong dự án, độ tin cậy lịch trình 24h dưới Doze/App
    Standby phụ thuộc OEM)**: MIUI/ColorOS tùy biến diệt tiến trình nền phổ biến — TestPlan v1.3 không yêu cầu
    "đúng giờ tuyệt đối" nhưng vẫn cần 1 lượt test thật tốn thời gian thực (vài giờ/thiết bị); đây là lý do
    chính khiến worst-case của 2 Task này (6 và 5) cao gần gấp đôi likely-case tương ứng.
  - Không có lịch sử git/code thực tế của chính team này để hiệu chỉnh tốc độ person-day vẫn áp dụng cho Phase
    9/10/11 — khác với Phase 6/7, `estimate.md` tự xác nhận ĐÃ ĐỌC code thật trước khi ước lượng 3 Phase này
    (xem mục "Dữ liệu đã khảo sát" của `estimate.md`), nên mức tin cậy ở mức Trung bình (cao hơn Phase 6/7)
    nhưng vẫn thấp hơn mức "Cao" vì lịch sử git của repo này (toàn bộ Phase 1.1-1.7 commit trong 1 commit duy
    nhất, cách nhau 2 ngày) không phản ánh nhịp người code thủ công, không dùng để hiệu chỉnh tốc độ được.

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
  đầu), trong khi WBS này gộp chung vào Phase 12 (Deliverable 12.4) vì cùng bản chất cross-cutting — không
  phải sai số, chỉ là khác cách trình bày; đã ghi rõ ở mục Phase 12.
- Rủi ro thiết kế chưa chốt: HLD tự xác nhận cơ chế dual-implementation API 29 vs 31+ cho TelephonyCallback
  (ảnh hưởng Task 4.1.1, lan sang Task 6.2.1) và cơ chế background reliability cho NFR-4 chưa được
  `low-level-designer` quyết định — đây là khoảng trống thiết kế đã biết, không phải lỗi của bảng ước lượng,
  nhưng ảnh hưởng trực tiếp đến độ tin cậy của effort Task 4.1.1/6.2.1.
- **Phase 6 (FR-8) không có sai số cộng dồn**: 14 dòng Task cộng trực tiếp cho đúng Best 18.75 / Likely 37.5
  / Worst 72, khớp tuyệt đối với dòng "Tổng Phase 1.6" mà `estimate.md` tự ghi — khác với Phase 4 (xem bên
  trên), ở đây không phát hiện bất thường số liệu.
- **Phase 7 (FR-9/FR-10) cũng không có sai số cộng dồn**: 17 dòng Task cộng trực tiếp cho đúng Best 18.25 /
  Likely 38 / Worst 70.5, khớp tuyệt đối với dòng "Tổng Phase 1.7" mà `estimate.md` tự ghi — giống Phase 6,
  khác Phase 4.
- **Phase 9/10/11 (FR-11/FR-12/FR-13) cũng không có sai số cộng dồn**: 12 dòng Task (Phase 9), 11 dòng Task
  (Phase 10) và 12 dòng Task (Phase 11) cộng trực tiếp cho đúng từng dòng "Tổng Phase 1.9/1.10/1.11" mà
  `estimate.md` tự ghi (8/15.5/28, 7.25/16.5/31.5, 9.35/20.75/39) — giống Phase 6/7, khác Phase 4.
- Vì Phase 12 (Cross-cutting, chịu ảnh hưởng gián tiếp từ sai số 0.5/0.5 đã có từ trước ở Phase 4) vẫn nằm
  trong dòng TỔNG toàn Phase 1 cùng Phase 6/7/9/10/11, dòng TỔNG cuối cùng (99.3 / 196.75 / 359) vẫn lệch
  0.5/0.5 so với dòng "TỔNG CỘNG TOÀN BỘ PHASE 1 (bao gồm Phase 1.6, 1.7, 1.8, 1.9, 1.10, 1.11)" của
  `estimate.md` (99.8 / 196.75 / 359.5) — lệch này 100% là do mang theo sai số cũ của Phase 4, không phải do
  Phase 9/10/11 hay việc đổi số Phase 8 → 12 gây ra.
- **Lệch dữ liệu dependency so với bản phác thảo nhiệm vụ ban đầu**: yêu cầu nhiệm vụ gốc gợi ý "Phase 9 (FR-11)
  phụ thuộc Phase 3 (chặn real-time)", nhưng đọc kỹ HLD v1.3 mục "Phase breakdown (bổ sung)" xác nhận dòng chữ
  thật là "Phase 1.7 phải hoàn thành trước" (CONTACTS-PRIORITY là ngoại lệ trực tiếp của PREFIX matching, chứ
  không phải của cơ chế real-time EXACT gốc) — ánh xạ đúng là Phase 9 phụ thuộc **Phase 7**, không phải Phase
  3. WBS này dùng Phase 7 (đúng với HLD), không dùng Phase 3 như gợi ý ban đầu — xem mục phụ thuộc của Phase 9.
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
- Task 9.4.3 ("Regression chung cả 3 phase 9/10/11") nằm trong bảng Phase 1.9 của `estimate.md` nhưng mô tả
  tự nhận là việc chung cho cả Phase 9, 10 và 11 — WBS này giữ đúng vị trí mã 9.4.3 như input (không tách
  thành 3 bản sao ở Phase 10/11) để tránh đếm effort 2-3 lần; chỉ ghi nhận ở đây để người đọc không hiểu lầm
  Task này CHỈ thuộc phạm vi Phase 9.

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
- **(Phase 9/10/FR-11/FR-12)** Cập nhật `CallHistoryScreen`/`CallHistoryService.deriveDisplayStatus` để hiển
  thị `decisionReason` mới (`CONTACTS_PRIORITY`/`BLOCK_TOGGLE_UNKNOWN`/`BLOCK_TOGGLE_ANONYMOUS`/
  `BLOCK_TOGGLE_INTERNATIONAL`) — HLD v1.3/TestPlan v1.3 tự ghi nhận NGOÀI PHẠM VI lần cập nhật này (cùng loại
  gap đã có từ Phase 7, vẫn CHƯA đóng cho cả 2 đợt). Dữ liệu đã được thu thập đúng ở `call_history.
  decision_reason` (Task 9.3.1/9.3.2), chỉ UI/logic hiển thị còn để ngỏ. Khuyến nghị dùng `ui-ux-designer`
  cập nhật `CallHistoryScreen` cho CẢ 2 gap (Phase 7 và Phase 9/10) cùng lúc, sau đó chạy lại
  `project-estimator` bổ sung effort UI riêng.
- **(Phase 10/FR-12)** Xác nhận độ tin cậy kỹ thuật phát hiện số ẩn danh/mã quốc gia qua `CallScreeningService`/
  Telecom API (Task 10.2.2/10.4.4) — SRS 7.1 câu hỏi mở CHƯA được `system-architect`/`low-level-designer` xác
  nhận khả thi tại thời điểm ước lượng. Khuyến nghị xác nhận trước khi bắt đầu Task 10.2.2.
- **(Phase 11/FR-13)** Xác nhận cấu hình `android:process` thật cho `PermissionCheckWorker`/
  `SafeCallApplication` (cùng hay khác process với `CallScreeningService`) — LLD v1.4 tự ghi "chưa xác định",
  không chặn việc code (thiết kế không phụ thuộc kết quả đó) nhưng là điểm mở kỹ thuật chưa đóng, cùng loại
  câu hỏi đã treo từ Phase 7 cho `CallScreeningService`.
- **(Phase 10/FR-12)** Tùy chọn đổi "quốc gia nhà" cho định nghĩa "số quốc tế" — SRS v6.1/5.2 xác nhận rõ đây
  là NGOÀI PHẠM VI Phase 1 (mã quốc gia tham chiếu cố định +84), không có Task nào trong Phase 10 ước lượng
  cho tùy chọn này.
