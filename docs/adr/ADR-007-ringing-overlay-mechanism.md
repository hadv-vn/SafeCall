# ADR-007: Dùng Window overlay (`TYPE_APPLICATION_OVERLAY`/`SYSTEM_ALERT_WINDOW`) cho `RingingScamOverlay`, không dùng full-screen-intent Notification

| | |
|---|---|
| **Ngày** | 2026-10-21 |
| **Trạng thái** | Accepted |

## Context

FR-8/AC1 (`docs/srs/SRS-SafeCall-Phase1.md`) yêu cầu `RingingScamOverlay` phải hiển thị **đè lên UI khác đang mở** ngay khi một cuộc gọi từ số nằm trong blocklist/bị nghi lừa đảo đang đổ chuông, bất kể người dùng đang dùng app nào tại thời điểm đó. LLD (`docs/design/LLD-SafeCall-Phase1.md`, mục "Rủi ro 3") ghi nhận đây là một hạng mục kỹ thuật chưa chốt ở v1.1, tạm nghiêng về Phương án B. `system-architect` đã phân tích lại độ tin cậy thực tế của hai phương án trong `docs/architecture/ringing-overlay-mechanism-decision.md` và đảo ngược đề xuất tạm thời đó.

**Hai phương án đã xem xét:**

- **Phương án A — Window overlay**: `WindowManager.addView` với `TYPE_APPLICATION_OVERLAY`, vẽ từ `CallMonitorService` đang chạy sẵn (cùng service dùng cho `endCall()` ở ADR-005), cần quyền đặc biệt `SYSTEM_ALERT_WINDOW` xin qua `ACTION_MANAGE_OVERLAY_PERMISSION`.
- **Phương án B — Full-screen-intent Notification**: `Notification.Builder.setFullScreenIntent()`, tận dụng hạ tầng Notification đã có sẵn cho FR-2/3/4, về lý thuyết không cần quyền riêng trên các bản Android cũ.

**Lý do Phương án B bị loại:**

1. Từ Android 10 (API 29), `setFullScreenIntent()` có ràng buộc đã công bố: nếu màn hình đang sáng VÀ mở khóa tại thời điểm notification được gửi, hệ thống **chỉ hiển thị heads-up notification**, không tự launch full-screen Activity — full-screen intent chỉ thực sự toàn màn hình khi màn hình tắt/khóa. Đây đúng là cấu trúc đối lập với tình huống chính mà FR-8/AC1 mô tả (người dùng đang mở app khác, màn hình sáng/mở khóa khi cuộc gọi lừa đảo đến). Ở đúng tình huống phổ biến nhất này, B hạ cấp thành banner cần người dùng tự chạm để mở overlay thật — mất tính "ngay lập tức", tính năng coi như vô hiệu.
2. Từ Android 13 (API 33) cần khai báo `USE_FULL_SCREEN_INTENT`; từ Android 14 (API 34), quyền này không tự động cấp cho mọi app — chỉ tự cấp cho app giữ vai trò gọi điện/báo thức (`ROLE_DIALER`/tương đương). SafeCall không giữ vai trò này (xác nhận theo ADR-001/ADR-002: mô hình `CallScreeningService`, không trở thành default dialer). Nhiều khả năng B trên Android 13+/14+ cũng cần một bước xin quyền qua Settings riêng — mất lợi thế "không cần quyền riêng" so với A, co hẹp đáng kể khoảng cách chi phí onboarding giữa hai phương án.
3. Lợi ích tái dùng hạ tầng Notification của B là thật nhưng nhỏ: phần tốn effort nhất của `RingingScamOverlay` (layout toàn màn hình, 2 nút lớn, 4 trạng thái outcome, result card) phải tự code riêng dù chọn A hay B, không tái dùng được từ Notification đơn giản hiện có cho FR-2/3/4.

Phương án A có hạn chế đã biết: `TYPE_APPLICATION_OVERLAY` không vẽ đè được lên lớp khóa màn hình (keyguard) — nếu cuộc gọi đến khi máy khóa, overlay chỉ lộ ra sau khi mở khóa. Hạn chế này được chấp nhận vì FR-8/AC1 yêu cầu đúng "đè lên UI khác **đang mở**" (không yêu cầu hoạt động tốt nhất ở màn hình khóa) — đúng thế mạnh của A, đúng thế yếu của B.

## Decision

Dùng **Phương án A** — `WindowManager.addView` với `TYPE_APPLICATION_OVERLAY`, gọi từ `CallMonitorService` (cùng service native chịu trách nhiệm `endCall()` theo ADR-005), cho `RingingScamOverlay`.

Quyền `SYSTEM_ALERT_WINDOW` được xin qua `ACTION_MANAGE_OVERLAY_PERMISSION` — một màn hình Settings riêng của hệ thống, **không phải** runtime permission dialog thông thường như `ANSWER_PHONE_CALLS`/`READ_CALL_LOG`.

Quyền này là **bắt buộc cho toàn bộ FR-8**, không giống `ANSWER_PHONE_CALLS` (ADR-005) chỉ ảnh hưởng riêng nhánh ngắt cuộc gọi: nếu `SYSTEM_ALERT_WINDOW` không được cấp, `RingingScamOverlay` không hiển thị được dưới bất kỳ hình thức nào — toàn bộ FR-8 vô hiệu, không có nhánh fallback suy giảm như các quyền khác.

Không dùng full-screen-intent Notification cho mục đích hiển thị overlay đổ chuông, dù vẫn giữ nguyên hạ tầng Notification hiện có cho FR-2/3/4 (không liên quan tới quyết định này).

## Consequences

**Tích cực:**
- Đáp ứng đúng FR-8/AC1 ở đúng tình huống phổ biến nhất (màn hình đang sáng/mở khóa, người dùng đang dùng app khác) — không bị hệ điều hành hạ cấp hành vi như Phương án B.
- Cơ chế đã được chứng minh ổn định bởi các app tương tự trên thị trường (Truecaller, chat-head Messenger) cho cùng mục đích "vẽ đè lên mọi app khác".
- Chi phí quyền của A không còn là nhược điểm đặc thù trên Android 13+/14+ — do B nhiều khả năng cũng cần một bước Settings tương tự (`USE_FULL_SCREEN_INTENT`), chênh lệch với chi phí của A co hẹp đáng kể trên các phiên bản mới.

**Tiêu cực / trade-off:**
- Cần thêm một bước onboarding riêng, rõ ràng cho `SYSTEM_ALERT_WINDOW` — khác quyền runtime thông thường, phải dẫn người dùng qua Settings (`ACTION_MANAGE_OVERLAY_PERMISSION`) thay vì một dialog hệ thống đơn giản; rationale phải giải thích rõ "không cấp = toàn bộ FR-8 không hoạt động" (nghiêm trọng hơn các quyền "tùy chọn nhẹ" khác).
- `TYPE_APPLICATION_OVERLAY` không vẽ đè được lên lớp khóa màn hình (keyguard) — nếu cuộc gọi lừa đảo đến khi máy đang khóa, overlay chỉ lộ ra sau khi người dùng tự mở khóa. Hạn chế đã biết, chấp nhận vì nằm ngoài phạm vi yêu cầu chính xác của FR-8/AC1, nhưng là một giới hạn thật cần người dùng/QA hiểu rõ, không phải lỗi.
- Vòng đời `addView`/`removeView` phải được thiết kế cẩn thận, gắn chặt với vòng đời `CallMonitorService` — nếu service bị kill bất thường (OOM, bị hệ thống thu hồi, OEM power-saving) mà không dọn dẹp đúng cách, overlay có thể "dính" lại vĩnh viễn trên màn hình, chặn người dùng dùng điện thoại — rủi ro UX nghiêm trọng hơn một bug thông thường vì ảnh hưởng toàn bộ thiết bị, không riêng SafeCall.
- `SYSTEM_ALERT_WINDOW` có lịch sử bị lợi dụng cho tapjacking/overlay attack — cần `security-auditor` review bề mặt tấn công của `RingingScamOverlay` (đảm bảo overlay không nhận input từ nguồn nào khác ngoài 2 nút của chính nó) khi có code thật.
- Cần test trên thiết bị thật đa OEM (gộp vào vòng test thiết bị thật đã có cho `endCall()`/ADR-005), vì một số OEM (MIUI, ColorOS, Realme UI) có toggle tùy biến riêng cho quyền overlay ngoài dialog chuẩn của Android, hành vi cấp/thu hồi quyền có thể khác biệt.
- Không tái dùng được hạ tầng Notification hiện có cho FR-2/3/4 — phần layout/logic của `RingingScamOverlay` (toàn màn hình, 2 nút lớn, 4 trạng thái outcome, result card) phải tự code riêng, tăng effort so với kỳ vọng ban đầu khi còn nghiêng về Phương án B.

## Ngoài phạm vi ADR này (cascade cần cập nhật)

- `docs/design/LLD-SafeCall-Phase1.md` (mục "Rủi ro 3"): thay trạng thái "chưa chốt, nghiêng B" bằng quyết định cứng A; bổ sung chi tiết `WindowManager`/`TYPE_APPLICATION_OVERLAY`, vòng đời `addView`/`removeView` — thuộc `low-level-designer`.
- `docs/design/UIUX-SafeCall-Phase1.md` (Giả định 9): cập nhật thành bước onboarding THẬT xin `SYSTEM_ALERT_WINDOW` (bắt buộc, không phải "tùy chọn nhẹ" như `ANSWER_PHONE_CALLS`); cập nhật bảng phân loại quyền + luồng mermaid — thuộc `ui-ux-designer`.
- `docs/test/TestPlan-SafeCall-Phase1.md`: bổ sung các rủi ro cần kiểm chứng thực nghiệm đã nêu trong `docs/architecture/ringing-overlay-mechanism-decision.md` (hành vi `SYSTEM_ALERT_WINDOW` trên OEM tùy biến, hành vi overlay khi máy đang khóa, tương tác giữa overlay SafeCall và UI cuộc gọi đến gốc của app Điện thoại mặc định), gộp vào vòng test thiết bị thật đa OEM đã có cho `endCall()` — thuộc `test-plan-writer`.
- Review cấu hình `exported`/bề mặt tấn công tapjacking của `RingingScamOverlay` khi có code thật — thuộc `security-auditor`.
