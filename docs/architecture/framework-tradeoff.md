# Trade-off: React Native vs Flutter cho SafeCall (Android Phase 1)

| | |
|---|---|
| **Ngày** | 2026-10-20 |
| **Trạng thái** | Đề xuất — cần chốt thành ADR sau khi quyết định |
| **Input** | `docs/srs/SRS-SafeCall-Phase1.md` (FR-1..FR-7, NFR-1..7) |
| **Ràng buộc đã chốt (không so sánh lại)** | Cross-platform UI + native module Kotlin riêng cho chặn gọi; `CallScreeningService` chạy hoàn toàn độc lập trong native, đọc trực tiếp SQLite cục bộ, không phụ thuộc runtime JS/Dart; Phase 1 chỉ Android. |

## Bối cảnh hiện tại
Repo chưa có code sản phẩm — đây là lựa chọn framework khởi điểm, không phải thay thế stack đang chạy. Phân tích dựa hoàn toàn trên yêu cầu SRS và đặc tính kỹ thuật của hai framework.

**Giả định cần xác nhận**: phân tích giả định một team trung lập, không có bias kỹ năng sẵn có. Nếu team/founder đã có thế mạnh rõ ràng với một trong hai stack, điều đó nên lấn át khuyến nghị chung này.

---

## So sánh theo 4 tiêu chí

### 1. Bridge native module cho phần chặn gọi (tần suất thấp: xin quyền/role, event best-effort khi foreground)
Cả hai đều **không cần** bridge cho luồng chặn real-time (chạy 100% trong Kotlin). Bridge chỉ cần cho: xin quyền/role, và push event "cuộc gọi vừa kết thúc" khi app đang foreground.
- **React Native**: Native Modules (`@ReactMethod` + Promise) đủ dùng; event một chiều qua `DeviceEventEmitter`. `android/` là Gradle project thật, không có lớp dịch giữa.
- **Flutter**: `MethodChannel` cho request/response, `EventChannel` → Dart `Stream` cho event — khớp tự nhiên hơn với mô hình "push event một chiều". `android/` cũng là Gradle project thật.
- **Kết luận**: Hòa, chênh lệch không đáng kể ở quy mô bridge cần thiết.

### 2. Độ chín ecosystem plugin cho telephony/call-screening
Không framework nào có plugin chính thức cho `CallScreeningService`/`ROLE_CALL_SCREENING` — cần custom native module ở cả hai.
| | React Native | Flutter |
|---|---|---|
| Contacts | `react-native-contacts` (lâu năm) | `flutter_contacts` (hiện đại hơn) |
| Runtime permissions | `react-native-permissions` (từng gặp breakage qua các lần nâng cấp RN) | `permission_handler` (chuẩn thực tế, ít phân mảnh) |
- **Kết luận**: Nhích nhẹ về Flutter, không quyết định vì phần khó nhất (custom role/screening) giống nhau.

### 3. Background/foreground service theo dõi trạng thái cuộc gọi
Thành phần native Android thuần túy, vòng đời do OS quản lý, không phụ thuộc Dart/JS runtime — giống nhau hoàn toàn bất kể framework UI.

### 4. Khả năng maintain lâu dài
| | React Native | Flutter |
|---|---|---|
| Hiring pool | Lớn hơn (JS/TS phổ biến) | Nhỏ hơn |
| Rủi ro churn kiến trúc | Đã trải qua Old→New Architecture (2023-2025), gây breakage nhiều native module bên thứ ba | Ổn định từ ~2019, chưa có migration kiến trúc tương đương |
| Nhất quán UI đa OEM | Phụ thuộc view native nhiều hơn | Tự vẽ widget, ít lệch hơn |
- **Kết luận**: RN thắng hiring-pool thô; Flutter thắng ổn định kiến trúc + nhất quán UI. Vì phần khó nhất (CallScreeningService, SQLite, BroadcastReceiver, foreground service) luôn cần dev vững Kotlin bất kể UI framework, lợi thế hiring-pool JS của RN bị giảm giá trị.

---

## Khuyến nghị: Flutter

1. Bridge native và background-service hòa tuyệt đối.
2. Ecosystem permission/contacts của Flutter thống nhất hơn.
3. **Yếu tố quyết định**: SafeCall phụ thuộc rất sâu vào native Kotlin (CallScreeningService là trái tim của app). RN có lịch sử churn kiến trúc (Old→New Architecture) từng làm vỡ nhiều native module bên thứ ba — rủi ro nguy hiểm cho app có giá trị cốt lõi nằm ở native integration. Flutter không có biến cố migration kiến trúc tương đương.
4. UI nhất quán hơn qua OEM Android giảm gánh nặng QA cho một app mà độ tin cậy là yếu tố sống còn.
5. Hiring-pool nhỏ hơn của Dart là nhược điểm thật nhưng giảm giá trị vì kỹ năng khó nhất vẫn là Kotlin.

**Caveat bắt buộc**: nếu team/founder hiện tại đã có kinh nghiệm JS/React sẵn, chi phí chuyển đổi sang Dart sẽ làm giảm lợi thế trên — trường hợp đó nên ưu tiên RN để tận dụng kỹ năng sẵn có. **Đây là decision point cần người dùng xác nhận, không tự chốt được vì không có thông tin về team thực tế.**

## Thành phần & phụ thuộc bị ảnh hưởng
- Cấu trúc project: `android/app/src/main/kotlin/com/safecall/callscreening/` (CallScreeningService, DAO SQLite/Room, BroadcastReceiver) — giống nhau bất kể RN/Flutter. UI nằm ở `lib/` (Flutter) hoặc `src/` (RN).
- `BlocklistService` phía UI và native Kotlin phải đọc/viết cùng một file SQLite — cần chốt thư viện SQLite phía UI tương thích chế độ WAL/locking với driver native, tránh race condition (việc của `data-modeler`/`low-level-designer`).
- Nhận event từ native khi app foreground: Flutter `EventChannel`→`Stream`; RN `NativeEventEmitter`/`DeviceEventEmitter` — map 1:1 sang luồng hiển thị prompt (FR-2/FR-3/FR-4).
- AndroidManifest khai báo `CallScreeningService` — độc lập với framework UI.

## Rủi ro kỹ thuật chính
- Race condition đọc/viết đồng thời SQLite giữa native writer và UI reader — cần chốt WAL/locking trước khi viết `BlocklistService`.
- `android:allowBackup` có thể sao lưu file SQLite chứa số điện thoại bên thứ ba (NFR-1) — cần loại trừ/mã hóa trong manifest, bất kể framework.
- Nếu chọn Flutter mà team chưa có kinh nghiệm Dart — chi phí học tập ban đầu (không lớn với dev đã quen Kotlin/mobile).

## Decision point cần chốt trước khi ghi ADR
- Chọn RN hay Flutter — ảnh hưởng toàn bộ cấu trúc project, khó đổi sau này.
- Thư viện SQLite phía UI + chế độ WAL/locking chia sẻ với native Kotlin (chốt cùng `data-modeler`).

## Cân nhắc bảo mật
- Blocklist/history chứa số điện thoại bên thứ ba (NFR-1) — loại trừ khỏi auto-backup hoặc mã hóa, bất kể framework.
- Không có backend/auth ở Phase 1 nên không có secret/token cần quản lý.
- Bridge chỉ truyền dữ liệu nội bộ, không mở bề mặt tấn công mạng mới.
