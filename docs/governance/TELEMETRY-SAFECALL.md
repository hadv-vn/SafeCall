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
