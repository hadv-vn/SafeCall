# Software Requirements Specification (SRS)
# SafeCall — Chặn cuộc gọi lừa đảo (Android, Phase 1)

| | |
|---|---|
| **Phiên bản** | 3.0 |
| **Ngày** | 2026-10-21 |
| **Trạng thái** | Chốt — dựa trên yêu cầu đã được làm rõ |
| **Nguồn input** | `docs/requirements/scam-call-blocking.md` |

### Lịch sử phiên bản

| Phiên bản | Ngày | Thay đổi | Lý do |
|---|---|---|---|
| 1.0 | 2026-10-20 | Bản SRS gốc — FR-1..FR-7, NFR-1..NFR-7, phạm vi, giao diện ngoài. | Cấu trúc hóa yêu cầu đã làm rõ ban đầu từ `docs/requirements/scam-call-blocking.md`. |
| 2.0 | 2026-10-21 | Bổ sung **FR-8** (SCAM-DETECT-05 — xác nhận lừa đảo ngay lúc đang đổ chuông, kèm ngắt cuộc gọi hiện tại ngay khi xác nhận "Có"); bổ sung thuật ngữ "Overlay lúc đổ chuông" (1.3); bổ sung quyền mới `ANSWER_PHONE_CALLS` vào NFR-2 và Phần 6 (Giao diện ngoài); mở rộng NFR-3 và NFR-4 để phản ánh rủi ro không thể hoàn tác và ràng buộc độ tin cậy overlay lúc đổ chuông; cập nhật Phạm vi (5.1) và Phụ lục (7.1) với các giả định/câu hỏi mở riêng cho SCAM-DETECT-05. FR-1..FR-7 giữ nguyên không thay đổi. | Story mới SCAM-DETECT-05 được thêm vào yêu cầu nghiệp vụ nguồn SAU KHI SRS v1.0 đã được viết — cần cập nhật để SRS phản ánh đúng, đầy đủ yêu cầu nguồn hiện tại. |
| 3.0 | 2026-10-21 | **CẬP NHẬT FR-1** (BLOCK-02) để phản ánh quy tắc khớp blocklist mới — khớp CẢ exact (số cụ thể) VÀ prefix (đầu số), chuẩn hóa qua `PhoneNumberNormalizer`; **CẬP NHẬT FR-6** (CALLHISTORY-01) để phân biệt rõ nguồn chặn (exact/prefix) trong lịch sử cuộc gọi; thêm **FR-9** (BLOCK-03 — chặn theo đầu số/prefix) và **FR-10** (BLOCK-04 — Export/Import blocklist JSON, cơ chế merge); bổ sung thuật ngữ "Đầu số (prefix)", "Schema version", "Merge" (1.3); đồng bộ lại Phần 5 (Phạm vi) — chuyển Export/Import từ "Ngoài phạm vi" sang "Trong phạm vi", bổ sung chặn theo đầu số vào trong phạm vi (để khắc phục mâu thuẫn nội tại với FR-9/FR-10 mới); bổ sung Android Share Sheet vào Phần 6 (Giao diện ngoài); mở rộng NFR-1 (điểm nhạy cảm privacy mới khi export), NFR-3 (rủi ro chặn nhầm bị khuếch đại bởi prefix ngắn) và NFR-4 (hiệu năng tra cứu matching exact+prefix khi blocklist lớn); cập nhật Phụ lục (7.3) về tình trạng cascade các tài liệu liên quan. FR-2..FR-5, FR-7, FR-8 giữ nguyên không thay đổi. | BLOCK-03 (chặn theo đầu số) và BLOCK-04 (export/import blocklist JSON) được thêm vào yêu cầu nghiệp vụ nguồn (`docs/requirements/scam-call-blocking.md`) SAU KHI SRS v2.0 đã được viết — cần cập nhật để SRS phản ánh đúng, đầy đủ yêu cầu nguồn hiện tại. |

---

## 1. Giới thiệu

### 1.1 Mục đích tài liệu
Tài liệu này mô tả đầy đủ yêu cầu phần mềm cho SafeCall Phase 1 — ứng dụng Android chặn cuộc gọi lừa đảo dựa trên blocklist cục bộ (local-only). Tài liệu được cấu trúc hóa theo tinh thần IEEE 830 (rút gọn) từ yêu cầu nghiệp vụ đã được làm rõ, dùng làm input chính thức cho các giai đoạn thiết kế tiếp theo (`system-architect`, `high-level-designer`, `low-level-designer`, `data-modeler`, `ui-ux-designer`, `test-plan-writer`).

### 1.2 Phạm vi tài liệu
Tài liệu áp dụng cho toàn bộ tính năng Phase 1 của SafeCall trên Android: chặn real-time, phát hiện và xác nhận cuộc gọi lừa đảo sau khi kết thúc, quản lý blocklist thủ công, lịch sử cuộc gọi, và onboarding/cấp quyền. Chi tiết phạm vi trong/ngoài xem Phần 5.

### 1.3 Định nghĩa, thuật ngữ, từ viết tắt
| Thuật ngữ | Giải thích |
|---|---|
| Blocklist | Danh sách số điện thoại bị chặn, lưu cục bộ trên thiết bị (local-only, không backend, không chia sẻ giữa người dùng). |
| Prompt xác nhận | Hộp thoại hỏi người dùng "đây có phải cuộc gọi lừa đảo không" sau khi một cuộc gọi đến đã kết nối kết thúc. |
| Contact | Số điện thoại đã được lưu trong danh bạ (contacts) của thiết bị. |
| Số lạ | Số điện thoại gọi đến không khớp với bất kỳ contact nào đã lưu (hoặc tất cả số đều bị coi là "lạ" nếu người dùng từ chối quyền danh bạ — xem ONBOARD-01). |
| Vai trò chặn gọi | Quyền/role hệ thống Android cho phép app chặn cuộc gọi đến (call screening role). |
| Cuộc gọi nhỡ | Cuộc gọi đến không được trả lời (missed call). |
| Local-only | Toàn bộ dữ liệu (blocklist, lịch sử) chỉ lưu trên thiết bị, không đồng bộ/chia sẻ qua backend hay giữa các người dùng. |
| FR / NFR | Functional Requirement / Non-Functional Requirement. |
| AC | Acceptance Criteria — tiêu chí nghiệm thu, dạng Given/When/Then. |
| Overlay lúc đổ chuông *(mới — v2.0)* | Lớp giao diện hiển thị đè lên màn hình hiện tại khi có cuộc gọi đến đang đổ chuông (chưa được trả lời), cho phép người dùng xác nhận ngay "đây có phải lừa đảo không" trước khi cuộc gọi được trả lời hoặc kết thúc. Khác với "Prompt xác nhận" (hiển thị SAU khi cuộc gọi đã kết nối và kết thúc) — hai cơ chế song song, không thay thế nhau (xem FR-8). |
| `ANSWER_PHONE_CALLS` *(mới — v2.0)* | Quyền hệ thống Android, dùng để chủ động kết thúc/từ chối một cuộc gọi đang đổ chuông (qua `TelecomManager.endCall()`), phục vụ hành vi ngắt cuộc gọi ngay khi người dùng xác nhận "Có, đây là lừa đảo" trong FR-8. Quyền này KHÁC với vai trò chặn gọi (Call Screening Role) đã có — vai trò chặn gọi dùng để chặn cuộc gọi TRƯỚC khi đổ chuông, còn `ANSWER_PHONE_CALLS` dùng để ngắt cuộc gọi ĐANG đổ chuông. |
| Đầu số (prefix) *(mới — v3.0)* | Một chuỗi tiền tố số (toàn chữ số, có thể có dấu "+" ở đầu, tối thiểu 4 ký tự số không tính "+"/mã quốc gia) được thêm vào blocklist dưới dạng entry loại "đầu số". Một số gọi đến bị chặn theo quy tắc prefix nếu số đó BẮT ĐẦU BẰNG đầu số đã chặn, sau khi cả hai được chuẩn hóa theo cùng quy tắc của `PhoneNumberNormalizer`. Khác với entry "số cụ thể" (exact match) — một blocklist có thể chứa đồng thời cả hai loại entry (xem FR-1, FR-9). |
| Schema version *(mới — v3.0)* | Trường `schema_version` trong file JSON export của blocklist (FR-10), dùng để kiểm tra tương thích khi import: app từ chối import file có `schema_version` MỚI HƠN version hiện tại hỗ trợ (từ chối an toàn); tự xử lý tương thích xuôi cho file có `schema_version` CŨ HƠN nhưng còn được hỗ trợ. |
| Merge *(mới — v3.0)* | Cơ chế hợp nhất dữ liệu dùng khi import blocklist (FR-10): các entry trong file import được thêm vào blocklist hiện tại; entry đã tồn tại (khớp chính xác theo loại + giá trị đã chuẩn hóa) bị bỏ qua; KHÔNG entry nào trong blocklist hiện tại bị xóa hoặc ghi đè — khác với "thay thế" (replace) hoàn toàn. |

### 1.4 Tài liệu tham chiếu
- `docs/requirements/scam-call-blocking.md` — yêu cầu nghiệp vụ đã làm rõ (nguồn duy nhất của toàn bộ nội dung SRS này).

---

## 2. Mô tả tổng quan

### 2.1 Bối cảnh nghiệp vụ
Người dùng muốn tự bảo vệ mình khỏi bị làm phiền/lừa đảo lặp lại bởi các số mà **chính họ** đã từng nhận diện là lừa đảo, mà không cần nhớ hoặc tự tay chặn từng số — hệ thống tự "học" dần theo trải nghiệm cá nhân của người dùng đó. SafeCall Phase 1 là một MVP cá nhân hóa (không cộng đồng, không chia sẻ dữ liệu giữa người dùng).

### 2.2 Đối tượng dùng / Persona
- Người dùng Android đã cài đặt SafeCall và cấp đủ quyền cần thiết (vai trò chặn gọi, trạng thái cuộc gọi, thông báo, danh bạ).
- Không có vai trò quản trị/vận hành khác (không có backend, không có đội vận hành dữ liệu cộng đồng) — mỗi người dùng chỉ quản lý dữ liệu của riêng mình trên thiết bị của họ.

### 2.3 Giả định & ràng buộc (lấy từ input, chưa xác nhận riêng với stakeholder ở mức chi tiết số liệu)
- Ngưỡng thời lượng cuộc gọi tối thiểu để kích hoạt prompt xác nhận: **3 giây**. Đây là giá trị mặc định hợp lý được áp dụng để không chặn tiến độ, có thể điều chỉnh — xem câu hỏi mở ở Phần 7.
- Không có push notification chủ động khi một quyền bị thu hồi — chỉ kiểm tra lại khi app được mở lên foreground.
- Không có màn hình riêng để quản lý danh sách số "đã xác nhận Không phải lừa đảo" — nếu người dùng đổi ý, dùng lại luồng thêm tay vào blocklist (BLOCK-01/FR-5).
- Toàn bộ dữ liệu là local-only: không backend, không đồng bộ/chia sẻ giữa người dùng, không đa thiết bị.

---

## 3. Yêu cầu chức năng

Mỗi nhóm yêu cầu dưới đây giữ nguyên mã gốc (BLOCK-02, SCAM-DETECT-01...) từ tài liệu nguồn để tiện tham chiếu, đồng thời gán mã FR để trích dẫn trong các tài liệu thiết kế sau.

### FR-1 — Chặn real-time số/đầu số đã có trong blocklist *(nguồn: BLOCK-02 — cập nhật v3.0, bổ sung quy tắc khớp đầu số)*
As a người dùng Android đã cài SafeCall và cấp đủ quyền, I want cuộc gọi từ số khớp với blocklist (dù là khớp chính xác số cụ thể hay khớp đầu số) bị tự động chặn ngay khi gọi đến, so that tôi không bị làm phiền lại bởi số hoặc dải số tôi đã biết là lừa đảo.

- **AC1**: Given số gọi đến khớp CHÍNH XÁC (exact match, đã chuẩn hóa định dạng) với một entry "số cụ thể", When cuộc gọi đến, Then cuộc gọi bị chặn tự động, không đổ chuông.
- **AC2** *(mới — v3.0)*: Given số gọi đến BẮT ĐẦU BẰNG (prefix match, sau khi chuẩn hóa — cả số gọi đến và đầu số đã lưu được chuẩn hóa theo CÙNG quy tắc của `PhoneNumberNormalizer` trước khi so sánh, để tránh lệch giữa dạng nội địa "0912..." và dạng quốc tế "+84912...") một entry "đầu số" (xem FR-9), When cuộc gọi đến, Then cuộc gọi bị chặn tự động, không đổ chuông.
- **AC3** *(mới — v3.0)*: Given số gọi đến khớp CẢ HAI loại, When cuộc gọi đến, Then cuộc gọi vẫn bị chặn — khớp MỘT TRONG HAI loại là đủ điều kiện.
- **AC4** *(mới — v3.0)*: Given số gọi đến KHÔNG khớp exact VÀ KHÔNG khớp prefix với bất kỳ entry nào, When cuộc gọi đến, Then KHÔNG bị chặn bởi blocklist (vẫn có thể được xử lý bởi FR-2/FR-8).
- **AC5**: Given SafeCall chưa được cấp quyền/vai trò chặn gọi, When có cuộc gọi khớp exact hoặc prefix, Then cuộc gọi KHÔNG bị chặn và app cảnh báo rõ quyền chưa được cấp.
- **AC6**: Given số gọi đến vừa là contact vừa khớp EXACT với một entry số cụ thể (do người dùng tự chặn người quen — xem FR-5), When cuộc gọi đến, Then blocklist được ưu tiên — cuộc gọi vẫn bị chặn (người dùng đã chủ động xác nhận muốn chặn người này).
- **AC7** *(mới — v3.0)*: Given số gọi đến vừa là contact vừa khớp PREFIX với một đầu số, When cuộc gọi đến, Then cuộc gọi vẫn bị chặn theo quy tắc prefix — vì FR-9 (BLOCK-03) đã bắt buộc cảnh báo trước khi thêm đầu số nếu đầu số đó khớp với bất kỳ contact nào, người dùng coi như đã chủ động xác nhận rủi ro này từ lúc thêm.

### FR-2 — Phát hiện cuộc gọi đến kết thúc và hỏi xác nhận *(nguồn: SCAM-DETECT-01)*
As a người dùng nhận cuộc gọi từ số lạ, I want được hỏi "đây có phải lừa đảo không" ngay sau khi cuộc gọi kết thúc, so that tôi đánh giá được ngay khi còn nhớ rõ nội dung.

- **AC1**: Given cuộc gọi ĐẾN đã kết nối (được trả lời) từ số không phải contact, chưa có trong blocklist, chưa từng bị đánh dấu "Không" (vĩnh viễn), When cuộc gọi kết thúc, Then app hiển thị prompt xác nhận.
- **AC2**: Given cuộc gọi là cuộc gọi ĐI hoặc cuộc gọi NHỠ (không trả lời), When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt (quyết định đã chốt).
- **AC3**: Given số là contact đã lưu, When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.
- **AC4**: Given số đã có trong blocklist, When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt (đã có quyết định rồi).
- **AC5**: Given số đã từng được xác nhận "Không phải lừa đảo" (vĩnh viễn), When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.
- **AC6**: Given thời lượng cuộc gọi dưới 3 giây *(giả định/mặc định — xem Phần 2.3 và Phần 7)*, When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt (coi là cuộc gọi quá ngắn để đánh giá).

### FR-3 — Xác nhận "Có" → thêm vào blocklist *(nguồn: SCAM-DETECT-02)*
- **AC1**: Given prompt đang hiển thị cho số X, When người dùng chọn "Có", Then số X được thêm vào blocklist ngay lập tức, xuất hiện trong màn hình quản lý blocklist (FR-5), và bị chặn tự động từ lần gọi tiếp theo (FR-1).
- **AC2**: Given số X đã có sẵn trong blocklist do lỗi đồng bộ trạng thái, When người dùng chọn "Có", Then hệ thống không tạo bản ghi trùng lặp (ràng buộc UNIQUE theo số đã chuẩn hóa).

### FR-4 — Xác nhận "Không" / bỏ qua *(nguồn: SCAM-DETECT-03)*
- **AC1**: Given prompt đang hiển thị cho số X, When người dùng chọn "Không" rõ ràng, Then số X được ghi nhận "đã xác nhận không phải lừa đảo" vĩnh viễn và KHÔNG hỏi lại cho số đó nữa.
- **AC2**: Given người dùng không tương tác với prompt (để tự đóng/hết hạn), When cuộc gọi tiếp theo từ cùng số X kết thúc (vẫn thỏa các điều kiện khác), Then prompt được hiển thị lại (coi "bỏ qua" là tạm thời, khác với "Không").
- **AC3**: Given số X đã được đánh dấu "Không phải lừa đảo" vĩnh viễn nhưng người dùng đổi ý, When người dùng muốn chặn số đó, Then người dùng có thể tự thêm tay vào blocklist qua FR-5 (không có luồng "gỡ đánh dấu an toàn" riêng).

### FR-5 — Quản lý blocklist thủ công *(nguồn: BLOCK-01)*
- **AC1**: Given tôi mở màn hình quản lý blocklist, When tải xong, Then thấy đầy đủ số đang bị chặn (không phân biệt nguồn thêm tự động hay thủ công).
- **AC2**: Given blocklist rỗng, When mở màn hình, Then hiển thị trạng thái rỗng rõ ràng, không phải màn hình trắng/lỗi.
- **AC3**: Given tôi nhập số hợp lệ để thêm, When xác nhận, Then số đó vào blocklist và bị chặn real-time từ thời điểm đó (FR-1).
- **AC4**: Given tôi nhập số không hợp lệ (sai định dạng/trống), When xác nhận, Then báo lỗi định dạng, không thêm.
- **AC5**: Given tôi nhập số đã có trong blocklist, When xác nhận, Then báo "đã tồn tại", không tạo bản ghi trùng.
- **AC6**: Given tôi chọn xóa một số, When xác nhận xóa, Then số đó bị xóa và không còn bị chặn từ thời điểm đó.
- **AC7**: Given tôi bắt đầu xóa nhưng hủy trước khi xác nhận, When hủy, Then số đó vẫn còn trong blocklist.
- **AC8**: Given tôi thêm tay một số đang là contact vào blocklist, When tôi bắt đầu thêm, Then app hiển thị cảnh báo rõ ràng ("Số này có trong danh bạ của bạn — bạn có chắc muốn chặn?") trước khi cho xác nhận thêm (quyết định đã chốt).

### FR-6 — Lịch sử cuộc gọi *(nguồn: CALLHISTORY-01 — cập nhật v3.0, phân biệt nguồn chặn exact/prefix)*
- **AC1** *(cập nhật v3.0)*: Given có cuộc gọi đến (bị chặn hoặc không), When cuộc gọi kết thúc, Then một bản ghi lịch sử được lưu (số, thời gian, bị chặn hay không — **nếu bị chặn bởi blocklist thì ghi rõ chặn do khớp exact hay khớp prefix theo đầu số nào**, có được hỏi xác nhận hay không, phản hồi của người dùng nếu có).
- **AC2** *(cập nhật v3.0)*: Given tôi mở màn hình lịch sử cuộc gọi, When tải xong, Then thấy danh sách các cuộc gọi gần đây kèm trạng thái (**đã chặn lúc đổ chuông / đã chặn real-time theo số cụ thể / đã chặn real-time theo đầu số** / đã hỏi xác nhận sau cuộc gọi / bình thường).
- **AC3** *(mới — v3.0)*: Given một đầu số mới được thêm vào blocklist, When xem lại các bản ghi lịch sử đã lưu TRƯỚC thời điểm thêm đầu số đó, Then các bản ghi đó KHÔNG bị áp dụng hồi tố theo quy tắc prefix mới — quy tắc chỉ ảnh hưởng tới cuộc gọi đến SAU thời điểm thêm (mặc định, xem Phần 7).

### FR-7 — Onboarding và cấp quyền *(nguồn: ONBOARD-01)*
- **AC1**: Given người dùng mở app lần đầu, When onboarding bắt đầu, Then app giải thích rõ lý do cần từng quyền (vai trò chặn gọi, trạng thái cuộc gọi, thông báo, danh bạ) TRƯỚC khi mở dialog hệ thống.
- **AC2**: Given người dùng từ chối một quyền bắt buộc, When từ chối, Then app hiển thị rõ chức năng nào sẽ không hoạt động và cho phép cấp lại sau, không ép đóng app.
- **AC3**: Given đã cấp đủ quyền bắt buộc, When mở lại app sau này, Then không hiển thị lại toàn bộ luồng onboarding.
- **AC4**: Given một quyền bắt buộc bị thu hồi sau khi đã cấp, When người dùng mở lại app (foreground), Then app phát hiện và hướng dẫn cấp lại. *(Không yêu cầu push notification chủ động khi quyền bị thu hồi trong Phase 1 — giả định/mặc định, xem Phần 2.3.)*
- **AC5**: Given người dùng từ chối quyền truy cập danh bạ, When từ chối, Then app vẫn hoạt động nhưng TẤT CẢ số gọi đến (kể cả người quen) được coi là "số lạ" — app hiển thị banner giải thích rõ hệ quả này và cho phép cấp quyền lại bất kỳ lúc nào. *(Giả định/mặc định — tính năng suy giảm nhẹ nhàng, không chặn sử dụng app.)*

### FR-8 — Xác nhận lừa đảo ngay lúc đang đổ chuông *(nguồn: SCAM-DETECT-05 — mới bổ sung ở v2.0)*
As a người dùng nhận cuộc gọi từ số lạ đang đổ chuông, I want được xác nhận ngay "đây có phải lừa đảo không" trong khi cuộc gọi còn đang đổ chuông, so that tôi có thể chặn cuộc gọi đó ngay lập tức và các lần gọi sau, mà không phải chờ đến khi cuộc gọi kết thúc.

- **AC1**: Given cuộc gọi đến từ số không phải contact, chưa có trong blocklist, chưa từng được xác nhận "Không phải lừa đảo" (vĩnh viễn), When cuộc gọi đang đổ chuông (chưa được trả lời), Then app hiển thị overlay/prompt xác nhận "đây có phải lừa đảo không" ngay trong lúc đổ chuông — đây là tùy chọn THÊM, song song và không thay thế luồng hỏi sau cuộc gọi (FR-2).
- **AC2**: Given số gọi đến là contact đã lưu, When cuộc gọi đang đổ chuông, Then KHÔNG hiển thị overlay lúc đổ chuông (nhất quán với điều kiện loại trừ của FR-2/AC3).
- **AC3**: Given số gọi đến đã có trong blocklist, When cuộc gọi đến, Then KHÔNG hiển thị overlay lúc đổ chuông — cuộc gọi đã bị chặn real-time (FR-1), không đổ chuông nên không áp dụng luồng này.
- **AC4**: Given số gọi đến đã từng được xác nhận "Không phải lừa đảo" (vĩnh viễn), When cuộc gọi đang đổ chuông, Then KHÔNG hiển thị overlay lúc đổ chuông.
- **AC5**: Given overlay đang hiển thị lúc đổ chuông cho số X, When người dùng chọn "Có, đây là lừa đảo", Then (a) số X được thêm vào blocklist ngay lập tức, xuất hiện trong màn hình quản lý blocklist (FR-5), bị chặn tự động từ lần gọi tiếp theo (FR-1); VÀ (b) cuộc gọi ĐANG đổ chuông đó bị ngắt/từ chối ngay lập tức — người dùng không cần tự tay từ chối cuộc gọi nữa. **Hành động ngắt cuộc gọi này là KHÔNG THỂ HOÀN TÁC** (xem NFR-3).
- **AC6**: Given overlay đang hiển thị lúc đổ chuông cho số X, When người dùng chọn "Không phải lừa đảo", Then số X được ghi nhận "đã xác nhận không phải lừa đảo" vĩnh viễn, KHÔNG hỏi lại cho số đó ở cả hai luồng (FR-2 và FR-8), và cuộc gọi tiếp tục đổ chuông bình thường (không bị ảnh hưởng).
- **AC7**: Given overlay đang hiển thị lúc đổ chuông cho số X, When cuộc gọi được trả lời hoặc ngừng đổ chuông TRƯỚC KHI người dùng tương tác với overlay, Then overlay tự đóng mà không ghi nhận phản hồi nào cho số X (default/giả định — xem Phần 7.1); luồng hỏi xác nhận sau cuộc gọi (FR-2) vẫn áp dụng bình thường nếu cuộc gọi đã được trả lời và thỏa các điều kiện hiện có.
- **AC8**: Given người dùng không tương tác với overlay và cuộc gọi tự kết thúc do không ai trả lời (missed), When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt nào sau đó cho cuộc gọi nhỡ này (nhất quán với FR-2/AC2); số X vẫn ở trạng thái "chưa xác định" và có thể được hỏi lại (cả hai luồng) ở lần gọi tiếp theo.
- **AC9**: Given overlay lúc đổ chuông cho số X đã từng bị bỏ qua/không tương tác (coi là tạm thời, nhất quán với FR-4/AC2), When số X gọi lại lần sau và vẫn đang đổ chuông, Then overlay được hiển thị lại.
- **AC10**: Given thiết bị/app chưa được cấp quyền cần thiết để hiển thị overlay hoặc để ngắt cuộc gọi đang đổ chuông (`ANSWER_PHONE_CALLS`), When có cuộc gọi đến từ số lạ đang đổ chuông, Then overlay KHÔNG hiển thị (hoặc hiển thị nhưng không thể ngắt cuộc gọi — xem giả định Phần 7.1), cuộc gọi vẫn đổ chuông bình thường, và luồng hỏi sau cuộc gọi (FR-2) vẫn là cách duy nhất để xác nhận cho số đó.

### FR-9 — Chặn theo đầu số (prefix blocking) *(nguồn: BLOCK-03 — mới bổ sung ở v3.0)*
As a người dùng quản lý blocklist, I want thêm một đầu số (chuỗi tiền tố) vào danh sách chặn, so that tôi chặn được toàn bộ dải số bắt đầu bằng đầu số đó mà không phải tự thêm từng số cụ thể một.

- **AC1**: Given tôi chọn thêm theo loại "Đầu số" (khác với "Số cụ thể" ở FR-5), When tôi nhập một chuỗi đầu số hợp lệ (toàn chữ số, có thể có dấu "+" ở đầu, độ dài tối thiểu **4 ký tự số** không tính dấu "+"/mã quốc gia — mặc định đã áp dụng, điều chỉnh được sau nếu cần) và xác nhận, Then đầu số đó được thêm vào blocklist dưới dạng entry loại "đầu số", được chuẩn hóa theo cùng quy tắc `PhoneNumberNormalizer` dùng cho số cụ thể (để so khớp nhất quán ở FR-1), và áp dụng chặn real-time từ lần gọi tiếp theo.
- **AC2**: Given tôi nhập đầu số ngắn hơn 4 ký tự số, When xác nhận, Then báo lỗi rõ ràng ("Đầu số quá ngắn, có thể chặn nhầm nhiều số không liên quan") và không thêm.
- **AC3**: Given tôi nhập chuỗi không hợp lệ (ký tự không phải số ngoài dấu "+" ở đầu, hoặc trống), When xác nhận, Then báo lỗi định dạng, không thêm.
- **AC4**: Given đầu số tôi nhập đã tồn tại chính xác trong danh sách đầu số đang chặn, When xác nhận, Then báo "đã tồn tại", không tạo bản ghi trùng.
- **AC5**: Given đầu số tôi nhập khớp (bằng quy tắc prefix) với số điện thoại của một hoặc nhiều contact đã lưu, When tôi xác nhận thêm, Then app hiển thị cảnh báo rõ ràng (tương tự FR-5/AC8 — ví dụ "Đầu số này khớp với N liên hệ trong danh bạ của bạn, các cuộc gọi từ họ cũng sẽ bị chặn — bạn có chắc muốn tiếp tục?") trước khi cho xác nhận thêm.
- **AC6**: Given tôi mở màn hình quản lý blocklist, When tải xong, Then danh sách hiển thị cả entry "số cụ thể" và entry "đầu số", có nhãn trực quan phân biệt rõ hai loại (ví dụ nhãn "Đầu số" kèm mỗi entry loại này).
- **AC7**: Given tôi chọn xóa một entry đầu số, When xác nhận xóa, Then entry đó bị xóa — các số thuộc dải đó không còn bị chặn theo quy tắc prefix này (trừ khi vẫn khớp exact với một entry số cụ thể khác).
- **AC8**: Given tôi bắt đầu xóa một entry đầu số nhưng hủy trước khi xác nhận, When hủy, Then entry đó vẫn còn trong blocklist.

### FR-10 — Export/Import blocklist *(nguồn: BLOCK-04 — mới bổ sung ở v3.0)*
As a người dùng muốn lưu dự phòng hoặc chuyển blocklist cá nhân sang thiết bị khác, I want xuất toàn bộ blocklist ra file và nhập lại file đó vào blocklist hiện tại, so that tôi không phải tự thêm lại từng số/đầu số bằng tay khi đổi điện thoại hoặc muốn giữ bản sao dự phòng.

**Export:**
- **AC1**: Given blocklist có ít nhất một entry (số cụ thể và/hoặc đầu số), When tôi chọn "Export blocklist", Then app tạo một file JSON chứa toàn bộ entry hiện có (gồm cả loại, phân biệt rõ trong dữ liệu, kèm trường `schema_version` để hỗ trợ kiểm tra tương thích khi import sau này) và mở Android Share Sheet chuẩn để tôi chọn ứng dụng chia sẻ.
- **AC2**: Given blocklist đang rỗng, When tôi mở màn hình quản lý blocklist, Then nút "Export" ở trạng thái disabled kèm chú thích ngắn ("Chưa có số nào để xuất").
- **AC3**: Given tôi chọn "Export" rồi hủy ngay trong Share Sheet, When hủy, Then blocklist hiện tại không thay đổi gì.

**Import:**
- **AC4**: Given tôi chọn "Import blocklist" và chọn một file JSON đúng định dạng export của SafeCall (cùng `schema_version` hoặc version thấp hơn tương thích), When import, Then các entry trong file được MERGE vào blocklist hiện tại — entry đã tồn tại (khớp chính xác theo loại + giá trị đã chuẩn hóa) bị bỏ qua, entry mới được thêm, KHÔNG entry nào trong blocklist hiện tại bị xóa/ghi đè.
- **AC5**: Given file import có N entry hợp lệ mới và M entry trùng, When import hoàn tất, Then app hiển thị kết quả rõ ràng ("Đã thêm N số mới, bỏ qua M số đã có").
- **AC6**: Given file không phải JSON hợp lệ hoặc không đúng cấu trúc blocklist của SafeCall, When import, Then app báo lỗi rõ ràng ("File không hợp lệ hoặc không đúng định dạng") và blocklist hiện tại KHÔNG bị thay đổi.
- **AC7**: Given file hợp lệ về cấu trúc nhưng chứa một số entry sai định dạng (ví dụ đầu số ngắn hơn 4 ký tự), When import, Then các entry không hợp lệ bị bỏ qua, các entry hợp lệ khác vẫn được merge, và app báo rõ số lượng entry bị bỏ qua vì không hợp lệ.
- **AC8**: Given file import có `schema_version` MỚI HƠN version hiện tại của app hỗ trợ, When import, Then app từ chối import và báo rõ "File được xuất từ phiên bản SafeCall mới hơn, vui lòng cập nhật app" (từ chối an toàn, không cố đọc cấu trúc không rõ).
- **AC9**: Given file import có `schema_version` CŨ HƠN (nhưng vẫn trong danh sách version được hỗ trợ), When import, Then app tự xử lý tương thích xuôi (đọc đúng theo cấu trúc version cũ đó), không báo lỗi.
- **AC10**: Given quá trình import bị gián đoạn giữa chừng (app bị tắt/crash), When người dùng mở lại app, Then blocklist phải ở trạng thái nhất quán — toàn bộ entry hợp lệ của lần import đó đã được thêm, hoặc không entry nào được thêm (cơ chế đảm bảo thuộc thiết kế kỹ thuật, không mô tả ở đây — xem `low-level-designer`).
- **AC11**: Không giới hạn số lượng entry tối đa cho một lần import ở Phase 1 (mặc định — bổ sung giới hạn sau nếu phát sinh vấn đề hiệu năng thực tế).

---

## 4. Yêu cầu phi chức năng

| Mã | Nội dung | Trạng thái |
|---|---|---|
| NFR-1 | Dữ liệu blocklist chứa số điện thoại của bên thứ ba (người gọi) — vẫn là dữ liệu cá nhân cần xử lý cẩn trọng dù lưu local-only: phải được xóa khi gỡ app, được bảo vệ trên thiết bị. **Điểm nhạy cảm mới (FR-10/BLOCK-04 export): khi export, số điện thoại bên thứ ba được CHỦ ĐỘNG truyền ra ngoài thiết bị qua kênh người dùng tự chọn (Android Share Sheet) — khác với giả định ban đầu "không cần xử lý privacy vì không qua mạng". Không yêu cầu mã hóa file export ở Phase 1. Cần rà soát lại ADR-003 (local-only blocklist) ở bước cascade thiết kế.** *(Cập nhật v3.0.)* | Đã xác định (từ input) |
| NFR-2 | Minh bạch với người dùng về lý do cần các quyền nhạy cảm của Android (vai trò chặn gọi, trạng thái điện thoại, danh bạ, thông báo, **và quyền mới `ANSWER_PHONE_CALLS` — dùng để chủ động ngắt cuộc gọi đang đổ chuông qua `TelecomManager.endCall()`, phục vụ FR-8**) — giải thích rõ trước khi xin cấp quyền hệ thống (liên quan FR-7, FR-8). *(Cập nhật v2.0: bổ sung `ANSWER_PHONE_CALLS`.)* | Đã xác định (từ input) |
| NFR-3 | Rủi ro chặn nhầm số quan trọng (ví dụ chặn nhầm người quen) được giảm thiểu bằng cảnh báo rõ ràng khi chặn một contact (FR-5/AC8), nhưng vẫn là rủi ro cố hữu của tính năng tự động — không thể loại bỏ hoàn toàn trong Phase 1. **Riêng với FR-8 (SCAM-DETECT-05), rủi ro này nghiêm trọng hơn: hành động ngắt cuộc gọi đang đổ chuông khi xác nhận "Có" là KHÔNG THỂ HOÀN TÁC — khác với việc thêm số vào blocklist (có thể xóa lại sau nếu nhấn nhầm, xem FR-5/AC6), một cuộc gọi đã bị ngắt thì không thể "nối lại".** *(Cập nhật v2.0.)* **Rủi ro chặn nhầm còn bị KHUẾCH ĐẠI bởi FR-9 (BLOCK-03): một đầu số ngắn có thể chặn nhầm nhiều số không liên quan cùng lúc, rộng hơn rủi ro chặn một số cụ thể — đây là lý do có ngưỡng độ dài tối thiểu (4 ký tự số) và cảnh báo khi khớp contact (FR-9/AC5).** *(Cập nhật v3.0.)* | Đã xác định (từ input) |
| NFR-4 | Độ tin cậy của việc phát hiện thời điểm cuộc gọi kết thúc khi app chạy ở background là một ràng buộc kỹ thuật cần xử lý ở giai đoạn thiết kế (`system-architect`/`low-level-designer`); input không đi sâu vào cơ chế hay số liệu cụ thể (ví dụ tỷ lệ phát hiện thành công, độ trễ). **Tương tự, khả năng hiển thị overlay lúc đổ chuông một cách đáng tin cậy (đè lên ứng dụng khác đang mở, đúng thời điểm, trước khi cuộc gọi được trả lời hoặc hết chuông) cho FR-8 cũng là ràng buộc kỹ thuật cần xử lý ở giai đoạn thiết kế — input không nêu số liệu cụ thể.** *(Cập nhật v2.0.)* **Hiệu năng tra cứu matching (exact + prefix) cho FR-1, đặc biệt khi blocklist lớn (có thể tăng nhanh sau khi import — FR-10), là ràng buộc kỹ thuật cần cấu trúc tra cứu phù hợp (không so khớp tuyến tính) — input không nêu số liệu cụ thể (ví dụ ngưỡng thời gian phản hồi), chỉ nêu đây là yêu cầu định tính cần `system-architect`/`low-level-designer` xử lý.** *(Cập nhật v3.0.)* | Đã xác định ở mức ràng buộc định tính (từ input) — **chi tiết kỹ thuật/số liệu: Chưa xác định — cần bổ sung ở giai đoạn thiết kế** |
| NFR-5 | Hiệu năng (thời gian phản hồi khi chặn cuộc gọi, thời gian tải màn hình blocklist/lịch sử). | Chưa xác định — cần bổ sung |
| NFR-6 | Khả năng mở rộng (ví dụ: số lượng bản ghi blocklist/lịch sử cuộc gọi lớn vẫn hoạt động tốt). | Chưa xác định — cần bổ sung |
| NFR-7 | Tuân thủ quy định/pháp lý cụ thể về dữ liệu cá nhân (ví dụ GDPR, Nghị định bảo vệ dữ liệu cá nhân...). Input chỉ nêu nguyên tắc chung "dữ liệu cá nhân cần xử lý cẩn trọng" (xem NFR-1), không chỉ định khung pháp lý cụ thể. | Chưa xác định — cần bổ sung |

---

## 5. Phạm vi

### 5.1 Trong phạm vi
- Android, Phase 1.
- Chặn real-time dựa trên blocklist cục bộ — **gồm cả khớp chính xác số cụ thể (exact match) VÀ khớp đầu số (prefix match)**. *(Cập nhật v3.0.)*
- Phát hiện cuộc gọi đến đã kết nối kết thúc, hỏi xác nhận cho số lạ (FR-2/FR-3/FR-4).
- **Xác nhận lừa đảo ngay lúc cuộc gọi đang đổ chuông, kèm ngắt cuộc gọi hiện tại ngay khi xác nhận "Có" (FR-8) — tùy chọn thêm bên cạnh luồng hỏi sau cuộc gọi, không thay thế.** *(Mới — v2.0.)*
- Xử lý nhánh "Có" / "Không" / "bỏ qua", áp dụng nhất quán cho cả hai thời điểm xác nhận (sau cuộc gọi và lúc đổ chuông). *(Cập nhật v2.0: mở rộng cho cả hai thời điểm.)*
- Quản lý blocklist thủ công: thêm/xóa số cụ thể (FR-5) **VÀ thêm/xóa đầu số (FR-9)**, hiển thị phân biệt rõ hai loại, gồm cảnh báo khi chặn contact (cả exact và prefix). *(Cập nhật v3.0.)*
- **Export toàn bộ blocklist ra file JSON (kèm `schema_version`), chia sẻ qua Android Share Sheet; Import file JSON theo cơ chế MERGE (FR-10). Thao tác THỦ CÔNG, hoàn toàn cục bộ — KHÔNG mâu thuẫn với ADR-003 (local-only).** *(Mới — v3.0.)*
- Lịch sử cuộc gọi cơ bản (xem lại các cuộc gọi gần đây và trạng thái xử lý), **gồm phân biệt nguồn chặn (exact/prefix) — xem FR-6.** *(Cập nhật v3.0.)*
- Onboarding và cấp quyền cần thiết, **gồm quyền mới `ANSWER_PHONE_CALLS`** *(Mới — v2.0.)*
- Local-only, không backend, không chia sẻ/đồng bộ TỰ ĐỘNG giữa người dùng hoặc giữa thiết bị.

### 5.2 Ngoài phạm vi
- iOS (`CXCallDirectoryExtension`) — phase sau.
- Backend, đồng bộ/chia sẻ blocklist cộng đồng (nhiều người dùng góp chung).
- Nhận diện lừa đảo tự động bằng AI/phân tích nội dung cuộc gọi.
- Số điện thoại ẩn danh/riêng tư (không có số để so khớp, **áp dụng cho cả exact và prefix**) — xác nhận ngoài phạm vi Phase 1.
- Hỏi xác nhận cho cuộc gọi đi hoặc cuộc gọi nhỡ (quyết định đã chốt).
- **Backup cloud TỰ ĐỘNG và đồng bộ đa thiết bị TỰ ĐỘNG — export/import thủ công của FR-10 không phải cơ chế tự động, người dùng phải tự thực hiện mỗi lần.** *(Cập nhật v3.0 — thay cho mục "Import/export blocklist" đã có ở v2.0, nay import/export thủ công đã CHUYỂN vào Trong phạm vi qua FR-10.)*
- Màn hình riêng để quản lý danh sách số "đã xác nhận Không phải lừa đảo" — nếu đổi ý, dùng luồng thêm tay vào blocklist (FR-5).
- **Áp dụng hồi tố quy tắc prefix cho các bản ghi lịch sử cuộc gọi đã lưu TRƯỚC KHI đầu số được thêm (xem FR-6/AC3).** *(Mới — v3.0.)*
- **Giới hạn số lượng entry tối đa khi import (xem FR-10/AC11).** *(Mới — v3.0.)*

---

## 6. Giao diện ngoài

Input không đề cập backend hay API bên ngoài (local-only, không server). Tuy nhiên, input có đề cập rõ việc tích hợp với các dịch vụ/quyền của hệ điều hành Android — được ghi nhận ở đây như giao diện phần mềm/hệ điều hành:

- **Vai trò chặn gọi (Call Screening Role)**: app cần được cấp vai trò hệ thống Android để có thể tự động chặn cuộc gọi đến (FR-1).
- **Quyền trạng thái cuộc gọi (phone state)**: cần để phát hiện cuộc gọi đến/kết nối/kết thúc và phân loại (đến/đi/nhỡ) phục vụ FR-2.
- **Quyền danh bạ (contacts)**: cần để xác định số gọi đến có phải contact đã lưu không (FR-2/AC3, FR-5/AC8). Nếu bị từ chối, toàn bộ số đều coi là "lạ" (FR-7/AC5).
- **Quyền thông báo (notifications)**: cần để hiển thị cảnh báo/prompt liên quan (ONBOARD-01, FR-7).
- **Quyền `ANSWER_PHONE_CALLS`** *(mới — v2.0)*: cần để app chủ động kết thúc/từ chối một cuộc gọi đang đổ chuông thông qua `TelecomManager.endCall()`, phục vụ hành vi ngắt cuộc gọi ngay khi người dùng xác nhận "Có, đây là lừa đảo" lúc đổ chuông (FR-8/AC5). Theo input, tính khả thi kỹ thuật của việc dùng `TelecomManager.endCall()` chỉ với quyền này (không cần giữ vai trò default dialer của thiết bị) **chưa được xác nhận** — cần `system-architect`/`low-level-designer` xác nhận (xem Phần 7.1).
- **Android Share Sheet (chuẩn hệ điều hành)** *(mới — v3.0)*: dùng khi export blocklist (FR-10/AC1) — app tạo file JSON rồi mở Share Sheet chuẩn của Android để người dùng tự chọn ứng dụng chia sẻ/lưu file. SafeCall không tự gửi dữ liệu tới bất kỳ đích cụ thể nào — kênh đích do người dùng chủ động chọn qua Share Sheet. Đây KHÔNG phải backend hay API bên thứ ba do SafeCall kiểm soát, nhưng là điểm dữ liệu cá nhân (số điện thoại bên thứ ba) được chủ động truyền ra ngoài thiết bị — xem NFR-1.

Ngoài các tích hợp hệ điều hành trên, không có tích hợp với backend hay API bên thứ ba nào do SafeCall kiểm soát (không chia sẻ/đồng bộ dữ liệu liên thiết bị/liên người dùng TỰ ĐỘNG) theo input.

---

## 7. Phụ lục

### 7.1 Câu hỏi còn mở / giả định cần xác nhận lại
- **Ngưỡng thời lượng 3 giây (FR-2/AC6)**: đây là giá trị mặc định hợp lý do người làm rõ yêu cầu đề xuất, **chưa được stakeholder xác nhận riêng**. Cần xác nhận lại hoặc điều chỉnh nếu có phản hồi thực tế từ người dùng (ví dụ: nhiều cuộc gọi lừa đảo ngắn hơn 3 giây vẫn cần hỏi xác nhận).
- **Không có push notification khi quyền bị thu hồi (FR-7/AC4)**: hiện chỉ kiểm tra khi app foreground. Đây là giả định đơn giản hóa MVP, cần xác nhận lại có đủ đáp ứng kỳ vọng người dùng không, đặc biệt nếu người dùng ít khi mở lại app.
- **Không có màn hình riêng quản lý danh sách "đã xác nhận Không phải lừa đảo" (FR-4/AC3)**: giả định người dùng ít có nhu cầu đổi ý; cần xác nhận lại nếu phát sinh phản hồi người dùng muốn xem/quản lý danh sách này riêng.
- **Độ tin cậy phát hiện cuộc gọi kết thúc khi app chạy background (NFR-4)**: input xác nhận đây là ràng buộc kỹ thuật cần xử lý ở giai đoạn thiết kế, chưa có số liệu/cơ chế cụ thể — cần `system-architect`/`low-level-designer` làm rõ tiếp.
- **NFR-5, NFR-6, NFR-7 (hiệu năng, khả năng mở rộng, tuân thủ pháp lý cụ thể)**: input không nêu, cần bổ sung nếu có yêu cầu từ stakeholder hoặc luật áp dụng tại thị trường triển khai.
- **Ngưỡng độ dài tối thiểu cho đầu số: 4 ký tự số (FR-9/AC2)** *(mới — v3.0)*: input tự nêu đây là "mặc định đã áp dụng, điều chỉnh được sau nếu cần" — chưa được stakeholder xác nhận riêng bằng số liệu thực tế (ví dụ tỷ lệ chặn nhầm quan sát được).
- **Cơ chế đảm bảo tính nhất quán khi import bị gián đoạn (FR-10/AC10)** *(mới — v3.0)*: input xác nhận đây là "cơ chế đảm bảo thuộc thiết kế kỹ thuật, không mô tả ở đây" — cần `low-level-designer` thiết kế cụ thể (ví dụ transaction/rollback).
- **Không mã hóa file export ở Phase 1 (NFR-1)** *(mới — v3.0)*: input xác nhận đây là quyết định rõ ràng cho Phase 1, nhưng do dữ liệu (số điện thoại bên thứ ba) nay được chủ động truyền ra ngoài thiết bị, cần `system-architect` xác nhận lại mức độ rủi ro này có cần mã hóa ở Phase sau không.

### 7.1.1 Câu hỏi còn mở / giả định cần xác nhận lại riêng cho FR-8 (SCAM-DETECT-05) *(mới — v2.0)*
Các mục dưới đây là giả định/mặc định mà input đã tự nêu rõ là **"để không chặn tiến độ — điều chỉnh lại nếu sai"**, chưa được stakeholder xác nhận riêng:
- Sau khi ngắt cuộc gọi (FR-8/AC5), app hiển thị thông báo ngắn (toast/snackbar) xác nhận hành động (ví dụ "Đã chặn số này") — tránh người dùng hiểu lầm hành động không có tác dụng. Chưa xác nhận riêng.
- Overlay tự đóng ngay khi trạng thái cuộc gọi thay đổi (được trả lời hoặc hết chuông) nếu người dùng chưa tương tác (FR-8/AC7) — không cho xác nhận "trễ" sau khi đã chuyển trạng thái. Chưa xác nhận riêng.
- Overlay cần hiển thị đè lên ứng dụng khác nếu người dùng đang dùng app khác lúc có cuộc gọi đến — input đánh dấu đây là **rủi ro kỹ thuật cần `system-architect` xác nhận khả thi** (có thể cần quyền hiển thị overlay bổ sung ngoài `ANSWER_PHONE_CALLS`).
- Tính khả thi kỹ thuật của việc dùng `TelecomManager.endCall()` chỉ với quyền `ANSWER_PHONE_CALLS` + vai trò `ROLE_CALL_SCREENING` đã có, **không cần** giữ vai trò default dialer của thiết bị — input giả định khả thi nhưng **chưa được `system-architect` xác nhận**. Đây là điều kiện tiên quyết kỹ thuật cho toàn bộ FR-8/AC5.

### 7.2 Mâu thuẫn nội tại phát hiện trong input
Không có mâu thuẫn trong nội dung nghiệp vụ — phần user story/AC, phạm vi, ràng buộc phi chức năng và giả định/mặc định của input (bao gồm cả BLOCK-03 và BLOCK-04 mới ở v3.0) nhất quán với nhau. SRS này phản ánh đúng nguyên vẹn nội dung đã cung cấp trong `docs/requirements/scam-call-blocking.md` tại thời điểm v3.0.

Một điểm cần lưu ý (không phải mâu thuẫn trong input, mà là mâu thuẫn NỘI TẠI trong chính SRS v2.0 đã được phát hiện và sửa khi cập nhật lên v3.0): SRS v2.0 (Phần 5.2) ghi "Import/export blocklist... (hệ quả của 'local-only')" là **Ngoài phạm vi**. Input nguồn nay đã bổ sung BLOCK-04, đưa export/import (thủ công, cục bộ) vào **Trong phạm vi** (FR-10). SRS v3.0 đã đồng bộ lại Phần 5.1/5.2 cho khớp — không còn mâu thuẫn.

Một điểm cần lưu ý khác (tương tự lựa chọn biên tập đã áp dụng ở v2.0 cho FR-7): input nguồn (BLOCK-02/AC7, dòng 30 của file nguồn) có lý giải cho việc blocklist ưu tiên over contact khi khớp prefix bằng cách tham chiếu ngược tới cảnh báo bắt buộc của BLOCK-03 lúc thêm đầu số — nội dung này đã được giữ nguyên trong FR-1/AC7 (tham chiếu chéo tới FR-9) để không làm mất ngữ cảnh lý giải gốc.

### 7.3 Đối chiếu với hệ thống/tài liệu hiện có
Repo hiện đã có các tài liệu thiết kế/kế hoạch khác liên quan đến SafeCall: `docs/design/HLD-SafeCall-Phase1.md`, `docs/design/LLD-SafeCall-Phase1.md`, `docs/design/ERD-SafeCall-Phase1.md`, `docs/design/UIUX-SafeCall-Phase1.md`, `docs/test/TestPlan-SafeCall-Phase1.md`, `docs/planning/estimate.md`, `docs/planning/WBS-SafeCall-Phase1.md`, và các ADR trong `docs/adr/` (gồm `ADR-003-local-only-blocklist.md`). Theo chính file yêu cầu nguồn (`docs/requirements/scam-call-blocking.md`, mục "Bước tiếp theo" / "Cảnh báo cascade"), **toàn bộ các tài liệu này được viết TRƯỚC KHI BLOCK-03 (chặn theo đầu số) và BLOCK-04 (export/import) được thêm vào** và hiện **CHƯA được rà soát/cập nhật** để phản ánh FR-9/FR-10 — cụ thể: HLD cần thêm luồng export/import và cập nhật module chặn real-time; LLD cần thiết kế thuật toán matching exact+prefix hiệu quả và logic validate/merge JSON; **ERD chịu ảnh hưởng lớn nhất** — bảng `blocklist` cần thêm cột `entry_type` (exact/prefix) và xem lại UNIQUE constraint theo từng loại; UIUX cần thêm lựa chọn loại entry và màn hình Export/Import; TestPlan cần test case cho ranh giới prefix match, export/import round-trip, file corrupt, schema version; estimate/WBS cần thêm effort; **ADR-003 cần bổ sung ghi chú về export/import thủ công và cập nhật phần Consequences về privacy khi export** (xem NFR-1). SRS v3.0 này là tài liệu ĐẦU TIÊN trong chuỗi được cập nhật cho đợt bổ sung BLOCK-03/BLOCK-04; các tài liệu thiết kế/kế hoạch còn lại hiện đang **không nhất quán** với SRS v3.0 và cần được cập nhật tiếp theo — không tự ý sửa các tài liệu đó ở đây (ngoài phạm vi vai trò của SRS writer).

### 7.4 Bước tiếp theo (ngoài phạm vi SRS này)
- SRS này là input bắt buộc cho các giai đoạn thiết kế tiếp theo: `system-architect`, `high-level-designer`, `low-level-designer`, `data-modeler`, `ui-ux-designer`, `test-plan-writer`.
- Không xếp độ ưu tiên hay ước lượng effort trong tài liệu này — nếu cần, dùng `project-estimator` sau khi SRS được chốt chính thức.
