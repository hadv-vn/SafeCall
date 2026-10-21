# Yêu cầu đã làm rõ: SafeCall — Chặn cuộc gọi lừa đảo (Android, Phase 1)

## Tóm tắt yêu cầu gốc
SafeCall là app Android (Phase 1; iOS là phase sau, ngoài phạm vi) chặn cuộc gọi lừa đảo dựa trên blocklist cục bộ (local-only, không backend, không chia sẻ giữa người dùng). Blocklist được xây dựng theo ba cách: (1) tự động, ngay lúc đang đổ chuông — xác nhận "Có" sẽ ngắt cuộc gọi hiện tại và thêm vào blocklist; (2) tự động, sau cuộc gọi đã kết nối — hỏi "đây có phải lừa đảo không"; (3) thủ công — người dùng tự xem/thêm/xóa số hoặc đầu số trong blocklist, và có thể export/import blocklist dưới dạng file.

## Mục tiêu nghiệp vụ thực sự
Người dùng muốn tự bảo vệ mình khỏi bị làm phiền/lừa đảo lặp lại bởi các số mà CHÍNH HỌ đã từng nhận diện là lừa đảo, mà không cần nhớ hoặc tự tay chặn từng số — hệ thống tự "học" dần theo trải nghiệm cá nhân của người dùng đó. Đây là MVP cá nhân hóa (không cộng đồng, không chia sẻ dữ liệu tự động), nhưng cho phép người dùng tự sao lưu/chuyển dữ liệu của chính mình khi cần.

## Quyết định đã chốt (từ stakeholder, không tự thay đổi ở các giai đoạn sau)
1. Chỉ hỏi xác nhận lừa đảo SAU cuộc gọi cho **cuộc gọi đến đã kết nối** — cuộc gọi đi (outgoing) và cuộc gọi nhỡ (missed) **không** kích hoạt prompt sau cuộc gọi.
2. "Không phải lừa đảo" → **không hỏi lại vĩnh viễn** cho số đó, áp dụng cho cả hai thời điểm xác nhận (đang đổ chuông và sau cuộc gọi).
3. "Bỏ qua" (không bấm gì) → coi là **tạm thời**, khác với "Không" — vẫn hỏi lại ở lần gọi tiếp theo.
4. Cho phép chặn một số đang là contact đã lưu, nhưng phải **cảnh báo rõ ràng** trước khi xác nhận.
5. Ngoài luồng hỏi xác nhận SAU cuộc gọi, có một TÙY CHỌN THÊM: khi cuộc gọi từ số lạ đang ĐỔ CHUÔNG (chưa bắt máy), app hiển thị overlay cho phép xác nhận "đây có phải lừa đảo không" NGAY LÚC ĐÓ. Không thay thế luồng sau cuộc gọi.
6. Khi xác nhận "Có, đây là lừa đảo" ngay lúc đang đổ chuông: số đó được thêm vào blocklist ngay lập tức, **VÀ cuộc gọi đang đổ chuông đó bị ngắt/từ chối ngay lập tức**. Yêu cầu quyền `ANSWER_PHONE_CALLS` (dùng `TelecomManager.endCall()`).
7. *(Mới bổ sung)* Chặn theo đầu số (prefix) là tính năng THÊM bên cạnh chặn theo số cụ thể (exact match) — không thay thế. Quy tắc khớp: số gọi đến bị chặn nếu BẮT ĐẦU BẰNG một đầu số đã chặn (prefix match). Một blocklist có thể chứa đồng thời cả entry "số cụ thể" (exact) và entry "đầu số" (prefix).
8. *(Mới bổ sung)* Export/Import blocklist là thao tác file THỦ CÔNG hoàn toàn trên thiết bị người dùng: export xuất toàn bộ blocklist ra file JSON, chia sẻ qua Android Share Sheet chuẩn; import đọc file JSON và MERGE vào blocklist hiện tại — bỏ qua entry trùng, KHÔNG xóa/thay thế dữ liệu hiện có. KHÔNG có server/backend tham gia — không mâu thuẫn với ADR-003 (local-only), nhưng làm thay đổi một phần lý do "không cần xử lý privacy" của ADR-003 vì dữ liệu nay có thể được người dùng chủ động truyền ra ngoài thiết bị (xem Ràng buộc phi chức năng).

## User story & Acceptance Criteria

### BLOCK-02 — Chặn real-time số/đầu số đã có trong blocklist *(cập nhật — bổ sung quy tắc khớp đầu số)*
As a người dùng Android đã cài SafeCall và cấp đủ quyền, I want cuộc gọi từ số khớp với blocklist (dù là khớp chính xác số cụ thể hay khớp đầu số) bị tự động chặn ngay khi gọi đến, so that tôi không bị làm phiền lại bởi số hoặc dải số tôi đã biết là lừa đảo.

- Given số gọi đến khớp CHÍNH XÁC (exact match, đã chuẩn hóa định dạng) với một entry "số cụ thể", When cuộc gọi đến, Then cuộc gọi bị chặn tự động, không đổ chuông.
- Given số gọi đến BẮT ĐẦU BẰNG (prefix match, sau khi chuẩn hóa — cả số gọi đến và đầu số đã lưu được chuẩn hóa theo CÙNG quy tắc của `PhoneNumberNormalizer` trước khi so sánh, để tránh lệch giữa dạng nội địa "0912..." và dạng quốc tế "+84912...") một entry "đầu số", When cuộc gọi đến, Then cuộc gọi bị chặn tự động, không đổ chuông.
- Given số gọi đến khớp CẢ HAI loại, When cuộc gọi đến, Then cuộc gọi vẫn bị chặn — khớp MỘT TRONG HAI loại là đủ điều kiện.
- Given số gọi đến KHÔNG khớp exact VÀ KHÔNG khớp prefix với bất kỳ entry nào, When cuộc gọi đến, Then KHÔNG bị chặn bởi blocklist (vẫn có thể được xử lý bởi SCAM-DETECT-01/05).
- Given SafeCall chưa được cấp quyền/vai trò chặn gọi, When có cuộc gọi khớp exact hoặc prefix, Then cuộc gọi KHÔNG bị chặn và app cảnh báo rõ quyền chưa được cấp.
- Given số gọi đến vừa là contact vừa khớp EXACT với một entry số cụ thể, When cuộc gọi đến, Then blocklist được ưu tiên — cuộc gọi vẫn bị chặn.
- Given số gọi đến vừa là contact vừa khớp PREFIX với một đầu số, When cuộc gọi đến, Then cuộc gọi vẫn bị chặn theo quy tắc prefix — vì BLOCK-03 (xem dưới) đã bắt buộc cảnh báo trước khi thêm đầu số nếu đầu số đó khớp với bất kỳ contact nào, người dùng coi như đã chủ động xác nhận rủi ro này từ lúc thêm.

### SCAM-DETECT-01 — Phát hiện cuộc gọi đến kết thúc và hỏi xác nhận
As a người dùng nhận cuộc gọi từ số lạ, I want được hỏi "đây có phải lừa đảo không" ngay sau khi cuộc gọi kết thúc, so that tôi đánh giá được ngay khi còn nhớ rõ nội dung.

- Given cuộc gọi ĐẾN đã kết nối (được trả lời) từ số không phải contact, chưa có trong blocklist, chưa từng bị đánh dấu "Không" (vĩnh viễn), When cuộc gọi kết thúc, Then app hiển thị prompt xác nhận.
- Given cuộc gọi là cuộc gọi ĐI hoặc cuộc gọi NHỠ (không trả lời), When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.
- Given số là contact đã lưu, When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.
- Given số đã có trong blocklist (exact hoặc khớp prefix), When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.
- Given số đã từng được xác nhận "Không phải lừa đảo" (vĩnh viễn), When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.
- Given thời lượng cuộc gọi dưới 3 giây *(giả định/mặc định)*, When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt.

### SCAM-DETECT-02 — Xác nhận "Có" (sau cuộc gọi) → thêm vào blocklist
- Given prompt đang hiển thị cho số X, When người dùng chọn "Có", Then số X được thêm vào blocklist dưới dạng entry "số cụ thể" (exact) ngay lập tức, xuất hiện trong màn hình quản lý blocklist, và bị chặn tự động từ lần gọi tiếp theo.
- Given số X đã có sẵn trong blocklist, When người dùng chọn "Có", Then hệ thống không tạo bản ghi trùng lặp.

### SCAM-DETECT-03 — Xác nhận "Không" / bỏ qua (sau cuộc gọi)
- Given prompt đang hiển thị cho số X, When người dùng chọn "Không" rõ ràng, Then số X được ghi nhận "đã xác nhận không phải lừa đảo" vĩnh viễn và KHÔNG hỏi lại cho số đó nữa ở cả hai thời điểm xác nhận.
- Given người dùng không tương tác với prompt, When cuộc gọi tiếp theo từ cùng số X kết thúc, Then prompt được hiển thị lại.
- Given số X đã được đánh dấu "Không phải lừa đảo" vĩnh viễn nhưng người dùng đổi ý, When người dùng muốn chặn số đó, Then có thể tự thêm tay vào blocklist qua BLOCK-01.

### SCAM-DETECT-05 — Xác nhận lừa đảo ngay lúc đang đổ chuông
As a người dùng nhận cuộc gọi từ số lạ đang đổ chuông, I want được xác nhận ngay "đây có phải lừa đảo không" trong khi cuộc gọi còn đang đổ chuông, so that tôi có thể chặn cuộc gọi đó ngay lập tức và các lần gọi sau.

- Given cuộc gọi đến từ số không phải contact, chưa có trong blocklist (exact hoặc prefix), chưa từng được xác nhận "Không phải lừa đảo", When cuộc gọi đang đổ chuông, Then app hiển thị overlay xác nhận — tùy chọn THÊM, không thay thế SCAM-DETECT-01.
- Given số gọi đến là contact đã lưu, When cuộc gọi đang đổ chuông, Then KHÔNG hiển thị overlay.
- Given số gọi đến đã khớp blocklist (exact hoặc prefix), When cuộc gọi đến, Then KHÔNG hiển thị overlay — cuộc gọi đã bị chặn real-time bởi BLOCK-02 trước khi tới bước đổ chuông, nên luồng này không áp dụng (xác nhận: chặn theo đầu số không cần sửa đổi gì ở SCAM-DETECT-05, vì BLOCK-02 luôn chạy trước và chặn sớm hơn).
- Given số gọi đến đã từng được xác nhận "Không phải lừa đảo" vĩnh viễn, When cuộc gọi đang đổ chuông, Then KHÔNG hiển thị overlay.
- Given overlay đang hiển thị cho số X, When người dùng chọn "Có, đây là lừa đảo", Then (a) số X được thêm vào blocklist dưới dạng entry "số cụ thể" (exact) ngay lập tức; VÀ (b) cuộc gọi ĐANG đổ chuông đó bị ngắt/từ chối ngay lập tức.
- Given overlay đang hiển thị cho số X, When người dùng chọn "Không phải lừa đảo", Then số X được ghi "đã xác nhận không phải lừa đảo" vĩnh viễn, cuộc gọi tiếp tục đổ chuông bình thường.
- Given overlay đang hiển thị cho số X, When cuộc gọi đổi trạng thái (trả lời/hết chuông) TRƯỚC KHI người dùng tương tác, Then overlay tự đóng, không ghi phản hồi; SCAM-DETECT-01 vẫn áp dụng nếu cuộc gọi đã được trả lời.
- Given người dùng không tương tác và cuộc gọi tự kết thúc do không ai trả lời, When cuộc gọi kết thúc, Then KHÔNG hiển thị prompt nào sau đó cho cuộc gọi nhỡ này.
- Given overlay cho số X đã từng bị bỏ qua, When số X gọi lại và vẫn đang đổ chuông, Then overlay hiển thị lại.
- Given thiết bị chưa được cấp quyền cần thiết (overlay hoặc `ANSWER_PHONE_CALLS`), When có cuộc gọi đến từ số lạ đang đổ chuông, Then overlay không hiển thị hoặc không thể ngắt cuộc gọi, cuộc gọi vẫn đổ chuông bình thường.

### BLOCK-01 — Quản lý blocklist thủ công (số cụ thể — exact match)
- Given tôi mở màn hình quản lý blocklist, When tải xong, Then thấy đầy đủ số đang bị chặn (cả entry tự động và thủ công, cả exact và prefix — xem BLOCK-03).
- Given blocklist rỗng, When mở màn hình, Then hiển thị trạng thái rỗng rõ ràng.
- Given tôi nhập số hợp lệ để thêm, When xác nhận, Then số đó vào blocklist (dạng exact) và bị chặn real-time từ thời điểm đó.
- Given tôi nhập số không hợp lệ (sai định dạng/trống), When xác nhận, Then báo lỗi định dạng, không thêm.
- Given tôi nhập số đã có trong blocklist, When xác nhận, Then báo "đã tồn tại", không tạo bản ghi trùng.
- Given tôi chọn xóa một số, When xác nhận xóa, Then số đó bị xóa và không còn bị chặn.
- Given tôi bắt đầu xóa nhưng hủy trước khi xác nhận, When hủy, Then số đó vẫn còn trong blocklist.
- Given tôi thêm tay một số đang là contact vào blocklist, When tôi bắt đầu thêm, Then app hiển thị cảnh báo rõ ràng trước khi cho xác nhận thêm.

### BLOCK-03 — Chặn theo đầu số (prefix blocking) *(mới bổ sung)*
As a người dùng quản lý blocklist, I want thêm một đầu số (chuỗi tiền tố) vào danh sách chặn, so that tôi chặn được toàn bộ dải số bắt đầu bằng đầu số đó mà không phải tự thêm từng số cụ thể một.

- Given tôi chọn thêm theo loại "Đầu số" (khác với "Số cụ thể" ở BLOCK-01), When tôi nhập một chuỗi đầu số hợp lệ (toàn chữ số, có thể có dấu "+" ở đầu, độ dài tối thiểu **4 ký tự số** không tính dấu "+"/mã quốc gia — mặc định đã áp dụng, điều chỉnh được sau nếu cần) và xác nhận, Then đầu số đó được thêm vào blocklist dưới dạng entry loại "đầu số", được chuẩn hóa theo cùng quy tắc `PhoneNumberNormalizer` dùng cho số cụ thể (để so khớp nhất quán ở BLOCK-02), và áp dụng chặn real-time từ lần gọi tiếp theo.
- Given tôi nhập đầu số ngắn hơn 4 ký tự số, When xác nhận, Then báo lỗi rõ ràng ("Đầu số quá ngắn, có thể chặn nhầm nhiều số không liên quan") và không thêm.
- Given tôi nhập chuỗi không hợp lệ (ký tự không phải số ngoài dấu "+" ở đầu, hoặc trống), When xác nhận, Then báo lỗi định dạng, không thêm.
- Given đầu số tôi nhập đã tồn tại chính xác trong danh sách đầu số đang chặn, When xác nhận, Then báo "đã tồn tại", không tạo bản ghi trùng.
- Given đầu số tôi nhập khớp (bằng quy tắc prefix) với số điện thoại của một hoặc nhiều contact đã lưu, When tôi xác nhận thêm, Then app hiển thị cảnh báo rõ ràng (tương tự BLOCK-01/AC8 — ví dụ "Đầu số này khớp với N liên hệ trong danh bạ của bạn, các cuộc gọi từ họ cũng sẽ bị chặn — bạn có chắc muốn tiếp tục?") trước khi cho xác nhận thêm.
- Given tôi mở màn hình quản lý blocklist, When tải xong, Then danh sách hiển thị cả entry "số cụ thể" và entry "đầu số", có nhãn trực quan phân biệt rõ hai loại (ví dụ nhãn "Đầu số" kèm mỗi entry loại này).
- Given tôi chọn xóa một entry đầu số, When xác nhận xóa, Then entry đó bị xóa — các số thuộc dải đó không còn bị chặn theo quy tắc prefix này (trừ khi vẫn khớp exact với một entry số cụ thể khác).
- Given tôi bắt đầu xóa một entry đầu số nhưng hủy trước khi xác nhận, When hủy, Then entry đó vẫn còn trong blocklist.

### BLOCK-04 — Export/Import blocklist *(mới bổ sung)*
As a người dùng muốn lưu dự phòng hoặc chuyển blocklist cá nhân sang thiết bị khác, I want xuất toàn bộ blocklist ra file và nhập lại file đó vào blocklist hiện tại, so that tôi không phải tự thêm lại từng số/đầu số bằng tay khi đổi điện thoại hoặc muốn giữ bản sao dự phòng.

**Export:**
- Given blocklist có ít nhất một entry (số cụ thể và/hoặc đầu số), When tôi chọn "Export blocklist", Then app tạo một file JSON chứa toàn bộ entry hiện có (gồm cả loại, phân biệt rõ trong dữ liệu, kèm trường `schema_version` để hỗ trợ kiểm tra tương thích khi import sau này) và mở Android Share Sheet chuẩn để tôi chọn ứng dụng chia sẻ.
- Given blocklist đang rỗng, When tôi mở màn hình quản lý blocklist, Then nút "Export" ở trạng thái disabled kèm chú thích ngắn ("Chưa có số nào để xuất").
- Given tôi chọn "Export" rồi hủy ngay trong Share Sheet, When hủy, Then blocklist hiện tại không thay đổi gì.

**Import:**
- Given tôi chọn "Import blocklist" và chọn một file JSON đúng định dạng export của SafeCall (cùng `schema_version` hoặc version thấp hơn tương thích), When import, Then các entry trong file được MERGE vào blocklist hiện tại — entry đã tồn tại (khớp chính xác theo loại + giá trị đã chuẩn hóa) bị bỏ qua, entry mới được thêm, KHÔNG entry nào trong blocklist hiện tại bị xóa/ghi đè.
- Given file import có N entry hợp lệ mới và M entry trùng, When import hoàn tất, Then app hiển thị kết quả rõ ràng ("Đã thêm N số mới, bỏ qua M số đã có").
- Given file không phải JSON hợp lệ hoặc không đúng cấu trúc blocklist của SafeCall, When import, Then app báo lỗi rõ ràng ("File không hợp lệ hoặc không đúng định dạng") và blocklist hiện tại KHÔNG bị thay đổi.
- Given file hợp lệ về cấu trúc nhưng chứa một số entry sai định dạng (ví dụ đầu số ngắn hơn 4 ký tự), When import, Then các entry không hợp lệ bị bỏ qua, các entry hợp lệ khác vẫn được merge, và app báo rõ số lượng entry bị bỏ qua vì không hợp lệ.
- Given file import có `schema_version` MỚI HƠN version hiện tại của app hỗ trợ, When import, Then app từ chối import và báo rõ "File được xuất từ phiên bản SafeCall mới hơn, vui lòng cập nhật app" (từ chối an toàn, không cố đọc cấu trúc không rõ).
- Given file import có `schema_version` CŨ HƠN (nhưng vẫn trong danh sách version được hỗ trợ), When import, Then app tự xử lý tương thích xuôi (đọc đúng theo cấu trúc version cũ đó), không báo lỗi.
- Given quá trình import bị gián đoạn giữa chừng (app bị tắt/crash), When người dùng mở lại app, Then blocklist phải ở trạng thái nhất quán — toàn bộ entry hợp lệ của lần import đó đã được thêm, hoặc không entry nào được thêm (cơ chế đảm bảo thuộc thiết kế kỹ thuật, không mô tả ở đây).
- Không giới hạn số lượng entry tối đa cho một lần import ở Phase 1 (mặc định — bổ sung giới hạn sau nếu phát sinh vấn đề hiệu năng thực tế).

### CALLHISTORY-01 — Lịch sử cuộc gọi
- Given có cuộc gọi đến (bị chặn hoặc không), When cuộc gọi kết thúc, Then một bản ghi lịch sử được lưu (số, thời gian, bị chặn hay không — nếu bị chặn bởi blocklist thì ghi rõ chặn do khớp exact hay khớp prefix theo đầu số nào, có được hỏi xác nhận hay không, phản hồi người dùng nếu có).
- Given tôi mở màn hình lịch sử cuộc gọi, When tải xong, Then thấy danh sách cuộc gọi gần đây kèm trạng thái (đã chặn lúc đổ chuông / đã chặn real-time theo số cụ thể / đã chặn real-time theo đầu số / đã hỏi xác nhận sau cuộc gọi / bình thường).
- Quy tắc prefix mới thêm KHÔNG áp dụng hồi tố cho các bản ghi lịch sử đã lưu TRƯỚC KHI đầu số đó được thêm — chỉ ảnh hưởng tới cuộc gọi đến SAU thời điểm thêm (mặc định).

### ONBOARD-01 — Onboarding và cấp quyền
- Given người dùng mở app lần đầu, When onboarding bắt đầu, Then app giải thích rõ lý do cần từng quyền (vai trò chặn gọi, trạng thái cuộc gọi, thông báo, danh bạ, quyền kết thúc cuộc gọi `ANSWER_PHONE_CALLS`) TRƯỚC khi mở dialog hệ thống.
- Given người dùng từ chối một quyền bắt buộc, When từ chối, Then app hiển thị rõ chức năng nào sẽ không hoạt động và cho phép cấp lại sau.
- Given đã cấp đủ quyền bắt buộc, When mở lại app sau này, Then không hiển thị lại toàn bộ luồng onboarding.
- Given một quyền bắt buộc bị thu hồi sau khi đã cấp, When người dùng mở lại app, Then app phát hiện và hướng dẫn cấp lại.
- Given người dùng từ chối quyền truy cập danh bạ, When từ chối, Then app vẫn hoạt động nhưng TẤT CẢ số gọi đến được coi là "số lạ" — banner giải thích rõ hệ quả.

## Phạm vi

### Trong phạm vi
- Android, Phase 1.
- Chặn real-time dựa trên blocklist cục bộ — gồm cả khớp chính xác số cụ thể (exact match) VÀ khớp đầu số (prefix match).
- Quản lý blocklist thủ công: thêm/xóa số cụ thể (BLOCK-01) VÀ thêm/xóa đầu số (BLOCK-03), hiển thị phân biệt rõ hai loại, gồm cảnh báo khi chặn contact (cả exact và prefix).
- Export toàn bộ blocklist ra file JSON (kèm `schema_version`), chia sẻ qua Android Share Sheet; Import file JSON theo cơ chế MERGE (BLOCK-04). Thao tác THỦ CÔNG, hoàn toàn cục bộ — KHÔNG mâu thuẫn với ADR-003.
- Phát hiện cuộc gọi đến đã kết nối kết thúc, hỏi xác nhận cho số lạ (SCAM-DETECT-01/02/03).
- Xác nhận lừa đảo ngay lúc cuộc gọi đang đổ chuông, kèm ngắt cuộc gọi hiện tại (SCAM-DETECT-05).
- Lịch sử cuộc gọi, gồm phân biệt nguồn chặn (exact/prefix).
- Onboarding và cấp quyền, gồm `ANSWER_PHONE_CALLS`.
- Local-only, không backend, không chia sẻ/đồng bộ TỰ ĐỘNG giữa người dùng hoặc giữa thiết bị.

### Ngoài phạm vi
- iOS — phase sau.
- Backend, đồng bộ/chia sẻ blocklist cộng đồng (nhiều người dùng góp chung).
- Backup cloud TỰ ĐỘNG và đồng bộ đa thiết bị TỰ ĐỘNG (export/import thủ công của BLOCK-04 không phải cơ chế tự động — người dùng phải tự thực hiện mỗi lần).
- Nhận diện lừa đảo tự động bằng AI/phân tích nội dung cuộc gọi.
- Số điện thoại ẩn danh/riêng tư (áp dụng cho cả exact và prefix).
- Hỏi xác nhận cho cuộc gọi đi hoặc cuộc gọi nhỡ.
- Màn hình riêng quản lý danh sách "đã xác nhận Không phải lừa đảo".
- Áp dụng hồi tố quy tắc prefix cho lịch sử cuộc gọi đã lưu trước khi đầu số được thêm.
- Giới hạn số lượng entry tối đa khi import.

## Ràng buộc phi chức năng sơ bộ
- Blocklist chứa số điện thoại của bên thứ ba — dữ liệu cá nhân cần xử lý cẩn trọng dù local-only.
- **Điểm nhạy cảm mới (BLOCK-04 export)**: khi export, số điện thoại bên thứ ba được CHỦ ĐỘNG truyền ra ngoài thiết bị qua kênh người dùng tự chọn — khác với mô tả ban đầu của ADR-003 ("không cần xử lý privacy vì không qua mạng"). Cần rà soát lại ADR-003 ở bước cascade. Không yêu cầu mã hóa file export ở Phase 1.
- Phụ thuộc các quyền nhạy cảm (vai trò chặn gọi, trạng thái điện thoại, danh bạ, thông báo, `ANSWER_PHONE_CALLS`).
- Rủi ro chặn nhầm được KHUẾCH ĐẠI bởi BLOCK-03 — một đầu số ngắn chặn nhầm nhiều số không liên quan cùng lúc, rộng hơn rủi ro chặn một số cụ thể — đây là lý do có ngưỡng độ dài tối thiểu và cảnh báo khi khớp contact.
- Độ tin cậy phát hiện cuộc gọi kết thúc, hiển thị overlay, và hiệu năng tra cứu matching (exact + prefix, đặc biệt khi blocklist lớn — cần cấu trúc tra cứu phù hợp, không so khớp tuyến tính) là ràng buộc kỹ thuật thuộc thiết kế.

## Giả định/mặc định đã áp dụng (để không chặn tiến độ)
- Ngưỡng thời lượng cuộc gọi tối thiểu để hỏi xác nhận sau cuộc gọi: 3 giây.
- Không có push notification chủ động khi quyền bị thu hồi — chỉ kiểm tra khi app foreground.
- Không có màn hình riêng quản lý danh sách "đã xác nhận Không phải lừa đảo".
- Ngưỡng độ dài tối thiểu cho đầu số: 4 ký tự số (không tính "+"/mã quốc gia).
- Đầu số được chuẩn hóa theo cùng quy tắc `PhoneNumberNormalizer` dùng cho số cụ thể.
- Thêm đầu số khớp contact → cảnh báo trước khi xác nhận (cùng pattern với BLOCK-01).
- Export khi blocklist rỗng → disable nút Export.
- JSON export có trường `schema_version`; import từ chối version mới hơn, tự xử lý tương thích xuôi với version cũ hơn được hỗ trợ.
- Không giới hạn số lượng entry khi import ở Phase 1.
- Prefix không áp dụng hồi tố cho lịch sử cuộc gọi đã lưu.

## Bước tiếp theo
- **Cảnh báo cascade**: các thay đổi trong lần cập nhật này (SCAM-DETECT-05 kèm ngắt gọi; BLOCK-03 chặn theo đầu số; BLOCK-04 export/import) đã lan tới các tài liệu sau — CẦN rà soát/cập nhật lại trước khi coi Phase 1 nhất quán end-to-end:
  - `docs/srs/SRS-SafeCall-Phase1.md` — thêm FR cho BLOCK-03/BLOCK-04, cập nhật FR logic chặn real-time (exact + prefix).
  - `docs/design/HLD-SafeCall-Phase1.md` — thêm luồng export (tạo JSON + Share Sheet) và import (đọc/validate/merge) là thành phần mới; cập nhật mô tả module chặn real-time.
  - `docs/design/LLD-SafeCall-Phase1.md` — thiết kế thuật toán matching (exact + prefix, cấu trúc tra cứu hiệu quả), validate/parse JSON, xử lý file corrupt, logic merge.
  - `docs/design/ERD-SafeCall-Phase1.md` — **ảnh hưởng lớn nhất**: bảng `blocklist` cần thêm cột `entry_type` (exact/prefix), xem lại UNIQUE constraint theo từng loại.
  - `docs/design/UIUX-SafeCall-Phase1.md` — màn hình quản lý blocklist thêm lựa chọn loại entry, UI Export/Import.
  - `docs/test/TestPlan-SafeCall-Phase1.md` — test case cho ranh giới prefix match, export/import round-trip, file corrupt, schema version.
  - `docs/planning/estimate.md`, `docs/planning/WBS-SafeCall-Phase1.md` — thêm effort.
  - `docs/adr/ADR-003-local-only-blocklist.md` — bổ sung ghi chú: export/import thủ công không mâu thuẫn với local-only, nhưng cập nhật lại phần Consequences về privacy khi export.
