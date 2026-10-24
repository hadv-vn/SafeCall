# Definition of Ready (retroactive) — SAFECALL

> Theo Build Playbook bước [7]. **Ghi chú trung thực:** file này được viết SAU KHI lát cắt dọc đã build+test
> xong (không đúng thứ tự chuẩn — DoR phải PASS trước khi build). Mục đích ở đây là đánh giá retroactive:
> nếu checklist này được chạy TRƯỚC lúc build, nó đã PASS hay FAIL? Đây là một bài học thẳng, không phải một
> tờ giấy hợp thức hoá ngược.

## Lát cắt dọc (Walking Skeleton) được chọn

Luồng đã thực tế build và demo được end-to-end: **Onboarding/cấp quyền (Phase 1) → thêm số vào Blocklist
(Phase 2) → lưu SQLite → hiển thị lại đúng trên UI**, verify tay trên emulator thật (`SafeCall_API34`, API
34), gồm: role `RoleManager` thật, 4 runtime permission thật, bước `SYSTEM_ALERT_WINDOW` settings-redirect
thật, thêm `0901234567` → chuẩn hoá đúng `+84901234567`, persist + hiển thị đúng chip loại/nguồn.

## Checklist DoR — đánh giá retroactive

| # | Mục | PASS/FAIL (nếu chạy TRƯỚC build) | Ghi chú |
|---|---|---|---|
| 1 | **Spec rõ?** | ✅ PASS | SRS v3.0 FR-5 (blocklist thủ công, AC1-8) + FR-7 (onboarding, AC1-5) đầy đủ, AC đo được rõ ràng. |
| 2 | **Data model có?** | ✅ PASS | ERD v1.2 có bảng `blocklist` + `schema_migrations` đầy đủ cột/kiểu/constraint cho đúng lát cắt này. |
| 3 | **API contract có?** (nội bộ — MethodChannel, vì local-only không có API ngoài) | ✅ PASS | LLD có pseudocode đầy đủ cho `PermissionsBridge`/`BlocklistService` — **lưu ý:** defect LLD đã biết (mất pseudocode 4 hàm lõi E1-E4) KHÔNG ảnh hưởng lát cắt này, vì 4 hàm đó thuộc Phase 4/6 (luồng prompt xác nhận), không thuộc Phase 1/2. |
| 4 | **AC đo được?** | ✅ PASS | Đã dẫn ở mục 1 — mỗi AC có Given/When/Then rõ ràng, verify tay khớp từng AC khi test. |
| 5 | **Mức Leash đã gán?** | ❌ **FAIL** | **Chưa có Delegation Map nào tồn tại trước khi build** — `DELEGATION-MAP-SAFECALL.md` chỉ được viết SAU, ở lượt làm việc này. Task 1.1.2 (PermissionsModule, chạm quyền hệ thống) đáng lẽ phải được gán A+ và chờ duyệt manifest trước khi build — thực tế không có bước gán Leash nào trước khi code. |
| 6 | **Cổng kiểm là gì?** | ❌ **FAIL** | Không có cổng kiểm cụ thể được định nghĩa TRƯỚC build cho lát cắt này (ví dụ "build phải xanh + chạy demo tay PASS 8/8 AC thì mới Done"). Thực tế: build chạy, lỗi được tự phát hiện/sửa qua vòng lặp, rồi mới test tay — không có checklist PASS/FAIL tường minh được chốt trước. |

**Kết quả tổng: 4/6 PASS, 2/6 FAIL.** Theo đúng nguyên tắc Build Playbook ("nếu thiếu mục nào → chưa được
build"), lát cắt này **lẽ ra KHÔNG ĐƯỢC build** cho tới khi mục 5 và 6 được chốt. Thực tế nó đã được build
(và cuối cùng chạy đúng, sau khi tự sửa 4 lỗi — xem Decision Log), nhưng đây là hệ quả trực tiếp và nhất quán
với quyết định ủy quyền tường minh mà người dùng đã chọn: *"Viết code trước, không verify build"* — một lựa
chọn chủ động đánh đổi tốc độ, không phải một sơ suất bị bỏ sót.

## 🔒 Cổng hiểu — áp dụng cho chính DoR này

- **Lát cắt dọc này đi xuyên những tầng nào?** Onboarding UI (Dart) → PermissionsBridge (MethodChannel) →
  PermissionsModule/RoleManager (Kotlin, tầng hệ điều hành) → ngược lại BlocklistScreen UI (Dart) →
  BlocklistService (Dart) → sqflite (SQLite file dùng chung) — đi từ móng quyền hệ thống tới 1 tính năng bề
  mặt (blocklist CRUD), đúng tinh thần bước [2] Module Map (dù Module Map không được viết thành file riêng
  cho SafeCall).
- **Mục DoR nào còn FAIL và vì sao chưa được build khi thiếu nó?** Mục 5 (Leash) — vì Task 1.1.2 chạm quyền
  hệ thống, không có cổng người duyệt trước khi build đã dẫn trực tiếp tới nguy cơ (và ở Task 4.1.1 tương tự,
  đã THẬT SỰ xảy ra) crash `SecurityException` chỉ được phát hiện sau khi cài lên thiết bị — phát hiện muộn
  hơn mức có thể, nếu Leash đã được gán và kiểm trước.

## Bài học áp dụng cho các lát cắt còn lại (test OEM thật, FR-8 endCall, release)

Trước khi build/chạy các Phase còn thiếu test thật (6, 7, 8 — test đa OEM, performance sanity-check, security,
UAT), chốt **đủ cả 6 mục DoR**, đặc biệt mục 5 (Leash — dùng `DELEGATION-MAP-SAFECALL.md` vừa tạo) và mục 6
(cổng kiểm cụ thể, ví dụ: "sanity-check hiệu năng Task 7.7.3 PASS + 0 regression Task 7.7.6" là cổng, không
phải "nhìn có vẻ chạy được").
