# Manual QA Test Script: FR-9/FR-10/FR-13 Settings + Permission Reminder + FR-2/3/4/6/8 Call-Screening Flow (SafeCall Phase 1, Android Emulator)

| | |
|---|---|
| **Ngày soạn** | 2026-11-08 |
| **Mục đích** | Kịch bản thao tác tay (kèm lệnh `adb` cụ thể) để thực thi trên emulator `SafeCall_API34` những test case mà `TestPlan-SafeCall-Phase1.md` đã hoạch định nhưng chưa có bằng chứng thực thi, sau khi FR-11/FR-12/FR-13 chuyển trạng thái từ 🟡 Code-only sang đã có UI/code. |
| **Quan hệ với Test Plan** | Đây KHÔNG phải bản cập nhật `docs/test/TestPlan-SafeCall-Phase1.md` — chỉ là kịch bản thực thi tay cụ thể hóa một phần phạm vi đã hoạch định ở đó (FR-2, FR-3, FR-4, FR-6, FR-8, FR-9/AC2+AC5, FR-10, FR-13), dùng package `com.safecall` trên AVD `SafeCall_API34`. |
| **Phạm vi KHÔNG bao gồm** | FR-1, FR-5, FR-7, FR-11/FR-12 (giả định đã tự walkthrough UI riêng theo commit `78691bd`); test trên thiết bị thật đa OEM cho `endCall()`/Doze/Battery-optimization (TestPlan đã nêu rõ đây là điều kiện bắt buộc riêng, ngoài phạm vi emulator). |

---

## 0. Môi trường & quy ước chung

- Emulator: AVD `SafeCall_API34`, app đã build/deploy bản mới nhất (`feat(settings)`, `feat(onboarding)`, `feat(callscreening)` — commit `78691bd`/`7eefd83`/`79daea2`).
- Package: `com.safecall`.
- Mỗi test case nên dùng **số điện thoại riêng, chưa dùng ở case khác** — số đã được xác nhận (blocklist/confirmed-safe) sẽ bị nhớ vĩnh viễn và làm sai lệch case sau.
- Sau mỗi case có thu hồi quyền (`pm revoke`, `appops set ... ignore`), luôn khôi phục lại (`pm grant`, `appops set ... allow`) trước khi chạy case tiếp theo.

---

## 1. FR-13 — PermissionCheckWorker + tap điều hướng đúng bước onboarding

**Thông tin nền:** `PermissionCheckWorker` (`android/.../callscreening/PermissionCheckWorker.kt`) là `WorkManager.Worker` thường, chạy mỗi 24h, `enqueueUniquePeriodicWork` policy `KEEP` → mở lại app không reset lịch, phải ép chạy thủ công để test. 6 bước cố định với `notificationId` 9001–9006, channel `safecall_permission_check`.

| notificationId | stepId | Quyền tương ứng |
|---|---|---|
| 9001 | role_call_screening | `RoleManager` role `CALL_SCREENING` |
| 9002 | call_state_and_log | `READ_PHONE_STATE`/`READ_CALL_LOG` |
| 9003 | notifications | `POST_NOTIFICATIONS` (API 33+) |
| 9004 | system_alert_window | `SYSTEM_ALERT_WINDOW` (special permission) |
| 9005 | contacts | `READ_CONTACTS` |
| 9006 | answer_phone_calls | `ANSWER_PHONE_CALLS` |

### Bước test (dùng quyền Danh bạ — an toàn nhất, không ảnh hưởng khả năng hiển thị notification)

1. Thu hồi quyền: `adb shell pm revoke com.safecall android.permission.READ_CONTACTS`
2. Tìm job id WorkManager: `adb shell dumpsys jobscheduler | grep -B2 -A10 com.safecall` (thường là job duy nhất của package, chạy qua `SystemJobService`).
3. Ép chạy ngay: `adb shell cmd jobscheduler run -f com.safecall <jobId>`
4. Theo dõi log: `adb logcat -s PermissionCheckWorker:*` → xác nhận `doWork()` phát hiện thiếu quyền contacts.
5. Xác nhận notification: kéo thanh thông báo hoặc `adb shell cmd statusbar expand-notifications`; verify `adb shell dumpsys notification --noredact | grep -A20 safecall_permission_check` → phải thấy `notificationId=9005`.
6. Tap vào notification trong notification shade → app phải mở `MainActivity` và nhảy thẳng vào `OnboardingFlowScreen` ở step "contacts" (bỏ qua Home/BlocklistScreen). Theo dõi logcat filter Flutter để thấy `getInitialPermissionStepId` trả về `"contacts"`.
7. Khôi phục: `adb shell pm grant com.safecall android.permission.READ_CONTACTS`

### Lặp lại cho các step khác

- **answer_phone_calls (9006):** `adb shell pm revoke/grant com.safecall android.permission.ANSWER_PHONE_CALLS`
- **system_alert_window (9004):** `adb shell appops set com.safecall SYSTEM_ALERT_WINDOW ignore` / `... allow` (special permission, không dùng `pm revoke`).
- **role_call_screening (9001):** thử `adb shell cmd role remove-role-holder android.app.role.CALL_SCREENING com.safecall` (API ≥30); nếu không hỗ trợ, tắt tay qua Settings > Default apps > Caller ID & spam blocking, rồi gán lại qua UI sau test (RoleManager role không revoke được bằng `pm revoke`).

### ⚠️ Edge case riêng — step "notifications" (9003, `POST_NOTIFICATIONS`)

Nếu revoke chính `POST_NOTIFICATIONS` để test case này, bản thân notification nhắc nhở **sẽ không hiển thị được** trên Android 13+ (OS chặn ở tầng hệ thống). Verify case này qua logcat (`doWork()` log phát hiện thiếu quyền), không chờ thấy notification trên tay.

---

## 2. FR-9 AC2/AC5 — validate prefix trên UI

Nguồn: `docs/srs/SRS-SafeCall-Phase1.md:158` (AC2), `:161` (AC5). Ngưỡng thực tế trong code (`lib/blocklist/phone_number_normalizer.dart`): tổng độ dài tính cả `+`/mã quốc gia phải **≥ 5 ký tự** (tương đương ≥4 số sau `+`).

**Chuẩn bị:** thêm 1 contact test trong emulator khớp tiền tố sẽ dùng, ví dụ số `+84912345678`:
```
adb shell am start -a android.intent.action.INSERT -t vnd.android.cursor.dir/contact --es name "Test Contact" --es phone "+84912345678"
```
(hoặc thêm tay qua app Contacts rồi bấm Save.)

### AC2 — prefix quá ngắn
1. BlocklistScreen → Add entry → loại "Prefix" → nhập `+841` (tổng 4 ký tự, dưới ngưỡng 5) → Save.
2. Phải bị từ chối với đúng message: **"Đầu số quá ngắn, có thể chặn nhầm nhiều số không liên quan"**.
3. Test biên: nhập `+8491` (đủ 5 ký tự) → phải được chấp nhận, không báo lỗi độ dài — xác nhận ngưỡng đúng chứ không lệch 1.

### AC5 — prefix trùng contact
1. Nhập prefix khớp contact test, ví dụ `+8491234`.
2. Phải hiện dialog: title **"Đầu số này khớp với liên hệ trong danh bạ"**, nội dung nêu đúng số lượng contact khớp.
3. Test cả 2 nhánh: bấm Cancel → entry không lưu; bấm Confirm/tiếp tục → entry được lưu.
4. (Tuỳ chọn) Verify DB thật: `adb shell run-as com.safecall sqlite3 databases/<db_name>.db "select * from blocklist order by id desc limit 1;"` (cần debug build).

---

## 3. FR-10 — Export/Import với file picker thật

### Export (lưu ý: KHÔNG phải Save-As dialog — qua Android Share Sheet, `share_plus`/`Share.shareXFiles` ghi file JSON vào temp dir rồi mở share sheet)
1. Thêm vài entry (exact + prefix) để export có dữ liệu.
2. Blocklist → Export → xác nhận Android Share Sheet thật hiện ra (không phải mock dialog trong app).
3. Chọn app đích (ví dụ "Files" hoặc Gmail) để lưu/forward file `safecall_blocklist_export.json`.
4. Kiểm nội dung: `adb shell run-as com.safecall cat /data/data/com.safecall/cache/safecall_blocklist_export.json`.

### Import (file picker thật — package `file_picker`, lọc `.json`)
1. Tạo file JSON hợp lệ theo schema (`schema_version`, `entries[]`), push vào emulator:
   ```
   adb push test_import.json /sdcard/Download/test_import.json
   ```
2. Blocklist → Import → xác nhận **System file picker thật** (Android Files/Downloads provider) mở lên.
3. Chọn `test_import.json` → verify app đọc, validate schema, normalize, merge, dedupe đúng.
4. Test thêm 2 case lỗi: file JSON sai schema (phải báo lỗi rõ ràng, không crash) và file có entry trùng với blocklist hiện có (phải dedupe, không tạo bản trùng).
5. Verify qua UI list hoặc sqlite3 dump như mục 2.

---

## 4. FR-2/FR-3/FR-4/FR-6/FR-8 — luồng call-screening đầy đủ

**Cơ chế nền cần biết trước khi test:**
- Mô phỏng qua console AVD: `adb emu gsm call <số>` (đổ chuông), `adb emu gsm accept <số>` (trả lời), `adb emu gsm cancel <số>` (kết thúc/hủy).
- **FR-8 (overlay lúc đổ chuông) chạy song song với FR-2 (hỏi sau cuộc gọi)** trên mọi số lạ đủ điều kiện — không phải 2 luồng tách biệt, nên hầu hết case dưới đây sẽ gặp cả hai nếu không chủ động bỏ qua overlay.
- Overlay (`RingingScamOverlay`) là 1 Activity toàn màn hình độc lập, không phải Flutter UI.
- Prompt sau cuộc gọi là **notification hệ thống** (channel `safecall_scam_confirmation`, heads-up, 2 nút "Có"/"Không" xử lý native, không mở app) — dialog trong app chỉ là kênh tăng cường khi app đang foreground.

### Case 1 — FR-8 AC1+AC5: overlay lúc đổ chuông, chọn "Có" → chặn + ngắt cuộc gọi ngay
```
adb emu gsm call 0987600001
```
1. Verify overlay full-screen hiện ngay: "Đang đổ chuông...", số hiển thị, câu hỏi, cảnh báo "không thể khôi phục", 2 nút.
2. Bấm **"Có, đây là lừa đảo"** → verify chuyển "Đang xử lý..." → tự đóng + Toast "Đã chặn số này và ngắt cuộc gọi."
3. Verify cuộc gọi thực sự bị ngắt: `adb shell dumpsys telephony.registry | grep mCallState` → phải về IDLE.
4. Verify số vào blocklist (source `AUTO_CONFIRMED`): `adb shell run-as com.safecall sqlite3 databases/<db>.db "select * from blocklist where normalized_number='+84987600001';"`
5. Verify lịch sử: badge chính `blockedAtRinging`, chip phụ outcome `SUCCESS`, `user_response=YES`.
6. Gọi lại cùng số → phải bị chặn real-time ngay, không đổ chuông, không overlay (FR-8 AC3), badge mới `blockedRealtimeExact`.

### Case 2 — FR-8 AC6: chọn "Không phải lừa đảo" trên overlay
```
adb emu gsm call 0987600002
```
1. Bấm **"Không phải lừa đảo"** → overlay đóng, Toast "Đã ghi nhận, sẽ không hỏi lại số này.", cuộc gọi vẫn đổ chuông bình thường.
2. `adb emu gsm accept 0987600002` → chờ ≥3s → `adb emu gsm cancel 0987600002`.
3. Verify FR-2 **không hỏi lại** sau cuộc gọi (không có notification `safecall_scam_confirmation`).
4. Gọi lại lần 2 → overlay **không hiện lại** (FR-8 AC4 — confirmed-safe vĩnh viễn, nhất quán cả 2 luồng).

### Case 3 — FR-8 AC7+AC8: trạng thái đổi trước khi tương tác; cuộc gọi nhỡ không hỏi gì
```
adb emu gsm call 0987600003
```
1. Không bấm gì trên overlay → `adb emu gsm accept 0987600003` ngay → overlay tự đóng, không Toast, không ghi DB.
2. Chờ ≥3s → `adb emu gsm cancel 0987600003` → FR-2 prompt vẫn xuất hiện bình thường (vì đã được trả lời).
3. Case nhỡ riêng với số khác `0987600004`: gọi, không tương tác overlay, `adb emu gsm cancel` (không accept) → sau khi kết thúc không có prompt FR-2 nào.
4. Gọi lại `0987600004` lần 2 → overlay/FR-2 vẫn có thể hỏi lại (số còn "chưa xác định" — AC9/FR-8).

### Case 4 — FR-2 AC1 + FR-3 AC1: prompt sau cuộc gọi, bấm "Có" ngay trên notification
```
adb emu gsm call 0987600005
```
1. Không tương tác overlay → `accept` → chờ ≥3s → `cancel`.
2. Verify heads-up notification: "SafeCall — Xác nhận cuộc gọi", "Cuộc gọi từ 0987600005 có phải lừa đảo không?", 2 nút "Có"/"Không".
3. Bấm **"Có"** trực tiếp trên notification (không mở app) → xử lý native.
4. Verify số vào blocklist, notification tự biến mất, badge = `confirmedYes`.
5. Gọi lại → chặn real-time ngay, badge `blockedRealtimeExact`.

### Case 5 — FR-4 AC1: bấm "Không" trên notification
Lặp Case 4 với số `0987600006`, bấm **"Không"** → `markConfirmedSafe`, notification tự đóng, badge = `confirmedNo`. Gọi lại → không hỏi lại ở cả 2 luồng, đổ chuông bình thường.

### Case 6 — FR-4 AC2: bỏ qua prompt (vuốt bỏ) khác với bấm "Không"
Lặp Case 4 với số `0987600007`, **vuốt bỏ notification** (không bấm nút). Gọi lại cùng số lần 2 → prompt **hiện lại** (khác hành vi "Không" ở Case 5).

### Case 7 — FR-2 AC6: cuộc gọi quá ngắn (<3s) không hỏi
```
adb emu gsm call 0987600008
adb emu gsm accept 0987600008
adb emu gsm cancel 0987600008
```
(accept→cancel trong vòng dưới 3 giây) → verify không có notification `safecall_scam_confirmation` sau cuộc gọi.

### Case 8 — FR-2 AC2: cuộc gọi đi không hỏi
`adb shell am start -a android.intent.action.CALL -d tel:0987600009` (cần `CALL_PHONE`) hoặc `ACTION_DIAL` rồi bấm tay, kết thúc cuộc gọi → verify không có prompt nào.

### Case 9 (bonus, nối FR-9/11/12) — các badge còn lại
1. Bật "Chặn tất cả số lạ" (Settings) → gọi số mới → chặn thẳng, badge `blockedToggleUnknown`.
2. Bật "Chặn số quốc tế" → gọi từ `+1...` → chặn thẳng, badge `blockedToggleInternational`.
3. Thêm prefix (FR-9) → gọi số khớp prefix → chặn thẳng, badge `blockedRealtimePrefix`.
4. Thêm contact khớp prefix đang chặn + bật Contacts-Priority → gọi từ số đó → **không bị chặn**, có chip phụ "contact được ưu tiên".

### Edge case AC10/FR-8 — thiếu quyền `ANSWER_PHONE_CALLS`
`adb shell pm revoke com.safecall android.permission.ANSWER_PHONE_CALLS`, lặp Case 1 → bấm "Có" phải cho outcome `NOT_APPLICABLE_STATE_CHANGED` (không ngắt được cuộc gọi, báo rõ lý do), số vẫn được chặn cho lần sau nhưng cuộc gọi hiện tại tiếp tục đổ chuông. Nhớ `pm grant` lại sau test.

### ⚠️ Hạn chế đã biết (ĐÃ ĐÓNG — xem instrumented test)
`adb emu gsm call` (mô phỏng qua modem/RIL giả lập của AVD) không mô phỏng được "số ẩn danh/Private Caller ID" — do đó case "Chặn số ẩn danh" (FR-12 AC3) **không** còn dùng kịch bản thao tác tay này. Đã xác nhận (mã nguồn AOSP `CallsManager.onSuccessfulIncomingCall`) rằng đây không phải giới hạn cứng của thiết bị, chỉ là sai cơ chế mô phỏng: gap đã được đóng bằng instrumented test `AnonymousCallBlockingInstrumentedTest`
(`android/app/src/androidTest/kotlin/com/safecall/callscreening/AnonymousCallBlockingInstrumentedTest.kt`), tiêm cuộc gọi trực tiếp vào Telecom framework thật qua một `ConnectionService` test harness MANAGED (`TestConnectionService`, không self-managed — self-managed bị Telecom bỏ qua `CallScreeningService` hoàn toàn) với `handlePresentation` = RESTRICTED/UNKNOWN/PAYPHONE, buộc `CallScreeningService` thật của SafeCall xử lý — chạy được trên AVD, không cần thiết bị thật. Chạy bằng `./gradlew :app:connectedDebugAndroidTest` sau khi gán role: `adb shell cmd role add-role-holder android.app.role.CALL_SCREENING com.safecall`.

Phạm vi còn lại thật sự để lại cho thiết bị thật: hành vi hiển thị "Số bị ẩn/Private Caller ID" trên **UI dialer hệ thống** của từng OEM khi carrier mạng thật gửi CLIR — đây là test hiển thị theo carrier/OEM, không phải logic chặn của SafeCall.
