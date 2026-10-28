# Telemetry — SAFECALL

| Trường | Giá trị |
|---|---|
| **Tool** | Claude Code (Claude Sonnet 5) |
| **Token (est)** | N/A — không có số liệu token thật được ghi lại trong phiên. Cần đo bằng công cụ nếu muốn reconcile sau |
| **Thời gian** | N/A — không có đồng hồ bấm giờ thật. Phiên rải từ 2026-10-20 → 2026-10-23 (nhiều ngày), nhưng giờ làm THẬT mỗi ngày chưa ghi. Cần người dùng tự bấm giờ nếu muốn số liệu chính xác |
| **Số vòng lặp** | ~5 vòng — đếm từ sự kiện sửa-lỗi quan sát được: (1) lỗi resource #1 → sửa → rebuild; (2) lỗi file_picker #2 → nâng version → rebuild; (3) build thành công lần đầu sau #1, #2; (4) lỗi service #3 phát hiện trên emulator → sửa manifest+code → rebuild+reinstall; (5) lỗi PRAGMA #4 phát hiện khi test UI → sửa database → rebuild+reinstall. Tổng: tối thiểu ~5 vòng lặp sửa-lỗi thật có thể đếm từ lịch sử hội thoại (không phải đo bằng công cụ) |
| **Rework** | CÓ — rebuild lại app nhiều lần do lỗi #1, #2 (build-time) trước bản build thành công đầu tiên; sau đó rebuild+reinstall ít nhất 2 lần nữa cho #3 (service crash) và #4 (database). Rework hệ quả: AI chọn "viết code trước, không verify build" → phát hiện nhiều lỗi sau khi build thực, không trước |
| **Human-edit** | 0 — Người dùng KHÔNG sửa trực tiếp bất kỳ dòng code nào trong giai đoạn viết+build+test. Toàn bộ 4 lỗi code (#1, #2, #3, #4) do AI tự phát hiện qua build/test thật (Gradle error, logcat crash, SQLiteLog) và tự sửa. Người dùng tham gia ở tầng quyết định cao hơn: chốt quyết định kiến trúc (Flutter vs RN), chọn mức ủy quyền rõ ("viết trước, không verify"), ra lệnh tiến Phase, xác nhận cài-emulator — KHÔNG phải tầng sửa từng dòng code. Ghi chú: Human-edit = 0 không có nghĩa là "AI không sai" (4 khối Decision Log chứng minh sai), mà là "người dùng chủ động giao toàn quyền sửa cho AI ở giai đoạn này, quyết định ủy quyền tường minh trước, không phải AI qua mặt" |

## Ghi chú bổ sung
- **Khối #5 (hard-stop tài liệu):** không phải lỗi code, nên không tính vào Human-edit, nhưng là một sự kiện governance quan trọng cần đánh dấu rõ cho viva mini
- **Số vòng lặp = ~5** là ước tính thấp nhất từ các sự kiện rõ ràng; số thật có thể cao hơn nếu có các vòng lặp nhỏ không được ghi lại trong lịch sử hội thoại
- **Token & Thời gian:** không có dữ liệu chính xác → để N/A, không bịa số. Nếu cần reconcile, dùng công cụ đo thật hoặc người dùng tự cung cấp

---

## Phần bổ sung — 2026-10-25 (release-readiness + governance + golden-file test)

| Trường | Giá trị |
|---|---|
| **Tool** | Claude Code (Claude Sonnet 5) |
| **Việc đã làm** | (1) Dựng cơ chế gradle đọc `signingConfigs.release` từ `key.properties` + runbook `RELEASE-SIGNING-SAFECALL.md`; (2) Tạo `WEEKLY-SAFECALL.md`/`DASHBOARD-SAFECALL.md` (EX-05); (3) Golden-file test `PhoneNumberNormalizer` (58 case, đóng Risk R9 một phần) |
| **Token (est)** | N/A — vẫn không có số liệu token thật |
| **Thời gian** | N/A — người dùng đã được hỏi trực tiếp (xem hội thoại) nhưng chưa cung cấp số giờ thật; không tự suy diễn |
| **Số vòng lặp** | 3 — mỗi việc (1)/(2)/(3) ở trên tính là 1 vòng, không phát sinh vòng sửa-lỗi phụ nào trong lượt này |
| **Rework** | KHÔNG — khác với giai đoạn build Phase 1-8 (rework ở 4/5 vòng), lượt này không phát hiện lỗi nào cần sửa lại. Riêng việc (3): giá trị `expect` trong fixture được tự verify bằng oracle Python độc lập **trước khi báo cáo xong** — 58/58 khớp ngay, không cần sửa |
| **Human-edit** | 0 — người dùng quyết định ở tầng lựa chọn phạm vi (trả lời câu hỏi rẽ nhánh qua `AskUserQuestion` cho việc (3), tránh đúng kiểu nhập nhằng mà Decision Log khối #5 đã vi phạm trước đó), không tự tay sửa dòng code/tài liệu nào |
| **Giới hạn đã biết** | Golden-file test (3) **chưa được chạy qua `flutter test`/`gradle test` thật** — môi trường AI không có 2 toolchain này trong PATH. Đây KHÔNG phải "Tested-Pass", chỉ là "đã viết + tự verify logic bằng oracle độc lập" — xem `RTM-SAFECALL.md`/`RISK-SAFECALL.md` đã cập nhật tương ứng |

---

## Phần bổ sung — 2026-10-25 (lần 2 — rebuild + cài emulator + test tay UI "Đầu số" FR-9)

| Trường | Giá trị |
|---|---|
| **Tool** | Claude Code (Claude Sonnet 5) |
| **Việc đã làm** | (1) Rebuild `flutter build apk --debug` sau các thay đổi gradle/signing/test của lượt trước trong cùng ngày — build thành công (`app-debug.apk`, ~162MB); (2) Cài lên emulator đang chạy sẵn (`emulator-5554`, API 34) bằng `adb install -r`, tái sử dụng dữ liệu SQLite cũ (không wipe); (3) Test tay qua `adb shell input`/`screencap`: mở app (vào thẳng `BlocklistScreen` vì onboarding đã hoàn tất từ phiên trước), thêm 1 số EXACT mới (`0987654321`→`+84987654321`, tái xác nhận FR-5), chuyển tab "Đầu số", thêm 1 entry PREFIX mới (`0999`→`+84999*`, **lần đầu test UI này cho FR-9**); (4) Kiểm `adb logcat -d *:E` sau mỗi bước |
| **Token (est)** | N/A — vẫn không có số liệu token thật |
| **Thời gian** | N/A — chưa có đồng hồ bấm giờ thật |
| **Số vòng lặp** | 1 — build, cài, và cả 2 lần test-thêm (EXACT + PREFIX) đều pass ngay lần đầu thao tác đúng; có 1 lần bấm sai toạ độ do quy đổi tỉ lệ ảnh screenshot (900x2000 hiển thị so với 1080x2400 thật) nhưng đây là lỗi thao tác công cụ test (adb tap), không phải lỗi code app — tự phát hiện qua ảnh chụp lại và sửa ngay, không tính là vòng rework của sản phẩm |
| **Rework** | KHÔNG (ở tầng code/app) — không phát hiện lỗi nào cần sửa code hay cấu hình trong lượt này |
| **Human-edit** | 0 — người dùng chỉ ra lệnh ("build lại source android", "cài lên emulator và kiểm tra thử"), không tự sửa dòng code/tài liệu nào |
| **Giới hạn đã biết** | Đây vẫn là test tay qua UI, không phải test tự động. **Chưa** test hành vi chặn cuộc gọi thật theo prefix (cần mô phỏng cuộc gọi qua `adb emu gsm call`, vẫn Not-started — xem `RTM-SAFECALL.md` FR-9/FR-1). **Chưa** test case từ chối đầu số dưới ngưỡng tối thiểu 4 ký tự (AC2) hay cảnh báo trùng contact (AC5) — chỉ test 1 case nhập hợp lệ thành công |
