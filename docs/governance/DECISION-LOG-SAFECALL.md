# Decision Log — SAFECALL

> Ghi mỗi lần AI làm sai và người sửa. Viva mini cần ≥3 khối thật — Decision Log trống hoặc
> do AI tự suy luận ra "chỗ sai" = rubber-stamp = KHÔNG đạt.

## Khối #1
- **Bước/task:** Phase 1 Build — Android resource setup
- **Thời điểm:** 2026-10-22
- **AI làm gì:** Để sót file `android/app/src/main/res/mipmap-hdpi/NOTE.md` (placeholder từ Phase 1, lẽ ra phải dọn)
- **Sai/thiếu/thừa ở đâu:** Android resource merger chỉ chấp nhận `.xml`/`.png`, file `.md` làm `:app:mergeDebugResources` fail — build fail ngay lần build đầu tiên
- **Tôi sửa thế nào:** Xoá file `.md` sót lại, tạo lại icon đúng chuẩn adaptive-icon (`mipmap-anydpi-v26/ic_launcher.xml` + vector drawable + color) — hợp lệ vì minSdkVersion=29 luôn ≥26 nên không cần PNG fallback
- **Mức ủy quyền L & lý do:** L3 (Leash A) — sửa file cấu hình non-hazard, rủi ro thấp, không chạm dữ liệu/bảo mật
- **Cổng nào chặn / hard-stop:** Gradle build fail tự nhiên (fail-closed). Không phải hard-stop mơ hồ — lỗi rõ ràng `:app:mergeDebugResources`

---

## Khối #2
- **Bước/task:** Phase 1 Build — Gradle dependency resolution
- **Thời điểm:** 2026-10-22 (sau khối #1 sửa xong)
- **AI làm gì:** Khai `file_picker: ^6.1.0` trong pubspec.yaml (dùng cho FR-10 export/import blocklist) từ giai đoạn viết code ban đầu
- **Sai/thiếu/thừa ở đâu:** Bản resolve 6.2.1 có code Java gọi `io.flutter.plugin.common.PluginRegistry.Registrar` — class đã bị xoá khỏi Flutter hiện hành, biên dịch Kotlin/Java fail ("cannot find symbol")
- **Tôi sửa thế nào:** Nâng constraint pubspec.yaml lên `^8.1.2` (resolve về 8.3.7), bản này dùng Flutter v2 embedding không phụ thuộc class đã xoá
- **Mức ủy quyền L & lý do:** L3 (Leash A) — thay đổi version dependency bên thứ 3, không phải thay đổi kiến trúc, không hazard. Lỗi này là hệ quả trực tiếp của quyết định ủy quyền trước: "viết code trước, không verify build"
- **Cổng nào chặn / hard-stop:** Gradle/Kotlin compile fail. Không phải hard-stop mơ hồ

---

## Khối #3
- **Bước/task:** Phase 3 Build — Android manifest & service initialization
- **Thời điểm:** 2026-10-22 (sau khối #1, #2 build thành công, khi chạy app trên emulator)
- **AI làm gì:** Khai `android:foregroundServiceType="phoneCall"` cho `CallMonitorService` trong AndroidManifest.xml
- **Sai/thiếu/thừa ở đâu:** Trên API 34+, type `phoneCall` đòi quyền `MANAGE_OWN_CALLS` hoặc vai trò Dialer mặc định của hệ thống — SafeCall là app `CallScreeningService` (chặn cuộc gọi), không phải app Dialer, không có quyền này. App crash thật với `SecurityException` khi gọi `startForeground()` (bắt được qua `adb logcat` lọc `FATAL EXCEPTION` trên emulator thật)
- **Tôi sửa thế nào:** Đổi `android:foregroundServiceType="phoneCall"` sang `"specialUse"`. Thêm permission `FOREGROUND_SERVICE_SPECIAL_USE` + property `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` trong manifest. Sửa code Kotlin `CallMonitorService.startForegroundWithOwnNotification` để nhánh theo `Build.VERSION.SDK_INT` (specialUse chỉ từ API 34, trước đó gọi `startForeground()` không kèm type)
- **Mức ủy quyền L & lý do:** Danh nghĩa ghi ở Leash A (L3), nhưng GAP GOVERNANCE: lỗi CHẠM quyền hệ thống (permission/manifest) — đáng lẽ phải ở A+ (L4, người duyệt trước cài-và-chạy thật) vì sai sẽ crash app. Thực tế AI chạy thẳng ở Leash A không có cổng người duyệt riêng, lỗi chỉ bắt được SAU khi đã crash trên emulator
- **Cổng nào chặn / hard-stop:** Crash log `SecurityException` trên thiết bị thật (muộn hơn mức lý tưởng). Cổng lẽ ra nên là: "bước cấu hình permission/manifest chạm quyền hệ thống → flag A+, chờ duyệt" nhưng không tồn tại

---

## Khối #4
- **Bước/task:** Phase 4 Build & Phase 5 Integration — Database initialization, Dart-Kotlin cross-stack
- **Thời điểm:** 2026-10-23 (sau build thành công, khi test tay màn hình Blocklist trên emulator)
- **AI làm gì:** Đặt câu `PRAGMA journal_mode = WAL`, `PRAGMA busy_timeout`, `PRAGMA foreign_keys` bên trong callback `onCreate` ở CẢ HAI phía — Dart (`lib/db/database_provider.dart` dùng sqflite) VÀ Kotlin (`android/.../db/SafeCallDatabaseHelper.kt` dùng SQLiteOpenHelper)
- **Sai/thiếu/thừa ở đâu:** Cả hai framework (sqflite, SQLiteOpenHelper) tự động bọc `onCreate` trong transaction (BEGIN...COMMIT) mà AI không lường trước khi viết code lúc đầu (vì lựa chọn "viết trước, không verify" nên không chạy thử ngay). `PRAGMA journal_mode = WAL` KHÔNG được phép thực thi trong lúc có transaction mở → SQLite error: "cannot change into wal mode from within a transaction" → mở database fail hoàn toàn → MỌI màn hình đọc `blocklist`/`call_history` lỗi "Không tải được...". Phát hiện: bắt được THẬT khi test tay Blocklist trên emulator (SQLiteLog thật, không phải suy luận)
- **Tôi sửa thế nào:** Chuyển PRAGMA từ `onCreate` sang `onConfigure` ở CẢ HAI phía: Dart (callback `onConfigure` của sqflite), Kotlin (`onConfigure` của SQLiteOpenHelper). `onConfigure` chạy một lần mỗi connection TRƯỚC mọi transaction, nên PRAGMA thực thi được. Phía Kotlin: chưa bắt được lỗi thật trên thiết bị thực (chưa chạm code đó khi test), nhưng AI chủ động sửa dựa trên pattern cùng framework — ghi rõ "sửa phòng ngừa, chưa verify thật Kotlin, chỉ suy luận từ cơ chế framework giống Dart"
- **Mức ủy quyền L & lý do:** Danh nghĩa ghi ở Leash A (L3), nhưng GAP GOVERNANCE thứ 2: lỗi CHẠM dữ liệu (toàn bộ tầng persistence) — cao rủi ro, ảnh hưởng MỌI tính năng. Thay đổi DB-layer/schema đáng lẽ phải ở A+ (L4, người duyệt trước khi coi xong), nhưng thực tế chạy ở Leash A tự sửa không có cổng người duyệt riêng
- **Cổng nào chặn / hard-stop:** SQLiteLog trong logcat + lỗi UI thật khi test (muộn hơn mức lý tưởng). Cổng lẽ ra nên là: "bước sửa DB-layer → flag A+, chờ duyệt" nhưng không tồn tại

---

## Khối #5
- **Bước/task:** Phase 1 Design → Phase 2 Code Gen — Low-level design review
- **Thời điểm:** 2026-10-21/22 (khi được yêu cầu viết code Phase 1, phát hiện LLD incomplete)
- **AI làm gì:** Nhận thấy LLD-SafeCall-Phase1.md v1.3 thiếu phần pseudocode gốc của 4 hàm lõi: `evaluatePromptEligibility`, `resolvePromptAtomically`, `evaluateRingingPromptEligibility`, `resolveRingingYes`. Low-level-designer ghi "như v1.1/v1.2, không đổi, xem bản trước" nhưng bản trước không còn tồn tại trên đĩa. AI TỰ SUY LUẬN LẠI pseudocode từ SRS AC + ERD + UIUX thay vì DỪNG để hỏi/làm rõ
- **Sai/thiếu/thừa ở đâu:** Vi phạm nguyên tắc hard-stop: "khi thiếu input cơ bản, PHẢI dừng, không tự đoán". Rủi ro: logic triển khai có thể lệch với ý định gốc mà không ai biết, vì bản pseudocode gốc v1.1/v1.2 đã mất
- **Tôi sửa thế nào:** KHÔNG có sửa trực tiếp. Ghi nhận là "hard-stop không được chấp hành" — AI đã tự đoán logic thay vì dừng hỏi. Cần để dấu này rõ ràng để viva mini có thể hỏi kỹ hơn trong phase design-review tiếp theo nếu phát hiện logic sai
- **Mức ủy quyền L & lý do:** L5 Restricted — KHÔNG được phép tự suy luận pseudocode 4 hàm lõi từ spec khi bản gốc design bị mất. Đây là quyết định kiến trúc/logic cơ bản, người dùng/designer PHẢI làm rõ và ghi lại bản pseudocode chính thức trước khi triển khai. Thực tế: AI đã bỏ qua hard-stop này, nên ghi nhận vi phạm
- **Cổng nào chặn / hard-stop:** HARD-STOP CÓ: "LLD v1.3 incomplete (mất pseudocode gốc) → dừng viết code, chờ designer làm rõ bản chính thức". NHƯNG AI đã bỏ qua, không dừng. Ghi nhận: vi phạm hard-stop, cần review thiết kế sau

---
