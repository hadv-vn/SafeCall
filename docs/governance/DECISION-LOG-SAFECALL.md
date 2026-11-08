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

## Khối #6
- **Bước/task:** Phase 4 Build (2026-10-23, khối #4) → phát hiện lại ở Phase 9/10 khi test hành vi chặn cuộc gọi thật lần đầu (FR-1/FR-9, 2026-11-08)
- **Thời điểm:** Lỗi được ĐƯA VÀO lúc 2026-10-23 (cùng đợt sửa PRAGMA của khối #4), nhưng chỉ được PHÁT HIỆN lúc 2026-11-08 — gần **2 tuần** chặn cuộc gọi hoàn toàn không hoạt động mà không ai biết
- **AI làm gì:** Khi sửa khối #4 (chuyển PRAGMA từ `onCreate` sang `onConfigure`), AI dùng `db.execSQL("PRAGMA journal_mode = WAL")` và `db.execSQL("PRAGMA busy_timeout = ...")` trong `SafeCallDatabaseHelper.kt` (Kotlin) — tại thời điểm đó ghi rõ "chưa verify thật Kotlin, chỉ suy luận từ pattern cùng framework Dart" (xem khối #4). Suy luận đó SAI
- **Sai/thiếu/thừa ở đâu:** Trên thiết bị thật (AVD API 34), cả `PRAGMA journal_mode` VÀ `PRAGMA busy_timeout` ở dạng setter đều trả về 1 dòng kết quả (giá trị thực tế sau khi set) — khác với Dart/sqflite. `execSQL()` của Android chỉ dành cho câu lệnh KHÔNG trả dữ liệu, nên ném `SQLiteException` MỖI LẦN `onConfigure()` chạy. Vì `onConfigure()` chạy lại ở MỖI connection mới — tức MỖI LẦN `CallScreeningService.onScreenCall` được gọi (do `dbHelper` là property lazy theo instance) — exception này nổ ra ở **MỌI cuộc gọi đến**. Hợp đồng fail-open bắt buộc (`screenSafely` bắt `BlocklistReadException`) **nuốt lỗi này âm thầm, không log** → kết quả: EXACT và PREFIX blocking **hoàn toàn không hoạt động** trên thiết bị thật, mọi cuộc gọi đều được cho qua bất kể có trong blocklist hay không — từ 2026-10-23 tới khi phát hiện. Không ai biết vì: (1) khối #4 tự nhận "chưa verify thật Kotlin"; (2) mọi "Tested-Pass"/"verify" trước đó cho FR-1/FR-5/FR-9 chỉ là test tay UI (thêm/xoá/hiển thị số trong `BlocklistScreen`), **không phải mô phỏng cuộc gọi thật** — `RTM-SAFECALL.md` tự ghi rõ FR-1 "chưa có thao tác gọi/giả lập cuộc gọi thật nào" suốt từ 2026-10-25 tới tận lúc này
- **Tôi sửa thế nào:** Đổi 3 call-site PRAGMA có trả dòng (`journal_mode` ×1 trong `onConfigure`, `busy_timeout` ×2 — `onConfigure` và `openReadOnlyForScreening`) từ `db.execSQL(...)` sang `db.rawQuery(...).use { it.moveToFirst() }`. Giữ `PRAGMA synchronous = NORMAL` ở `execSQL()` vì câu này KHÔNG trả dòng. Phát hiện bằng cách lần đầu mô phỏng cuộc gọi đến thật qua `adb emu gsm call`, thấy fail-open âm thầm, thêm log tạm để lần ra gốc rễ (log tạm đã xoá trước khi commit). Sau fix, verify lại qua `adb emu gsm call`: số khớp EXACT bị chặn + ghi `call_history` với `matched_entry_type=EXACT`; số khớp PREFIX bị chặn + ghi đúng `matched_prefix_value`; số không trong blocklist vẫn được cho qua — **đây là lần ĐẦU TIÊN hành vi chặn cuộc gọi thật của SafeCall được verify qua mô phỏng cuộc gọi, không phải chỉ test tay UI**
- **Mức ủy quyền L & lý do:** Danh nghĩa L3 (Leash A, cùng mức với khối #4 gốc) — nhưng đây là ví dụ cực đoan của đúng rủi ro mà khối #4 đã tự cảnh báo ("sửa phòng ngừa, chưa verify thật Kotlin"): một suy luận chưa verify ở tầng DB-layer (Layer 0) đã vô hiệu hoá HOÀN TOÀN tính năng lõi của toàn app trong ~2 tuần mà không có cổng nào bắt được sớm hơn, vì chưa có vòng test nào từng chạm tới hành vi chặn cuộc gọi thật (chỉ test tay UI)
- **Cổng nào chặn / hard-stop:** Không có cổng nào bắt được cho tới khi có người CHỦ ĐỘNG chạy `adb emu gsm call` lần đầu (2026-11-08) — đây không phải hard-stop bị bỏ qua, mà là **thiếu hẳn một cấp độ verify** (test hành vi end-to-end, không chỉ build/test tay UI) trong toàn bộ quy trình trước đó. Bài học áp dụng ngay cho các Task còn lại: "build xanh" + "test tay UI" KHÔNG đủ để coi một tính năng hot-path là hoạt động đúng — cần ít nhất 1 lần mô phỏng hành vi thật (System/E2E, Task 8.1.x) trước khi tin vào bất kỳ mitigation "đã code đúng" nào ở Layer 0

---

## Khối #7
- **Bước/task:** Phase 9/10 — verify tay FR-11/FR-12 (SettingsScreen + CallScreeningService pipeline), phiên riêng sau khối #6
- **Thời điểm:** Phát hiện 2026-11-08, trong lúc verify Case A2 (cờ `contacts_priority_enabled` ON, số contact khớp PREFIX đã chặn) — CHƯA sửa
- **AI làm gì:** Verify FR-11 bằng cách gọi `adb emu gsm call` tới số contact khớp PREFIX đã chặn ở cả 2 trạng thái cờ, đối chiếu kết quả qua Call Log hệ thống (`type`) — không dừng ở đó, còn thử đối chiếu thêm qua `CallHistoryScreen`/bảng `call_history` nội bộ app để lấy bằng chứng `decisionReason=CONTACTS_PRIORITY` như kế hoạch test ban đầu đề ra
- **Sai/thiếu/thừa ở đâu:** Khi cờ ON (cuộc gọi được cho qua, đổ chuông rồi kết thúc bằng `adb emu gsm cancel`), Call Log hệ thống ghi đúng `type=3` (MISSED, không bị REJECTED) — xác nhận `CallScreeningService` quyết định đúng. NHƯNG bảng `call_history` nội bộ app **không có dòng mới** sau nhiều lần poll (tới 10s), dù `CallMonitorService` đang chạy (`dumpsys activity services` xác nhận), có đủ quyền `READ_CALL_LOG`, và `TelephonyManager` đã về `mCallState=0` (IDLE) đúng. Có nghĩa `CallMonitorService.handleCallEnded()` không insert record cho cuộc gọi "cho qua rồi kết thúc" này — khác với luồng "bị chặn" (ghi ngay trong `onScreenCall`, đã xác nhận đúng ở Case A1/B2/C2). Logcat của app không có dòng log nào từ `CallScreeningService`/`CallMonitorService` (code không log info-level), chỉ thấy lặp lại cảnh báo không liên quan `SQLiteLog: database is locked` (`PRAGMA journal_mode=TRUNCATE`) đúng lúc Flutter (sqflite) và native (SQLiteOpenHelper) cùng mở file `safecall.db` — NGHI VẤN nhưng CHƯA xác nhận được đây là nguyên nhân (có thể `CallHistoryWriteException` bị nuốt âm thầm do lock contention, hoặc một điều kiện khác trong `handleCallEnded()`/`CallLogNumberResolver`/observer chưa bắt được sự kiện IDLE đúng cách)
- **Tôi sửa thế nào:** CHƯA sửa — đây là phát hiện phụ ngoài phạm vi yêu cầu gốc (verify FR-11/FR-12 qua pipeline `CallScreeningService`, không phải qua `CallMonitorService`/Luồng 2). Ghi nhận rõ để không chặn việc đóng FR-11/FR-12 (bằng chứng Call Log hệ thống đã đủ cho AC1/AC5 của 2 FR này), nhưng không được coi là "đã verify" cho phần badge `decisionReason` trên `CallHistoryScreen` mà FR-11/FR-6 cùng phụ thuộc
- **Mức ủy quyền L & lý do:** L3 (Leash A) cho hành động verify (chỉ đọc/quan sát, không sửa code) — nhưng PHÁT HIỆN này chạm Layer 0 tương tự khối #6 (hot-path ghi `call_history`), nên bất kỳ fix sau này cho `CallMonitorService.handleCallEnded()`/`CallLogNumberResolver` nên được coi là A+ (L4), cần người duyệt trước khi merge, giống tiền lệ khối #4/#6
- **Cổng nào chặn / hard-stop:** Không có cổng tự động nào bắt được (không log, không exception nổi lên UI) — chỉ phát hiện được vì lượt verify FR-11/FR-12 này chủ động đối chiếu CẢ 2 nguồn (Call Log hệ thống VÀ `call_history` nội bộ) thay vì tin 1 nguồn duy nhất. Cần điều tra thêm trước khi đóng: (1) thử lại với cuộc gọi ĐƯỢC TRẢ LỜI thật (`adb emu gsm accept` + chủ động hangup) thay vì `cancel` lúc đang chuông — loại trừ khả năng `type=3 MISSED` không đi qua đúng nhánh `handleCallEnded()` như `type=1 INCOMING` (đã trả lời); (2) thêm log tạm (giống cách khối #6 đã lần ra gốc rễ) nếu lặp lại được hiện tượng

---
