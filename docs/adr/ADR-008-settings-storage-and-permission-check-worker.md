# ADR-008: Lưu cờ cài đặt FR-11/FR-12 trong `blocklist_meta` (SQLite) và chạy PermissionCheckWorker (FR-13) bằng `androidx.work` thuần native, không qua plugin Flutter

| | |
|---|---|
| **Ngày** | 2026-11-07 |
| **Trạng thái** | Accepted |

## Context

SRS v6.0 (`docs/srs/SRS-SafeCall-Phase1.md`) bổ sung 3 FR mới làm phát sinh 2 quyết định kiến trúc liên quan tới nhau trong cùng một cascade, do `system-architect` phân tích và coordinator xác nhận chọn:

- **FR-11 (CONTACTS-PRIORITY)**: 1 cờ boolean, mặc định **BẬT**.
- **FR-12 (BLOCK-TOGGLE)**: 3 cờ boolean độc lập — `block_unknown`, `block_anonymous`, `block_international` — mặc định **TẮT** cả 3.
- **FR-13 (PERMISSION-CHECK)**: tác vụ nền định kỳ mỗi ngày kiểm tra TẤT CẢ quyền quan trọng (vai trò chặn gọi, trạng thái cuộc gọi, danh bạ, thông báo, `ANSWER_PHONE_CALLS`, Overlay `SYSTEM_ALERT_WINDOW`) và gửi thông báo nhắc nếu thiếu.

Cả 3 FR đều phải được `CallScreeningService`/tác vụ nền native đọc được, trong khi `CallScreeningService` có thể chạy ở **process riêng** với Flutter engine (`android:process` — nợ kỹ thuật cũ, LLD v1.3 ghi rõ "CHƯA xác nhận dứt điểm", thuộc phạm vi `system-architect`, xem `docs/design/LLD-SafeCall-Phase1.md` mục "Ngoài phạm vi LLD này"). Dự án đã từng gặp đúng loại rủi ro cross-process này và giải quyết bằng bảng singleton `blocklist_meta` (SQLite) lưu `prefix_version` cho `BlocklistPrefixIndex` (xem ERD/LLD v1.2, FR-9).

### Quyết định 1 — Nơi lưu 4 cờ cài đặt FR-11/FR-12

**Hai phương án đã xem xét:**

1. **`SharedPreferences`/Flutter `shared_preferences`** — bị loại: không đảm bảo nhất quán cross-process nếu `CallScreeningService` chạy process riêng với Flutter engine; thêm một cơ chế lưu trữ thứ hai song song với SQLite đã có, phá vỡ nguyên tắc "một nguồn sự thật" mà dự án đang giữ cho dữ liệu runtime ảnh hưởng quyết định chặn.
2. **Mở rộng bảng singleton `blocklist_meta` (SQLite) đã có** — được chọn: dự án đã dùng đúng bảng này để giải quyết vấn đề cross-process tương tự (`prefix_version`), đọc chung một query, không thêm dependency Dart mới.

### Quyết định 2 — Cơ chế PermissionCheckWorker (FR-13)

**Hai phương án đã xem xét:**

1. **Plugin Flutter `workmanager`** — bị loại: mỗi lần chạy phải khởi động headless Flutter engine chỉ để gọi lại native qua `MethodChannel`, tốn tài nguyên/thời gian khởi động, thêm phụ thuộc vòng đời Flutter vào loại tác vụ mà kiến trúc hiện có (ADR-001, ADR-005) đang chủ động tránh.
2. **`androidx.work.PeriodicWorkRequest` thuần native Kotlin** — được chọn: toàn bộ API cần (`RoleManager.isRoleHeld`, `checkSelfPermission`, `Settings.canDrawOverlays`, `NotificationManagerCompat`) đều là API Android thuần, không cần business logic Dart; nhất quán với nguyên tắc "đường đảm bảo cho hành vi quan trọng không phụ thuộc vòng đời Flutter engine" đã áp dụng cho `endCall()` ở ADR-005.

## Decision

### 1. Lưu trữ cài đặt FR-11/FR-12

Mở rộng bảng singleton `blocklist_meta` (SQLite, đã có từ FR-9) thêm 4 cột boolean mới:

| Cột | Default | Ý nghĩa |
|---|---|---|
| `contacts_priority_enabled` | `1` (true) | FR-11/AC1 |
| `block_unknown_enabled` | `0` (false) | FR-12/AC1 |
| `block_anonymous_enabled` | `0` (false) | FR-12/AC1 |
| `block_international_enabled` | `0` (false) | FR-12/AC1 |

Đúng theo quy ước boolean đã chốt cho schema này (`INTEGER`, giá trị `0`/`1`, kèm `CHECK (col IN (0,1))`, xem ERD mục "Boolean"). Mọi thay đổi cài đặt từ `SettingsScreen` (Dart) phải ghi qua repository hiện có của `blocklist_meta` — không ghi trực tiếp `SharedPreferences` ở bất kỳ đâu trong codebase.

Không thay đổi tinh thần local-only của ADR-003: 4 cờ này vẫn là dữ liệu cấu hình thuần cục bộ trên thiết bị, không qua backend, không đồng bộ.

### 2. PermissionCheckWorker (FR-13)

Dùng `androidx.work.PeriodicWorkRequest` (chu kỳ 24h, không `Constraints` đặc biệt — tác vụ nhẹ, chỉ đọc trạng thái quyền local + gửi notification), enqueue tại `Application.onCreate()` bằng:

```kotlin
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "permission_check_worker",
    ExistingPeriodicWorkPolicy.KEEP,
    periodicWorkRequest
)
```

`ExistingPeriodicWorkPolicy.KEEP` để sống sót qua reboot/app restart (không tạo nhiều bản enqueue trùng lặp mỗi lần `onCreate()` chạy lại).

API kiểm tra quyền cụ thể theo từng loại:
- Vai trò chặn gọi: `RoleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)`.
- Trạng thái cuộc gọi, danh bạ, `ANSWER_PHONE_CALLS`: `ContextCompat.checkSelfPermission()`.
- Overlay `SYSTEM_ALERT_WINDOW`: `Settings.canDrawOverlays(context)` — quyền đặc biệt, KHÔNG qua `checkSelfPermission()`.
- Thông báo: `NotificationManagerCompat.areNotificationsEnabled()` (mọi API level) + `checkSelfPermission(POST_NOTIFICATIONS)` bổ sung trên API 33+.

Chống thông báo trùng trong cùng một lần chạy: dùng notification ID cố định theo từng loại quyền (ví dụ `NOTIFICATION_ID_MISSING_OVERLAY`). `NotificationManagerCompat.notify(fixedId, ...)` tự thay thế thông báo cũ cùng ID — đạt tính idempotent miễn phí từ API Notification, KHÔNG cần thêm bảng DB mới để theo dõi trạng thái đã thông báo.

Toàn bộ logic `PermissionCheckWorker` là Kotlin thuần, không gọi qua `MethodChannel`, không khởi động Flutter engine.

## Consequences

**Tích cực:**
- Giữ ĐÚNG MỘT nguồn sự thật cho dữ liệu runtime ảnh hưởng quyết định chặn (SQLite `blocklist_meta`), tránh lặp lại đúng loại rủi ro cross-process mà dự án đã từng gặp và giải quyết — không thêm dependency Dart mới (`shared_preferences`).
- `PermissionCheckWorker` không phụ thuộc vòng đời Flutter engine, nhất quán với nguyên tắc đã thiết lập ở ADR-005 cho `endCall()` — không tốn chi phí khởi động headless Flutter engine mỗi lần chạy định kỳ.
- Idempotent "miễn phí" nhờ notification ID cố định — không cần thêm bảng DB theo dõi trạng thái đã thông báo, giảm độ phức tạp schema.
- `ExistingPeriodicWorkPolicy.KEEP` đảm bảo tác vụ sống sót qua reboot/app restart mà không tạo nhiều bản enqueue trùng lặp.

**Tiêu cực / trade-off:**
- Thêm migration mở rộng `blocklist_meta` (4 cột mới) — `data-modeler` cần thiết kế migration tăng `SUPPORTED_SCHEMA_VERSION`, đảm bảo giá trị default đúng (`contacts_priority_enabled=1`, 3 cột còn lại `=0`) khi áp dụng cho DB đã tồn tại từ trước (người dùng nâng cấp app, không phải cài mới).
- Gắn thêm 4 cờ cấu hình không liên quan trực tiếp tới blocklist vào một bảng vốn được thiết kế ban đầu chỉ cho `prefix_version` — bảng `blocklist_meta` dần trở thành "bảng cấu hình chung" hơn là bảng chuyên biệt theo đúng tên gọi; cần đặt lại tên/tách bảng ở phase sau nếu số lượng cờ cấu hình tiếp tục tăng (YAGNI tạm chấp nhận cho Phase 1, giống lý do ERD đã ghi khi thiết kế singleton ban đầu).
- **Rủi ro Doze/App Standby**: `WorkManager` KHÔNG đảm bảo chạy đúng giờ dưới Doze hoặc App Standby bucket "restricted" (đặc biệt OEM aggressive battery management như MIUI/ColorOS) — có thể trễ hơn 24h, là hành vi "best-effort, cuối cùng sẽ chạy", không phải "đúng hẹn giờ". Dự án chủ động KHÔNG xin `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` để né rủi ro này (rủi ro bị Play Store từ chối/gắn cờ, trải nghiệm xin quyền quá nặng so với lợi ích) — chấp nhận độ trễ này cho Phase 1. UX copy phải dùng ngôn ngữ "khoảng mỗi ngày", không hứa "đúng 24h" — thuộc phạm vi `ui-ux-designer`.
- **Rủi ro `POST_NOTIFICATIONS` tự tham chiếu**: nếu quyền thông báo (hoặc `POST_NOTIFICATIONS` trên API 33+) bị thu hồi, chính cơ chế PERMISSION-CHECK dùng để báo "thiếu quyền" cũng không thể gửi thông báo được — kể cả không thể tự báo về việc THIẾU quyền thông báo. Không có giải pháp kỹ thuật thật trong phạm vi chỉ dùng Notification API của Android. Fallback duy nhất là cơ chế đã có ở FR-7/AC4 (banner khi mở app foreground). Chấp nhận là rủi ro còn tồn đọng đã biết cho Phase 1, cùng cấp độ với rủi ro "không có call-ID" đã ghi trong ADR-005.
- Vẫn còn nợ kỹ thuật cũ chưa giải quyết trong ADR này: cấu hình `android:process` thật cho `CallScreeningService` (process riêng hay cùng process với Flutter engine) CHƯA được `system-architect` chốt dứt điểm (xem LLD v1.3, "Ngoài phạm vi LLD này") — quyết định lưu trữ ở `blocklist_meta` trong ADR này là đúng BẤT KỂ kết quả chốt đó là gì (SQLite đọc được từ mọi process), nhưng bản thân câu hỏi "process riêng hay không" vẫn mở, ngoài phạm vi ADR này.

## Tham chiếu chéo

- **ADR-003** (local-only blocklist): không mâu thuẫn — 4 cờ cài đặt mới vẫn là dữ liệu cấu hình thuần cục bộ trên thiết bị, không qua backend, không đồng bộ, không chia sẻ giữa người dùng/thiết bị.
- **ADR-005** (call-termination-mechanism): nguyên tắc "đường đảm bảo cho hành vi quan trọng không phụ thuộc vòng đời Flutter engine" được áp dụng lại nguyên vẹn cho `PermissionCheckWorker`.

## Ngoài phạm vi ADR này (cascade cần cập nhật)

- `data-modeler`: viết migration thật thêm 4 cột vào `blocklist_meta` (bao gồm `CHECK` ràng buộc 0/1, giá trị default đúng cho DB nâng cấp từ bản cũ), cập nhật ERD/LLD.
- `ui-ux-designer`: copy UX cho thông báo nhắc quyền phải dùng ngôn ngữ "khoảng mỗi ngày"; xác định số lượng thông báo khi nhiều quyền cùng bị thiếu trong một chu kỳ (gộp 1 hay tách riêng mỗi loại — câu hỏi mở đã ghi trong SRS v6.0, mục câu hỏi mở FR-13/AC2).
- `security-auditor`: review `PermissionCheckWorker`/notification channel khi có code thật (đảm bảo không lộ thông tin nhạy cảm qua nội dung notification).
- `test-plan-writer`: bổ sung test case cho hành vi trễ dưới Doze/App Standby trên thiết bị thật đa OEM, và test case notification ID cố định không tạo thông báo trùng.
