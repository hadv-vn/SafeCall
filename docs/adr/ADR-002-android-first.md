# ADR-002: Phase 1 chỉ xây dựng Android; iOS triển khai ở phase sau

| | |
|---|---|
| **Ngày** | 2026-10-20 |
| **Trạng thái** | Accepted |

## Context

SafeCall cần chọn phạm vi nền tảng cho Phase 1. Sự khác biệt giữa hai hệ điều hành ở khả năng chặn cuộc gọi:

- **Android**: hỗ trợ chặn cuộc gọi real-time qua `CallScreeningService` — role hệ thống cho phép app can thiệp ngay khi cuộc gọi đến, khớp trực tiếp với mô hình blocklist cá nhân hóa của SRS Phase 1 (FR-1, FR-2).
- **iOS**: chỉ cho phép chặn theo danh sách đồng bộ định kỳ qua `CXCallDirectoryExtension` — không real-time, yêu cầu người dùng tự bật extension trong Settings, không hỗ trợ mô hình "học" và chặn ngay lập tức theo trải nghiệm cá nhân như Android.

Đây là khác biệt căn bản về khả năng kỹ thuật được nền tảng cho phép, không phải kết quả của việc xếp ưu tiên thị trường/kinh doanh giữa hai hệ điều hành.

Người dùng chỉ mô tả quyết định cuối (Android trước, iOS sau); không có phương án thay thế nào khác (ví dụ "làm cả hai cùng lúc") được đề cập hoặc cân nhắc.

## Decision

Phase 1 của SafeCall chỉ xây dựng cho Android. iOS (dựa trên `CXCallDirectoryExtension`) thuộc phạm vi của một phase sau, nằm ngoài phạm vi Phase 1.

## Consequences

**Tích cực:**
- Ra MVP nhanh hơn, tập trung nguồn lực vào nền tảng duy nhất có khả năng hiện thực hóa đầy đủ mô hình blocklist cá nhân hóa real-time theo đúng yêu cầu SRS.
- Kiểm chứng giá trị sản phẩm trên Android trước khi đầu tư cho iOS — vốn bị giới hạn khả năng kỹ thuật từ chính Apple, không phải do SafeCall tự giới hạn.

**Tiêu cực / trade-off:**
- Người dùng iOS hoàn toàn không được phục vụ ở Phase 1.
- Kiến trúc Phase 1 không thiết kế sẵn cho App Group / `CXCallDirectoryExtension` — khi bước vào phase iOS, cần đánh giá và thiết kế bổ sung riêng; phần native call-blocking hiện tại (Kotlin/`CallScreeningService`) không tái sử dụng được trực tiếp cho iOS.
- Do bản chất khác biệt của `CXCallDirectoryExtension` (không real-time, cần đồng bộ danh sách định kỳ, người dùng phải tự bật thủ công trong Settings), trải nghiệm iOS ở phase sau sẽ khác biệt về bản chất so với Android — đây không phải là một việc "port" đơn giản, cần ADR riêng khi triển khai (**suy luận của agent dựa trên bản chất kỹ thuật đã nêu trong tài liệu kiến trúc tham khảo — cần người dùng xác nhận lại khi lập kế hoạch Phase iOS**).
