# ADR-006: Chốt `minSdkVersion` = 29 (Android 10) cho SafeCall Phase 1

| | |
|---|---|
| **Ngày** | 2026-10-21 |
| **Trạng thái** | Accepted |

## Context

ADR-005 (`docs/adr/ADR-005-call-termination-mechanism.md`) yêu cầu cứng: `minSdkVersion` của project phải ≥ 28 (Android 9), hoặc nếu thấp hơn thì phải feature-gate hành vi ngắt cuộc gọi theo `Build.VERSION.SDK_INT >= 28` — vì `TelecomManager.endCall()` (dùng cho FR-8/SCAM-DETECT-05) chỉ khả dụng từ API 28 trở lên. ADR-005 đặt ra ràng buộc tối thiểu nhưng không tự chốt giá trị `minSdkVersion` thực tế.

HLD (`docs/design/HLD-SafeCall-Phase1.md`, mục Deployment view) ghi rõ: "Giá trị `minSdkVersion` thực tế **chưa xác định** (chưa có Gradle config)" — đây là khoảng trống cần được chốt trước khi viết `build.gradle` thật.

Test Plan (`docs/test/TestPlan-SafeCall-Phase1.md`, mục Môi trường & dữ liệu test và bảng Loại kiểm thử áp dụng — Compatibility) đã dùng **Android Emulator API 29** (Android 10) làm baseline và **API 31+** (Android 12, nơi `TelephonyCallback` mới khả dụng) làm mốc so sánh, từ trước khi FR-8 được bổ sung vào SRS (API 29 được chọn làm baseline cho lý do khác: hành vi `TelephonyCallback` không đảm bảo cung cấp số điện thoại trên Android 10+ cho app thường — rủi ro đã có giải pháp dùng `CallLog`, không liên quan tới ngưỡng của `endCall()`).

Tính tới 2026, Android 10 (API 29, phát hành 2019) trở lên chiếm đại đa số thiết bị Android đang hoạt động — thị phần thiết bị dưới API 29 không đáng kể đối với một app mới ra mắt.

**Phương án đã xem xét và bị loại**: đặt `minSdkVersion = 28` (sát đúng ngưỡng tối thiểu mà ADR-005 yêu cầu, lẽ ra sẽ cho phép hỗ trợ thêm một phiên bản Android). Bị loại vì hai lý do: (1) chênh lệch chỉ một version (28 vs 29) không mang lại lợi ích thực tế do thị phần Android 9 trở xuống không đáng kể; (2) không khớp với baseline API 29 đã có sẵn trong Test Plan — chọn 28 sẽ buộc phải mở rộng ma trận test (thêm một môi trường API 28) hoặc chấp nhận một khoảng hở chưa test giữa ngưỡng kỹ thuật tối thiểu (28) và ngưỡng thực tế được test (29), mà không có lợi ích tương xứng.

## Decision

`minSdkVersion = 29` (Android 10) cho toàn bộ project SafeCall Phase 1.

Vì 29 > 28 (ngưỡng tối thiểu của ADR-005), **không cần** feature-gate theo `Build.VERSION.SDK_INT` cho hành vi `TelecomManager.endCall()` — nhánh fallback "feature-gate nếu thấp hơn 28" mô tả trong ADR-005/AC10 của FR-8 không áp dụng.

Tuy nhiên, quyết định này kéo theo một ràng buộc kỹ thuật khác cần được ghi nhận: `TelephonyCallback` (dùng trong `CallMonitorService` để theo dõi trạng thái cuộc gọi) chỉ khả dụng từ API 31. Với `minSdkVersion = 29`, project **bắt buộc** cần dual-implementation cho việc quan sát trạng thái cuộc gọi — `TelephonyCallback` cho API 31+, `PhoneStateListener` (hoặc cơ chế tương đương) cho API 29-30. Chi tiết thiết kế của dual-implementation này thuộc phạm vi `low-level-designer` (có thể cần một ADR riêng nếu phát sinh trade-off kỹ thuật đáng ghi lại) — **không lặp lại chi tiết ở đây**, ADR này chỉ ghi nhận đây là hệ quả bắt buộc phát sinh trực tiếp từ việc chốt `minSdkVersion = 29`.

## Consequences

**Tích cực:**
- Khớp đúng với baseline test đã có sẵn trong Test Plan (API 29/API 31+) từ trước khi FR-8 được thêm — không cần mở rộng ma trận môi trường test, không tạo khoảng hở giữa ngưỡng kỹ thuật và ngưỡng được test.
- Vượt ngưỡng tối thiểu của ADR-005 (≥ 28) nên không cần thêm logic feature-gate theo `SDK_INT` cho riêng hành vi `endCall()` — giảm một nhánh code/test case so với phương án 28.
- Loại trừ thiết bị Android 9 trở xuống khỏi phạm vi hỗ trợ không phát sinh chi phí thực tế đáng kể, vì thị phần nhóm thiết bị này không đáng kể đối với một app mới ra mắt năm 2026.

**Tiêu cực / trade-off:**
- Thiết bị Android 9 (API 28) trở xuống **không dùng được SafeCall** — người dùng nhóm này bị loại khỏi tập khách hàng tiềm năng ngay từ đầu. Được chấp nhận vì thị phần nhỏ, và vì `endCall()` (tính năng lõi FR-8) tự nó cũng đã yêu cầu tối thiểu API 28, nên biên độ bỏ lỡ so với mức tối thiểu kỹ thuật là không lớn.
- `android/app/build.gradle` cần khai báo rõ `minSdkVersion 29` — đây là một thay đổi cấu hình thật, hiện HLD đang ghi "chưa xác định" cho giá trị này và cần được đồng bộ (cascade, xem mục Ngoài phạm vi dưới).
- **Bắt buộc dual-implementation cho call-state observation** (`TelephonyCallback` cho API 31+, `PhoneStateListener`/tương đương cho API 29-30) — đây là hệ quả trực tiếp, không tránh được, của việc chọn 29 thay vì 31. Tăng độ phức tạp code của `CallMonitorService` (hai nhánh logic cần maintain song song, nguy cơ lệch hành vi giữa hai implementation) và tăng chi phí test (cần test cả hai nhánh trên cả hai dải API, không chỉ một). Đây là nợ kỹ thuật/gap đã được `estimate.md`/WBS ghi nhận là cần giải quyết trước khi code `CallMonitorService` — ADR này không giải quyết thiết kế cụ thể của dual-implementation, chỉ xác nhận nó là bắt buộc.

## Ngoài phạm vi ADR này (cascade cần cập nhật)

- `docs/design/HLD-SafeCall-Phase1.md` (mục Deployment view) hiện ghi "Giá trị `minSdkVersion` thực tế chưa xác định" — cần cập nhật thành 29, nhưng việc sửa tài liệu HLD không thuộc phạm vi Write/Edit của ADR này.
- `android/app/build.gradle` (khi được tạo) cần khai báo `minSdkVersion 29` — thuộc phạm vi code, không phải ADR.
- Thiết kế cụ thể của dual-implementation `TelephonyCallback`/`PhoneStateListener` cho `CallMonitorService` — thuộc `low-level-designer`, có thể cần một ADR riêng nếu phát sinh trade-off đáng ghi lại (ví dụ khác biệt hành vi giữa hai cơ chế ảnh hưởng tới độ chính xác số điện thoại nhận được).
</content>
