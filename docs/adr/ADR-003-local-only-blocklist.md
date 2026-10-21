# ADR-003: Blocklist lưu local-only trên thiết bị, nguồn dữ liệu duy nhất là xác nhận của chính người dùng

| | |
|---|---|
| **Ngày** | 2026-10-20 |
| **Trạng thái** | Accepted |

## Context

Mục tiêu nghiệp vụ của SafeCall Phase 1 là một MVP cá nhân hóa: mỗi người dùng tự "học" blocklist riêng dựa trên trải nghiệm của chính họ, không phải mô hình cộng đồng chia sẻ dữ liệu giữa nhiều người dùng.

Các phương án đã xem xét:

1. **Local-only** (không backend, không chia sẻ/đồng bộ giữa người dùng, không đa thiết bị) — được chọn cho MVP.
2. **Crowd-sourced / community-shared blocklist** (nhiều người dùng cùng góp số lừa đảo vào một danh sách chung) — bị loại cho Phase 1: giá trị sản phẩm cao hơn (network effect) nhưng đòi hỏi backend, cơ chế chống report ảo/lạm dụng, và hạ tầng server ngay từ đầu — vượt quá phạm vi MVP.

Nguồn dữ liệu duy nhất của blocklist được giữ nguyên gồm hai luồng:
- Xác nhận "Có" của người dùng sau prompt hỏi "Đây có phải lừa đảo không?" hiển thị ngay sau khi một cuộc gọi đến đã kết nối từ số lạ kết thúc (FR-2/FR-3 trong SRS).
- Người dùng tự thêm tay qua màn hình quản lý blocklist (FR-5 trong SRS).

## Decision

Blocklist của SafeCall Phase 1 chỉ lưu cục bộ trên thiết bị người dùng (local-only). Không có backend, không đồng bộ, không chia sẻ dữ liệu blocklist giữa các người dùng, không đa thiết bị. Dữ liệu blocklist chỉ được tạo ra từ: (1) xác nhận thủ công của người dùng sau cuộc gọi đến đã kết nối từ số lạ, hoặc (2) người dùng tự thêm tay qua màn hình quản lý blocklist.

## Consequences

**Tích cực:**
- Không cần backend/API ở Phase 1, giảm đáng kể effort phát triển và chi phí vận hành.
- Không phải xử lý vấn đề abuse/report ảo — vốn là rủi ro cố hữu của mô hình blocklist cộng đồng.
- Không cần xử lý các vấn đề privacy liên quan tới việc truyền số điện thoại của bên thứ ba (người gọi) ra ngoài thiết bị, vì không có truyền dữ liệu qua mạng.

**Tiêu cực / trade-off:**
- Không có network effect: mỗi người dùng tự tích lũy blocklist riêng từ đầu, không hưởng lợi từ việc một số lừa đảo đã bị hàng trăm người dùng khác xác nhận — mỗi máy phải "học lại" độc lập.
- Giá trị bảo vệ ban đầu (trước khi người dùng tự tích lũy đủ dữ liệu cá nhân) thấp hơn hẳn so với mô hình cộng đồng.
- Đây là điểm mở rộng rõ ràng đã được xác định cho một phase sau (community blocklist), hiện chưa thuộc phạm vi Phase 1 — cần quyết định kiến trúc riêng (backend, đồng bộ, chống abuse) khi được triển khai.

## Cập nhật (sau khi bổ sung BLOCK-04 — Export/Import blocklist)

**Bối cảnh bổ sung:** Sau khi ADR này được Accepted, yêu cầu được bổ sung thêm BLOCK-04 (`docs/requirements/scam-call-blocking.md`, Quyết định đã chốt #8 và Ràng buộc phi chức năng sơ bộ): người dùng có thể export toàn bộ blocklist ra file JSON và chia sẻ qua Android Share Sheet chuẩn (email, Drive, Zalo, Bluetooth, hoặc bất kỳ ứng dụng nào người dùng tự chọn trong Share Sheet); import đọc lại file JSON và merge vào blocklist hiện tại.

**Không thay đổi:** Decision gốc của ADR-003 vẫn giữ nguyên — Phase 1 vẫn **local-only**, không có backend, không có server trung tâm nào thu thập/lưu trữ/đồng bộ blocklist, không chia sẻ dữ liệu tự động giữa người dùng hoặc giữa thiết bị. BLOCK-04 không thêm bất kỳ thành phần server/API nào vào hệ thống.

**Cần cập nhật lại một phần lý do trong Consequences gốc:** Lý do "Không cần xử lý các vấn đề privacy liên quan tới việc truyền số điện thoại của bên thứ ba ra ngoài thiết bị, vì không có truyền dữ liệu qua mạng" (mục Consequences — Tích cực, dòng 3) **không còn hoàn toàn đúng** sau BLOCK-04. Khi người dùng chủ động chọn Export, số điện thoại của bên thứ ba (người gọi đã bị chặn) được đóng gói vào một file và có thể được truyền ra ngoài thiết bị qua kênh người dùng tự chọn trong Share Sheet.

Tuy nhiên, điều này **không mâu thuẫn với tinh thần "local-only"** của ADR-003, vì:
- Export là hành động **thủ công, một lần (one-shot), do người dùng tự chủ động kích hoạt** mỗi lần cần — không có cơ chế tự động/định kỳ nào do SafeCall khởi tạo (không có backup tự động, không có đồng bộ nền).
- Không có server trung tâm nào của SafeCall thu thập hoặc lưu trữ bản sao dữ liệu này — SafeCall chỉ tạo file cục bộ trên thiết bị rồi trao quyền chọn kênh chia sẻ hoàn toàn cho người dùng thông qua Share Sheet chuẩn của Android, giống hệt việc chia sẻ bất kỳ file nào khác từ thiết bị.
- Do đó Decision "không backend/không đồng bộ" của ADR-003 vẫn đứng vững; đây là một **giới hạn mới của Consequences**, không phải một thay đổi Decision.

**Rủi ro mới cần ghi nhận (Phase 1 chấp nhận, chưa giải quyết):**
- File export **không được mã hóa** ở Phase 1. Nếu file lưu lại hoặc bị lộ qua kênh chia sẻ không an toàn (ví dụ gửi email không mã hóa, lưu Drive public, qua Bluetooth cho thiết bị lạ), số điện thoại của bên thứ ba trong file đó có thể bị đọc bởi bất kỳ ai có được file.
- Người dùng **tự chịu trách nhiệm** về việc chọn kênh chia sẻ an toàn hay không — SafeCall không kiểm soát, không cảnh báo riêng về độ an toàn của từng kênh trong Share Sheet (đây là hành vi chuẩn của hệ điều hành, ngoài khả năng can thiệp của app).
- Rủi ro này là **nợ rủi ro đã biết, được chấp nhận cho Phase 1** theo đúng yêu cầu "không yêu cầu mã hóa file export ở Phase 1" (Ràng buộc phi chức năng sơ bộ). Việc có mã hóa file export ở phase sau hay không là quyết định riêng, chưa được đặt ra trong ADR này.

**Ngoài phạm vi ADR này (cascade cần cập nhật):** Chi tiết thiết kế JSON schema, cơ chế merge, xử lý file corrupt/version không tương thích của BLOCK-04 không thuộc phạm vi ADR-003 — thuộc `low-level-designer`/`test-plan-writer` theo cảnh báo cascade đã nêu trong `docs/requirements/scam-call-blocking.md` (mục Bước tiếp theo).
