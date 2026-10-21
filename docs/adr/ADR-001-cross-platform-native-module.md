# ADR-001: Dùng framework UI cross-platform kết hợp native module riêng (Kotlin) cho chức năng chặn cuộc gọi

| | |
|---|---|
| **Ngày** | 2026-10-20 |
| **Trạng thái** | Accepted |

## Context

SafeCall cần một kiến trúc tách biệt giữa:
- Phần giao diện (UI) và business logic chung (onboarding, quản lý blocklist, lịch sử cuộc gọi, hiển thị prompt xác nhận...), không có yêu cầu đặc thù hệ điều hành.
- Phần chặn cuộc gọi thực tế, phải tích hợp trực tiếp với API hệ thống của từng OS: Android `CallScreeningService` (Phase 1) và iOS `CXCallDirectoryExtension` (phase sau, xem ADR-002). Đây là các API hệ thống bắt buộc viết bằng native code (Kotlin cho Android) — không thể triển khai qua framework UI cross-platform.

Các phương án đã xem xét:

1. **Native hoàn toàn, 2 codebase riêng** (Kotlin cho Android, Swift cho iOS) — bị loại vì toàn bộ phần UI/business logic chung không có yêu cầu đặc thù OS, viết 2 lần làm tăng gấp đôi effort không cần thiết.
2. **Cross-platform thuần**, bao gồm cả phần chặn gọi — bị loại vì `CallScreeningService`/`CXCallDirectoryExtension` là API hệ thống bắt buộc viết native, không có cách triển khai qua framework cross-platform.
3. **Cross-platform cho UI/business logic chung + native module riêng (Kotlin) cho phần chặn gọi** — được chọn.

## Decision

Dùng một framework UI cross-platform cho phần giao diện và business logic chung của SafeCall. Viết native module riêng bằng Kotlin (Android, Phase 1) cho toàn bộ chức năng chặn cuộc gọi (`CallScreeningService`, `BroadcastReceiver`, truy cập SQLite trực tiếp). Phần native OS-integration này chạy độc lập, không phụ thuộc runtime của framework cross-platform.

## Consequences

**Tích cực:**
- Tận dụng cross-platform cho phần UI/business logic tốn effort lớn nhưng không đòi hỏi đặc thù OS, tránh viết toàn bộ app 2 lần.
- Vẫn đáp ứng được ràng buộc kỹ thuật bắt buộc (native) cho phần chặn gọi mà không phải đánh đổi toàn bộ chiến lược cross-platform.

**Tiêu cực / trade-off:**
- Phải duy trì ranh giới rõ ràng giữa hai lớp code (UI/business logic cross-platform vs native Kotlin OS-integration) — tăng độ phức tạp kiến trúc so với một codebase thuần nhất.
- Cần cơ chế chia sẻ dữ liệu giữa hai lớp (file SQLite chung), vì luồng chặn gọi real-time (native) phải hoạt động độc lập, không phụ thuộc việc runtime cross-platform (Dart/JS) có đang sống hay không.
- Hệ quả trực tiếp: rủi ro race condition khi đọc/viết đồng thời SQLite giữa native writer và lớp UI reader — cần chốt riêng chế độ WAL/locking ở giai đoạn thiết kế dữ liệu (thuộc phạm vi `data-modeler`/`low-level-designer`, chưa được chốt trong ADR này).
- Cần bridge (ví dụ `MethodChannel`/`EventChannel` hoặc tương đương) giữa hai lớp cho các luồng tần suất thấp: xin quyền/role, và nhận event "cuộc gọi vừa kết thúc" khi app đang foreground.
