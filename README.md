# SafeCall (Phase 1 — Android)

Ứng dụng chặn cuộc gọi lừa đảo. Phase 1 chỉ hỗ trợ Android — xem `docs/` cho toàn bộ tài liệu SDLC
(requirements → SRS → ADR → HLD → LLD → ERD → UI/UX → Test Plan → estimate → WBS).

## Yêu cầu môi trường

- Flutter SDK (stable) — xem phiên bản đã dùng trong `android/local.properties`/`.metadata`.
- Android SDK: platform 34 (targetSdk), build-tools tương ứng, NDK (xem `android/app/build.gradle`
  mục `ndkVersion`).
- JDK 17 (khớp `sourceCompatibility`/`targetCompatibility`/`kotlinOptions.jvmTarget` trong
  `android/app/build.gradle`).

## Chạy dự án

```
flutter pub get
flutter run
```

`android/local.properties` cần trỏ `flutter.sdk` tới đường dẫn Flutter SDK cục bộ — file này
không commit (xem `.gitignore`), mỗi máy dev tự tạo.

## Kiến trúc tóm tắt

- `lib/` — Flutter (UI/business logic, đọc/ghi SQLite qua `sqflite`).
- `android/app/src/main/kotlin/com/safecall/callscreening/` — module native Kotlin cho toàn bộ
  chức năng chặn/giám sát cuộc gọi (`CallScreeningService`, `CallMonitorService`,
  `BlocklistPrefixIndex`, `RingingScamOverlay`...). Giao tiếp Dart↔native qua MethodChannel/
  EventChannel (`com.safecall/permissions`, `com.safecall/contacts`, `com.safecall/call_events`,
  `com.safecall/call_events_response`).
- Chi tiết thiết kế: `docs/design/HLD-SafeCall-Phase1.md` (component diagram) và
  `docs/design/LLD-SafeCall-Phase1.md` (class/sequence diagram, pseudocode).

## Ghi chú vận hành quan trọng

**Schema SQLite tồn tại ở 2 nơi, PHẢI giữ giống nhau tuyệt đối:**
- `lib/db/safecall_schema.dart` (Dart, dùng bởi `sqflite`)
- `android/app/src/main/kotlin/com/safecall/callscreening/db/SafeCallSchema.kt` (Kotlin, dùng bởi
  `SQLiteOpenHelper`)

Cả hai mở CHUNG một file `safecall.db` trong thư mục `databases/` mặc định của app (không ai
"sở hữu" schema — bất kỳ phía nào mở DB trước đều tạo bảng như nhau nhờ `CREATE TABLE IF NOT
EXISTS`). Khi sửa 1 cột/bảng/constraint, PHẢI sửa đồng thời cả 2 file, nếu không UNIQUE
constraint và validate 2 phía (vd `PhoneNumberNormalizer`) sẽ lệch nhau.

**`PhoneNumberNormalizer` cũng tồn tại 2 bản** (`lib/blocklist/phone_number_normalizer.dart` và
`android/.../callscreening/PhoneNumberNormalizer.kt`) — PHẢI cho cùng output cho cùng input, vì
UNIQUE constraint trên `blocklist.normalized_number` dựa vào giả định này.

**Chỉ MỘT điểm gọi `TelecomManager.endCall()`** trong toàn bộ codebase:
`CallMonitorService.attemptEndCall()` (ADR-005) — không gọi API này ở bất kỳ nơi khác, kể cả qua
MethodChannel từ Dart.

**`minSdkVersion = 29`** (ADR-006) — bắt buộc cho `CallStateObserverFactory` chọn đúng
`TelephonyCallbackObserver` (API 31+) hay `PhoneStateListenerObserver` (API 29-30), và cho
`RoleManager`/`endCall()` (ADR-005, yêu cầu tối thiểu API 28) hoạt động ổn định.

## Trạng thái implementation (WBS)

7/8 Phase trong `docs/planning/WBS-SafeCall-Phase1.md` đã có code: Phase 1 (nền tảng) → Phase 7
(FR-9 chặn đầu số + FR-10 export/import). **Phase 8 (cross-cutting) còn các hạng mục KHÔNG thể làm
bằng code — cần thiết bị thật/người thật:**
- 8.1.1/8.1.2 — test System/E2E qua `adb emu gsm`, test trên thiết bị thật + SIM thật (nhiều OEM,
  theo Test Plan — đặc biệt `endCall()`/`SYSTEM_ALERT_WINDOW`/`CallStateObserver` dual-impl).
- 8.3.1 — UAT (cần người dùng thật phản hồi).

Phần đã làm được bằng code trong Phase 8: rà soát bảo mật cơ bản (SQL injection — toàn bộ truy
vấn đều dùng tham số hóa `?`/`ContentValues`, không nối chuỗi; `allowBackup=false`; đúng 1 điểm
gọi `endCall()`; `exported=false` cho `CallMonitorService`/`ScamConfirmationReceiver`) và rà soát
tương thích API 29-36 (không phát hiện vấn đề mới — `targetSdkVersion` giữ 34 theo ADR-006 dù
`compileSdk` đã lên 36 theo yêu cầu tối thiểu của Flutter stable hiện tại).

**Giới hạn đã biết, chưa giải quyết (không nằm trong phạm vi Phase 1)**: `CallMonitorService`
không có cơ chế tự khởi động lại khi bị hệ điều hành kill ngoài `START_STICKY` mặc định của
`Service` (không có `BroadcastReceiver` cho `BOOT_COMPLETED` hoặc cơ chế watchdog) — khớp với
NFR-4 trong SRS, vẫn được đánh dấu "chưa xác định, cần bổ sung ở giai đoạn thiết kế".

## Giả định/quyết định triển khai không có trong tài liệu SDLC gốc

Một số quyết định được đưa ra trực tiếp khi viết code vì tài liệu không chỉ định đủ chi tiết, hoặc
vì pseudocode gốc (LLD mục E1-E4) đã bị mất trong một lần cascade cập nhật trước đó:
- `ContactsRepository`/`ContactsLookupModule` (channel `com.safecall/contacts`) — không có trong
  bất kỳ class table nào của LLD.
- `CallHistoryRepository.updateWasPrompted` — method phát sinh để thỏa CHECK constraint
  `was_prompted` khi `insertCallRecord` được gọi trước khi biết kết quả `evaluatePromptEligibility`.
- `RingingCallNumberHolder` — giải pháp cho việc `TelephonyCallback` (API 31+) không còn trả số
  điện thoại kèm trạng thái RINGING.

Nên nhờ `low-level-designer` xác nhận lại các điểm trên nếu cần khớp tuyệt đối với thiết kế gốc.
