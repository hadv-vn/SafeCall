# Quyết định kỹ thuật: Cơ chế hiển thị RingingScamOverlay (FR-8/SCAM-DETECT-05 — chốt LLD "Rủi ro 3")

| | |
|---|---|
| **Ngày** | 2026-10-21 |
| **Trạng thái** | ĐÃ CHỐT — không còn mở |
| **Nguồn căn cứ** | `docs/design/LLD-SafeCall-Phase1.md` (mục "Rủi ro 3"), `docs/design/UIUX-SafeCall-Phase1.md`, `docs/srs/SRS-SafeCall-Phase1.md` (FR-8/AC1), `docs/adr/ADR-001-cross-platform-native-module.md`, `docs/architecture/call-termination-feasibility.md`, `docs/adr/ADR-005-call-termination-mechanism.md` |

## Kết luận

**Chốt Phương án A — Window overlay (`TYPE_APPLICATION_OVERLAY`, quyền `SYSTEM_ALERT_WINDOW`).**

Đảo ngược "đề xuất tạm thời nghiêng về B" trong LLD v1.1. Lý do: Phương án B có một hành vi hệ điều hành đã được tài liệu hóa khiến nó **cấu trúc không đáp ứng được chính FR-8/AC1** ("hiển thị đè lên UI khác đang mở") ở đúng tình huống phổ biến nhất — người dùng đang mở app khác, màn hình sáng/mở khóa khi có cuộc gọi lừa đảo đến.

## 1. Phân tích độ tin cậy thực tế — lý do quyết định

### 1.1 Full-screen-intent bị hạ cấp thành heads-up khi màn hình đang mở/mở khóa
Từ Android 10 (API 29), `Notification.Builder.setFullScreenIntent()` có ràng buộc đã công bố: **nếu thiết bị đang được sử dụng (màn hình sáng VÀ mở khóa) tại thời điểm notification được gửi, hệ thống CHỈ hiển thị heads-up notification, KHÔNG tự động launch full-screen Activity.** Full-screen intent chỉ thực sự "toàn màn hình" khi màn hình tắt/khóa. Đối chiếu FR-8/AC1: kịch bản mô tả rõ là người dùng ĐANG dùng app khác (màn hình sáng, mở khóa) — chính xác tình huống bị hạ cấp. Người dùng phải tự chạm banner heads-up để mở overlay thật — mất tính "ngay lập tức", tính năng coi như vô hiệu ở chính tình huống SRS mô tả.

### 1.2 `USE_FULL_SCREEN_INTENT` bị siết thành quyền đặc biệt từ Android 13/14
Từ API 33 cần khai báo `USE_FULL_SCREEN_INTENT`; từ API 34, quyền này không tự động cấp cho mọi app — chỉ tự cấp cho app giữ vai trò gọi điện/báo thức. SafeCall không giữ `ROLE_DIALER`/`InCallService` — nhiều khả năng không thuộc nhóm tự động cấp (cần kiểm chứng thực nghiệm). Nếu đúng, Phương án B trên Android 13+/14+ **cũng cần một bước xin quyền qua Settings riêng** — mất lợi thế "miễn phí" so với Phương án A.

### 1.3 Phương án A phù hợp đúng use case AC1
`TYPE_APPLICATION_OVERLAY` (qua `WindowManager.addView` từ `CallMonitorService` đang chạy sẵn) vẽ đè lên mọi app khác khi màn hình sáng/mở khóa — không bị hạ cấp theo trạng thái sử dụng như B. Cơ chế đã được chứng minh ổn định bởi các app tương tự (Truecaller, chat-head Messenger).

**Hạn chế đã biết (không loại trừ A, nhưng cần test)**: `TYPE_APPLICATION_OVERLAY` không vẽ đè được lên lớp khóa màn hình (keyguard) — nếu cuộc gọi đến khi máy khóa, overlay chỉ lộ ra sau khi mở khóa. Đây là đánh đổi đối lập với B, nhưng vì FR-8/AC1 yêu cầu đúng "đè lên UI khác **đang mở**" (không yêu cầu hoạt động tốt nhất ở màn hình khóa), ưu tiên đúng theo đặc tả là A.

## 2. Chi phí onboarding — đánh giá lại
Trên Android ≤ 12: A có chi phí onboarding cao hơn thật (cần bước Settings riêng). Trên Android 13+/14+: cả A và B nhiều khả năng đều cần bước Settings tương tự — chênh lệch co hẹp đáng kể. Quyền `SYSTEM_ALERT_WINDOW` (A) KHÔNG thể xếp "tùy chọn nhẹ" như `ANSWER_PHONE_CALLS` — không cấp thì overlay không hiển thị được dưới bất kỳ hình thức nào (khác `ANSWER_PHONE_CALLS`, overlay vẫn hiện nhưng không ngắt được gọi). Cần bước rationale onboarding RIÊNG, rõ ràng, giải thích "không cấp = toàn bộ FR-8 không hoạt động". Kết luận: chi phí onboarding của A có thật nhưng không đủ lớn để đảo ngược quyết định — cái giá của B (tính năng không hoạt động đúng ở tình huống chính của AC1) nghiêm trọng hơn.

## 3. Tái dùng hạ tầng Notification — đánh giá lại
Phần tốn effort nhất của `RingingScamOverlay` (layout toàn màn hình, 2 nút lớn, 4 trạng thái outcome, result card) phải tự code riêng dù chọn A hay B — không tái dùng được từ Notification đơn giản hiện có cho FR-2/3/4. Lợi ích tái dùng code của B là thật nhưng nhỏ, không đủ cân bằng rủi ro độ tin cậy ở mục 1.

## Bảng so sánh trade-off (thay bảng "đề xuất tạm thời" trong LLD)

| Tiêu chí | A — Window overlay (chọn) | B — Full-screen-intent Notification |
|---|---|---|
| Đáp ứng FR-8/AC1 (đè lên UI đang mở) | Đáp ứng đúng, ổn định | KHÔNG đáng tin cậy — hạ cấp thành heads-up khi màn hình sáng/mở khóa |
| Hoạt động khi màn hình khóa/tắt | Hạn chế — không qua được keyguard | Tốt — use case B được thiết kế cho |
| Chi phí quyền (Android ≤ 12) | Cần `SYSTEM_ALERT_WINDOW` qua Settings riêng | Không cần quyền riêng |
| Chi phí quyền (Android 13+/14+) | Không đổi theo version | Nhiều khả năng cũng cần `USE_FULL_SCREEN_INTENT` qua Settings riêng (cần kiểm chứng) |
| Tái dùng hạ tầng có sẵn | Thấp | Trung bình (chỉ phần tạo notification) |
| Độ phức tạp vòng đời/rủi ro leak | Cao hơn — tự quản lý `addView`/`removeView` | Thấp hơn — do hệ thống Notification quản lý |
| Rủi ro OEM | Có (toggle tùy biến MIUI/ColorOS...) | Có (hành vi fullScreenIntent tùy biến OEM) |

## Rủi ro cần kiểm chứng ở Test Plan (không đổi quyết định)
1. Hành vi auto-grant `USE_FULL_SCREEN_INTENT` cho app có `ROLE_CALL_SCREENING` trên Android 13/14 — chỉ liên quan nếu sau này cân nhắc fallback B cho thiết bị cũ.
2. `SYSTEM_ALERT_WINDOW` trên OEM tùy biến mạnh (MIUI, ColorOS, Realme UI) — gộp vào vòng test thiết bị thật đa OEM đã có cho `endCall()`.
3. Hành vi overlay khi máy đang khóa — xác nhận thực nghiệm overlay lộ ra ngay khi mở khóa trong lúc vẫn đang đổ chuông.
4. Tương tác giữa overlay SafeCall và UI cuộc gọi đến gốc của app Điện thoại mặc định — xác nhận overlay thực sự hiện đè lên, không bị che mất.

## Cân nhắc bảo mật
`SYSTEM_ALERT_WINDOW` có lịch sử bị lợi dụng cho tapjacking/overlay attack — `RingingScamOverlay` PHẢI không nhận input từ nguồn nào khác ngoài 2 nút của nó. Vòng đời `View`/`WindowManager` quản lý sai có thể để overlay "dính" lại trên màn hình sau khi `CallMonitorService` bị kill bất thường — rủi ro UX nghiêm trọng (chặn người dùng dùng điện thoại), cần cơ chế dọn dẹp chắc chắn trong `onDestroy()`/`onTaskRemoved()`.

## Hệ quả cần cascade (chưa thực hiện ở tài liệu này)
- **LLD**: thay "chưa chốt, nghiêng B" bằng quyết định cứng A; bổ sung chi tiết `WindowManager`/`TYPE_APPLICATION_OVERLAY`, vòng đời `addView`/`removeView`.
- **UI/UX**: Giả định 9 → bước onboarding THẬT xin `SYSTEM_ALERT_WINDOW` (bắt buộc, không phải "tùy chọn nhẹ" như `ANSWER_PHONE_CALLS`); cập nhật bảng phân loại quyền + luồng mermaid.
- **Test Plan**: bổ sung 4 rủi ro kiểm chứng, gộp vào vòng test thiết bị thật đa OEM.

## Ngoài phạm vi tài liệu này
Pseudocode/chi tiết vòng đời `WindowManager` — thuộc `low-level-designer`. Thiết kế rationale onboarding — thuộc `ui-ux-designer`. Test case thiết bị thật — thuộc `test-plan-writer`. Review `exported`/bề mặt tấn công — thuộc `security-auditor`.
