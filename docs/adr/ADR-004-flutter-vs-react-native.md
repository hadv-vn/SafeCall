# ADR-004: Dùng Flutter làm framework UI cross-platform (không chọn React Native)

| | |
|---|---|
| **Ngày** | 2026-10-20 |
| **Trạng thái** | Accepted |

## Context

Theo ADR-001, SafeCall cần một framework UI cross-platform cho phần giao diện/business logic chung, với native module Kotlin riêng cho chức năng chặn gọi (`CallScreeningService` chạy hoàn toàn độc lập trong native, đọc trực tiếp SQLite cục bộ, không phụ thuộc runtime UI framework).

`system-architect` đã so sánh React Native (RN) và Flutter theo 4 tiêu chí (phân tích chi tiết tại `docs/architecture/framework-tradeoff.md`):

1. **Bridge cho native module** (xin quyền/role, event "cuộc gọi vừa kết thúc" khi app foreground) — cả hai đều không cần bridge cho luồng chặn gọi real-time (chạy 100% trong Kotlin); chênh lệch không đáng kể ở phần bridge cần thiết. **Kết luận: hòa.**
2. **Độ chín ecosystem plugin telephony/contacts/permissions** — Flutter nhích nhẹ hơn (`flutter_contacts`, `permission_handler` chuẩn thực tế, ít phân mảnh) so với RN (`react-native-permissions` từng gặp breakage qua các lần nâng cấp RN). Không quyết định vì phần khó nhất (custom role/screening) giống nhau ở cả hai.
3. **Background/foreground service theo dõi trạng thái cuộc gọi** — là thành phần native Android thuần túy, vòng đời do OS quản lý, giống nhau hoàn toàn bất kể framework UI. **Kết luận: hòa.**
4. **Khả năng maintain lâu dài** — yếu tố quyết định: RN có hiring pool JS/TS lớn hơn, nhưng đã trải qua biến cố churn kiến trúc (Old Architecture → New Architecture, 2023-2025) từng làm vỡ nhiều native module bên thứ ba — rủi ro nguy hiểm cho một app phụ thuộc rất sâu vào native Kotlin integration (`CallScreeningService` là trái tim của app). Flutter ổn định từ ~2019, chưa có migration kiến trúc tương đương, và tự vẽ widget nên nhất quán hơn qua các OEM Android.

Người dùng đã xác nhận chọn theo khuyến nghị Flutter trong tài liệu trade-off, xác nhận không có bias kỹ năng team sẵn có với JS/React cần ưu tiên hơn khuyến nghị kỹ thuật.

## Decision

SafeCall dùng **Flutter** làm framework UI cross-platform. Không dùng React Native.

## Consequences

**Tích cực:**
- Tránh rủi ro churn kiến trúc lịch sử của RN (Old→New Architecture) có thể làm vỡ native module tích hợp `CallScreeningService` trong tương lai — rủi ro được đánh giá là nguy hiểm hơn cho SafeCall so với app thông thường vì giá trị cốt lõi nằm ở native integration.
- Ecosystem permission/contacts thống nhất hơn, ít phân mảnh hơn RN.
- UI tự vẽ widget nhất quán hơn qua các OEM Android khác nhau, giảm gánh nặng QA cho một app mà độ tin cậy là yếu tố sống còn.

**Tiêu cực / trade-off:**
- Team cần học/dùng Dart; hiring pool cho Dart nhỏ hơn JS/TS của RN — tuy trade-off này được giảm giá trị phần nào vì kỹ năng khó nhất của app (Kotlin/native integration) vẫn giống nhau bất kể chọn framework UI nào.
- Cấu trúc project cố định: `lib/` cho UI/Dart code, `android/app/src/main/kotlin/...` cho native Kotlin (`CallScreeningService`, SQLite DAO, `BroadcastReceiver`).
- Giao tiếp giữa hai lớp qua `MethodChannel` (request/response) và `EventChannel` (event một chiều, ví dụ "cuộc gọi vừa kết thúc" khi app foreground).
- Còn một quyết định kỹ thuật chưa chốt, phát sinh trực tiếp từ lựa chọn này: cần chọn thư viện SQLite phía Dart tương thích chế độ WAL/locking với driver native Kotlin để tránh race condition đọc/viết đồng thời — thuộc phạm vi `data-modeler`/`low-level-designer`, **chưa được chốt trong ADR này**.
