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
9. *(Mới bổ sung — xác nhận qua `AskUserQuestion` với stakeholder, 2026-11-06)* **Cơ chế ưu tiên Danh bạ (CONTACTS-PRIORITY)**: có một cài đặt (setting) BẬT/TẮT được — khi BẬT, nếu số gọi đến đã nằm trong Danh bạ (Contacts) của máy VÀ chỉ khớp theo quy tắc **đầu số/prefix** (FR-9/BLOCK-03) một cách VÔ TÌNH (người dùng không chủ động chặn chính contact đó), hệ thống bỏ qua việc chặn — cuộc gọi vẫn đổ chuông bình thường, KHÔNG áp dụng cho overlay lúc đổ chuông hay hỏi xác nhận sau cuộc gọi (các luồng đó đã tự loại trừ contact từ trước — xem SCAM-DETECT-01/05). Cơ chế này **KHÔNG** ghi đè trường hợp người dùng đã CHỦ ĐỘNG thêm chính số/entry đó (khớp EXACT) vào blocklist sau khi đã thấy cảnh báo "số này có trong danh bạ" (BLOCK-01/AC8, BLOCK-03/AC5) — quyết định chặn tường minh đó vẫn được tôn trọng. Mặc định của setting này là **BẬT**. Đây là thay đổi hành vi so với quyết định #4 ở trên đối với RIÊNG trường hợp khớp prefix vô tình — quyết định #4 (chặn exact + cảnh báo) giữ nguyên không đổi.
10. *(Mới bổ sung — xác nhận qua `AskUserQuestion` với stakeholder, 2026-11-06)* **Bật/Tắt các chế độ chặn nâng cao (BLOCK-TOGGLE)**: màn hình Cài đặt có 3 công tắc BẬT/TẮT độc lập — "Chặn tất cả số lạ" (số không có trong Danh bạ), "Chặn số ẩn danh" (Private/Unknown Caller ID — **đưa vào phạm vi Phase 1, thay thế quyết định "ngoài phạm vi" trước đó về số ẩn danh**), "Chặn số quốc tế" (số sau khi chuẩn hóa qua `PhoneNumberNormalizer` không thuộc mã quốc gia Việt Nam **+84** — cố định cho Phase 1, không có tùy chọn đổi "quốc gia nhà"). Cả 3 công tắc **mặc định TẮT** khi cài app lần đầu. Khi một công tắc BẬT và số gọi đến khớp điều kiện tương ứng, cuộc gọi bị **chặn thẳng giống blocklist** (không đổ chuông, không kích hoạt FR-2/FR-8), ngoại trừ: **Contacts đã lưu luôn được MIỄN TRỪ khỏi cả 3 công tắc này** (nhất quán với CONTACTS-PRIORITY ở quyết định #9) — một contact có số quốc tế hoặc không nằm trong danh bạ (mâu thuẫn logic nhưng về bản chất "số lạ" đã loại trừ contact) vẫn không bị chặn bởi BLOCK-TOGGLE. Số ẩn danh (không có số để so khớp) không thể là contact nên không áp dụng miễn trừ cho trường hợp đó.
11. *(Mới bổ sung — xác nhận qua `AskUserQuestion` với stakeholder, 2026-11-06)* **Kiểm tra trạng thái Quyền định kỳ (PERMISSION-CHECK)**: ngoài việc kiểm tra quyền khi app được mở lên foreground (ONBOARD-01/AC4 hiện có), app còn chạy một tác vụ nền (background task) kiểm tra định kỳ **mỗi ngày 1 lần** trạng thái của **TẤT CẢ quyền quan trọng** đã liệt kê (vai trò chặn gọi — Call Screening Role, trạng thái cuộc gọi, danh bạ, thông báo, `ANSWER_PHONE_CALLS`, VÀ quyền **Overlay — "Vẽ đè lên ứng dụng khác"/`SYSTEM_ALERT_WINDOW`**). Nếu phát hiện bất kỳ quyền nào bị hệ điều hành thu hồi (ví dụ do App Standby/Doze, hoặc người dùng tự tắt trong Cài đặt hệ thống), app phát một thông báo (notification) nhắc người dùng cấp lại quyền đó, kèm giải thích ngắn chức năng nào sẽ không hoạt động. Thông báo này được **lặp lại ở mỗi chu kỳ kiểm tra tiếp theo** (tức mỗi ngày) nếu quyền đó vẫn chưa được cấp lại — không chỉ báo một lần duy nhất. **Xác nhận bổ sung quan trọng**: quyền Overlay (`SYSTEM_ALERT_WINDOW`) trước đây là câu hỏi mở chưa được `system-architect` xác nhận có cần thiết cho FR-8 hay không (xem SRS Phần 7.1.1) — nay được CHÍNH THỨC xác nhận là quyền cần thiết và được thêm vào danh sách quyền của app (Phần "Giao diện ngoài"/ONBOARD-01), không còn là câu hỏi mở về SỰ CẦN THIẾT của quyền này (câu hỏi mở còn lại CHỈ là tính khả thi kỹ thuật triển khai).

## User story & Acceptance Criteria

### BLOCK-02 — Chặn real-time số/đầu số đã có trong blocklist *(cập nhật — bổ sung quy tắc khớp đầu số)*
As a người dùng Android đã cài SafeCall và cấp đủ quyền, I want cuộc gọi từ số khớp với blocklist (dù là khớp chính xác số cụ thể hay khớp đầu số) bị tự động chặn ngay khi gọi đến, so that tôi không bị làm phiền lại bởi số hoặc dải số tôi đã biết là lừa đảo.

- Given số gọi đến khớp CHÍNH XÁC (exact match, đã chuẩn hóa định dạng) với một entry "số cụ thể", When cuộc gọi đến, Then cuộc gọi bị chặn tự động, không đổ chuông.
- Given số gọi đến BẮT ĐẦU BẰNG (prefix match, sau khi chuẩn hóa — cả số gọi đến và đầu số đã lưu được chuẩn hóa theo CÙNG quy tắc của `PhoneNumberNormalizer` trước khi so sánh, để tránh lệch giữa dạng nội địa "0912..." và dạng quốc tế "+84912...") một entry "đầu số", When cuộc gọi đến, Then cuộc gọi bị chặn tự động, không đổ chuông.
- Given số gọi đến khớp CẢ HAI loại, When cuộc gọi đến, Then cuộc gọi vẫn bị chặn — khớp MỘT TRONG HAI loại là đủ điều kiện.
- Given số gọi đến KHÔNG khớp exact VÀ KHÔNG khớp prefix với bất kỳ entry nào, When cuộc gọi đến, Then KHÔNG bị chặn bởi blocklist (vẫn có thể được xử lý bởi SCAM-DETECT-01/05).
- Given SafeCall chưa được cấp quyền/vai trò chặn gọi, When có cuộc gọi khớp exact hoặc prefix, Then cuộc gọi KHÔNG bị chặn và app cảnh báo rõ quyền chưa được cấp.
- Given số gọi đến vừa là contact vừa khớp EXACT với một entry số cụ thể, When cuộc gọi đến, Then blocklist được ưu tiên — cuộc gọi vẫn bị chặn.
- Given số gọi đến vừa là contact vừa khớp PREFIX với một đầu số, và cài đặt CONTACTS-PRIORITY (xem dưới) đang TẮT, When cuộc gọi đến, Then cuộc gọi vẫn bị chặn theo quy tắc prefix — vì BLOCK-03 (xem dưới) đã bắt buộc cảnh báo trước khi thêm đầu số nếu đầu số đó khớp với bất kỳ contact nào, người dùng coi như đã chủ động xác nhận rủi ro này từ lúc thêm.
- *(Mới bổ sung — CONTACTS-PRIORITY)* Given số gọi đến vừa là contact vừa khớp PREFIX (và KHÔNG khớp EXACT với bất kỳ entry nào), và cài đặt CONTACTS-PRIORITY đang BẬT (mặc định), When cuộc gọi đến, Then cuộc gọi KHÔNG bị chặn — contact được ưu tiên, bỏ qua quy tắc prefix cho riêng số này.
- *(Mới bổ sung — CONTACTS-PRIORITY)* Given số gọi đến vừa là contact vừa khớp EXACT với một entry số cụ thể (bất kể CONTACTS-PRIORITY BẬT hay TẮT), When cuộc gọi đến, Then cuộc gọi vẫn bị chặn — khớp exact là hành động chặn CHỦ ĐỘNG của người dùng (đã qua cảnh báo), không bị ảnh hưởng bởi CONTACTS-PRIORITY.

### CONTACTS-PRIORITY — Cơ chế ưu tiên Danh bạ *(mới bổ sung)*
As a người dùng có danh bạ với nhiều số đã lưu, I want contact của tôi không bị chặn nhầm chỉ vì vô tình khớp một đầu số tôi đã thêm, so that tôi không bỏ lỡ cuộc gọi từ người quen do một quy tắc chặn rộng.

- Given tôi mở màn hình Cài đặt (Settings), When tải xong, Then thấy một tùy chọn BẬT/TẮT cho "Ưu tiên Danh bạ" (CONTACTS-PRIORITY), mặc định đang BẬT.
- Given CONTACTS-PRIORITY đang BẬT, số gọi đến là contact đã lưu VÀ chỉ khớp PREFIX (không khớp EXACT) với một đầu số trong blocklist, When cuộc gọi đến, Then cuộc gọi KHÔNG bị chặn, đổ chuông bình thường như một cuộc gọi không khớp blocklist.
- Given CONTACTS-PRIORITY đang TẮT, When áp dụng lại đúng hành vi gốc của BLOCK-02 (blocklist luôn thắng, kể cả khớp prefix với contact).
- Given CONTACTS-PRIORITY đang BẬT, số gọi đến là contact đã lưu VÀ khớp EXACT với một entry số cụ thể (người dùng đã chủ động chặn chính contact này, qua cảnh báo ở BLOCK-01/AC8), When cuộc gọi đến, Then cuộc gọi vẫn bị chặn — CONTACTS-PRIORITY không ghi đè hành động chặn chủ động.
- Given người dùng đổi trạng thái CONTACTS-PRIORITY trong Cài đặt, When lưu, Then quy tắc mới áp dụng ngay cho cuộc gọi tiếp theo — không cần khởi động lại app.
- Given CONTACTS-PRIORITY đang BẬT và một cuộc gọi từ contact bị bỏ qua chặn (vì chỉ khớp prefix), When cuộc gọi kết thúc, Then bản ghi lịch sử cuộc gọi (CALLHISTORY-01) vẫn được lưu, ghi rõ lý do không chặn là "contact được ưu tiên" (để người dùng hiểu vì sao số đó không bị chặn dù khớp một đầu số đang chặn).

### BLOCK-TOGGLE — Bật/Tắt các chế độ chặn nâng cao *(mới bổ sung)*
As a người dùng muốn chặn rộng hơn blocklist cá nhân, I want bật nhanh các bộ lọc "chặn số lạ", "chặn số ẩn danh", "chặn số quốc tế" trong Cài đặt, so that tôi tự quyết định mức độ chặn phù hợp với nhu cầu của mình mà không cần thêm từng số vào blocklist.

- Given tôi mở màn hình Cài đặt, When tải xong, Then thấy 3 công tắc BẬT/TẮT độc lập: "Chặn tất cả số lạ", "Chặn số ẩn danh", "Chặn số quốc tế" — cả 3 đều mặc định TẮT khi cài app lần đầu.
- Given "Chặn tất cả số lạ" đang BẬT, số gọi đến KHÔNG phải contact đã lưu (và không được miễn trừ theo quy tắc khác), When cuộc gọi đến, Then cuộc gọi bị chặn thẳng — không đổ chuông, không kích hoạt SCAM-DETECT-01/05.
- Given "Chặn số ẩn danh" đang BẬT, cuộc gọi đến KHÔNG có số hiển thị (Private/Unknown Caller ID, không có số để so khớp với bất kỳ danh sách nào), When cuộc gọi đến, Then cuộc gọi bị chặn thẳng — không đổ chuông, không kích hoạt SCAM-DETECT-01/05. *(Đưa vào phạm vi Phase 1 — thay thế quyết định "ngoài phạm vi" trước đó về số ẩn danh, CHỈ cho trường hợp bật công tắc này.)*
- Given "Chặn số quốc tế" đang BẬT, số gọi đến sau khi chuẩn hóa qua `PhoneNumberNormalizer` có mã quốc gia KHÁC Việt Nam (+84), When cuộc gọi đến, Then cuộc gọi bị chặn thẳng — không đổ chuông, không kích hoạt SCAM-DETECT-01/05. Mã quốc gia tham chiếu (+84) CỐ ĐỊNH cho Phase 1 — không có tùy chọn đổi "quốc gia nhà".
- Given số gọi đến LÀ contact đã lưu, When bất kỳ công tắc nào trong "Chặn số lạ" hoặc "Chặn số quốc tế" đang BẬT, Then cuộc gọi KHÔNG bị chặn bởi công tắc đó — Contacts luôn được MIỄN TRỪ khỏi cả hai bộ lọc này (nhất quán với CONTACTS-PRIORITY). *(Số ẩn danh không thể là contact nên không áp dụng miễn trừ này cho "Chặn số ẩn danh".)*
- Given một số vừa khớp "số lạ" VÀ "số quốc tế" cùng lúc (cả hai công tắc đang BẬT), When cuộc gọi đến, Then cuộc gọi vẫn chỉ bị chặn một lần (không có hành vi khác biệt khi khớp nhiều điều kiện).
- Given một cuộc gọi bị chặn bởi bất kỳ công tắc nào trong BLOCK-TOGGLE, When cuộc gọi kết thúc, Then bản ghi lịch sử cuộc gọi (CALLHISTORY-01) vẫn được lưu, ghi rõ lý do chặn là công tắc nào ("chặn số lạ" / "chặn số ẩn danh" / "chặn số quốc tế").
- Given người dùng đổi trạng thái bất kỳ công tắc nào trong Cài đặt, When lưu, Then quy tắc mới áp dụng ngay cho cuộc gọi tiếp theo — không cần khởi động lại app.
- Given số gọi đến đã khớp blocklist (BLOCK-02, exact hoặc prefix), When cuộc gọi đến, Then cuộc gọi bị chặn bởi BLOCK-02 trước — các công tắc BLOCK-TOGGLE không cần đánh giá thêm (blocklist luôn được kiểm tra trước BLOCK-TOGGLE).

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

### ONBOARD-01 — Onboarding và cấp quyền *(cập nhật — bổ sung quyền Overlay)*
- Given người dùng mở app lần đầu, When onboarding bắt đầu, Then app giải thích rõ lý do cần từng quyền (vai trò chặn gọi, trạng thái cuộc gọi, thông báo, danh bạ, quyền kết thúc cuộc gọi `ANSWER_PHONE_CALLS`, **quyền Overlay "Vẽ đè lên ứng dụng khác" `SYSTEM_ALERT_WINDOW` — mới xác nhận, phục vụ SCAM-DETECT-05**) TRƯỚC khi mở dialog hệ thống.
- Given người dùng từ chối một quyền bắt buộc, When từ chối, Then app hiển thị rõ chức năng nào sẽ không hoạt động và cho phép cấp lại sau.
- Given đã cấp đủ quyền bắt buộc, When mở lại app sau này, Then không hiển thị lại toàn bộ luồng onboarding.
- Given một quyền bắt buộc bị thu hồi sau khi đã cấp, When người dùng mở lại app, Then app phát hiện và hướng dẫn cấp lại.
- Given người dùng từ chối quyền truy cập danh bạ, When từ chối, Then app vẫn hoạt động nhưng TẤT CẢ số gọi đến được coi là "số lạ" — banner giải thích rõ hệ quả.

### PERMISSION-CHECK — Kiểm tra trạng thái Quyền định kỳ *(mới bổ sung)*
As a người dùng đã cấp đủ quyền cho SafeCall nhưng lâu ngày không mở app, I want được thông báo chủ động nếu một quyền quan trọng bị hệ điều hành thu hồi (ví dụ do App Standby/Doze) mà không cần tự mở app để phát hiện, so that app vẫn bảo vệ tôi liên tục mà tôi không cần nhớ tự kiểm tra.

- Given app đã được cấp đủ các quyền quan trọng (vai trò chặn gọi, trạng thái cuộc gọi, danh bạ, thông báo, `ANSWER_PHONE_CALLS`, Overlay `SYSTEM_ALERT_WINDOW`), When một tác vụ nền chạy **định kỳ mỗi ngày 1 lần**, Then tác vụ kiểm tra lại trạng thái hiện tại của TẤT CẢ các quyền này (không chỉ khi app được mở foreground — xem ONBOARD-01/AC4).
- Given tác vụ kiểm tra định kỳ phát hiện một hoặc nhiều quyền trong số trên đã bị thu hồi (do App Standby/Doze, hoặc người dùng tự tắt trong Cài đặt hệ thống Android), When phát hiện, Then app phát một thông báo (notification) hệ thống nhắc cấp lại quyền, kèm giải thích ngắn gọn chức năng nào đang KHÔNG hoạt động vì thiếu quyền đó.
- Given quyền bị thiếu vẫn CHƯA được cấp lại ở chu kỳ kiểm tra tiếp theo (ngày sau), When kiểm tra lại vẫn phát hiện thiếu, Then app LẶP LẠI thông báo nhắc nhở — không chỉ báo một lần duy nhất rồi im lặng.
- Given quyền đã được cấp lại (qua onboarding lại, qua Cài đặt hệ thống, hoặc qua banner trong app), When chu kỳ kiểm tra tiếp theo chạy, Then không còn thông báo nhắc nhở cho quyền đó nữa.
- Given người dùng nhấn vào thông báo nhắc cấp lại quyền, When mở, Then app điều hướng thẳng tới màn hình/luồng cấp lại quyền tương ứng (không chỉ mở app ở màn hình chính).
- Cơ chế kỹ thuật cụ thể để chạy tác vụ nền định kỳ đáng tin cậy trên Android (ví dụ `WorkManager` periodic work, ràng buộc tối thiểu ~15 phút/lần của hệ thống, tương thích với App Standby/Doze) thuộc phạm vi thiết kế kỹ thuật, không mô tả chi tiết ở đây — xem `system-architect`/`low-level-designer`.

## Phạm vi

### Trong phạm vi
- Android, Phase 1.
- Chặn real-time dựa trên blocklist cục bộ — gồm cả khớp chính xác số cụ thể (exact match) VÀ khớp đầu số (prefix match).
- Quản lý blocklist thủ công: thêm/xóa số cụ thể (BLOCK-01) VÀ thêm/xóa đầu số (BLOCK-03), hiển thị phân biệt rõ hai loại, gồm cảnh báo khi chặn contact (cả exact và prefix).
- Cơ chế ưu tiên Danh bạ (CONTACTS-PRIORITY): cài đặt BẬT/TẮT (mặc định BẬT) để contact bỏ qua chặn theo prefix vô tình — không ảnh hưởng tới chặn exact đã được người dùng chủ động xác nhận.
- Bật/Tắt các chế độ chặn nâng cao (BLOCK-TOGGLE): 3 công tắc độc lập trong Cài đặt — chặn tất cả số lạ, chặn số ẩn danh, chặn số quốc tế (mã quốc gia cố định +84) — mặc định TẮT cả 3; Contacts luôn miễn trừ khỏi "chặn số lạ" và "chặn số quốc tế".
- Export toàn bộ blocklist ra file JSON (kèm `schema_version`), chia sẻ qua Android Share Sheet; Import file JSON theo cơ chế MERGE (BLOCK-04). Thao tác THỦ CÔNG, hoàn toàn cục bộ — KHÔNG mâu thuẫn với ADR-003.
- Phát hiện cuộc gọi đến đã kết nối kết thúc, hỏi xác nhận cho số lạ (SCAM-DETECT-01/02/03).
- Xác nhận lừa đảo ngay lúc cuộc gọi đang đổ chuông, kèm ngắt cuộc gọi hiện tại (SCAM-DETECT-05).
- Lịch sử cuộc gọi, gồm phân biệt nguồn chặn (exact/prefix).
- Onboarding và cấp quyền, gồm `ANSWER_PHONE_CALLS` VÀ quyền Overlay `SYSTEM_ALERT_WINDOW` (mới xác nhận).
- Kiểm tra trạng thái Quyền định kỳ (PERMISSION-CHECK): tác vụ nền chạy mỗi ngày 1 lần kiểm tra TẤT CẢ quyền quan trọng, phát thông báo nhắc cấp lại (lặp lại mỗi chu kỳ nếu còn thiếu) nếu phát hiện quyền bị thu hồi — bổ sung cho cơ chế kiểm tra khi mở app (ONBOARD-01) đã có.
- Local-only, không backend, không chia sẻ/đồng bộ TỰ ĐỘNG giữa người dùng hoặc giữa thiết bị.

### Ngoài phạm vi
- iOS — phase sau.
- Backend, đồng bộ/chia sẻ blocklist cộng đồng (nhiều người dùng góp chung).
- Backup cloud TỰ ĐỘNG và đồng bộ đa thiết bị TỰ ĐỘNG (export/import thủ công của BLOCK-04 không phải cơ chế tự động — người dùng phải tự thực hiện mỗi lần).
- Nhận diện lừa đảo tự động bằng AI/phân tích nội dung cuộc gọi.
- Số điện thoại ẩn danh/riêng tư KHÔNG thể so khớp với blocklist (exact/prefix — không có số để so khớp) — vẫn ngoài phạm vi cho BLOCK-02/BLOCK-03. *(Cập nhật: riêng việc CHẶN THẲNG toàn bộ số ẩn danh qua công tắc "Chặn số ẩn danh" của BLOCK-TOGGLE nay ĐÃ vào Trong phạm vi — xem mục Trong phạm vi.)*
- Hỏi xác nhận cho cuộc gọi đi hoặc cuộc gọi nhỡ.
- Màn hình riêng quản lý danh sách "đã xác nhận Không phải lừa đảo".
- Áp dụng hồi tố quy tắc prefix cho lịch sử cuộc gọi đã lưu trước khi đầu số được thêm.
- Giới hạn số lượng entry tối đa khi import.
- Tùy chọn đổi "quốc gia nhà" cho định nghĩa "số quốc tế" ở BLOCK-TOGGLE — cố định +84 cho Phase 1.

## Ràng buộc phi chức năng sơ bộ
- Blocklist chứa số điện thoại của bên thứ ba — dữ liệu cá nhân cần xử lý cẩn trọng dù local-only.
- **Điểm nhạy cảm mới (BLOCK-04 export)**: khi export, số điện thoại bên thứ ba được CHỦ ĐỘNG truyền ra ngoài thiết bị qua kênh người dùng tự chọn — khác với mô tả ban đầu của ADR-003 ("không cần xử lý privacy vì không qua mạng"). Cần rà soát lại ADR-003 ở bước cascade. Không yêu cầu mã hóa file export ở Phase 1.
- Phụ thuộc các quyền nhạy cảm (vai trò chặn gọi, trạng thái điện thoại, danh bạ, thông báo, `ANSWER_PHONE_CALLS`, Overlay `SYSTEM_ALERT_WINDOW`).
- Tác vụ nền định kỳ của PERMISSION-CHECK tiêu tốn tài nguyên hệ thống (pin, lịch trình công việc) và bị ràng buộc bởi App Standby/Doze của Android — cần cơ chế kỹ thuật phù hợp (ví dụ `WorkManager`) để chạy đáng tin cậy mà không bị hệ thống chặn/trễ quá mức; đây là ràng buộc kỹ thuật thuộc thiết kế.
- Rủi ro chặn nhầm được KHUẾCH ĐẠI bởi BLOCK-03 — một đầu số ngắn chặn nhầm nhiều số không liên quan cùng lúc, rộng hơn rủi ro chặn một số cụ thể — đây là lý do có ngưỡng độ dài tối thiểu và cảnh báo khi khớp contact.
- Độ tin cậy phát hiện cuộc gọi kết thúc, hiển thị overlay, và hiệu năng tra cứu matching (exact + prefix, đặc biệt khi blocklist lớn — cần cấu trúc tra cứu phù hợp, không so khớp tuyến tính) là ràng buộc kỹ thuật thuộc thiết kế.

## Giả định/mặc định đã áp dụng (để không chặn tiến độ)
- Ngưỡng thời lượng cuộc gọi tối thiểu để hỏi xác nhận sau cuộc gọi: 3 giây.
- ~~Không có push notification chủ động khi quyền bị thu hồi — chỉ kiểm tra khi app foreground.~~ **Đã thay đổi bởi quyết định #11 (PERMISSION-CHECK, 2026-11-06)**: nay CÓ thông báo chủ động qua tác vụ nền định kỳ mỗi ngày — xem mục PERMISSION-CHECK. Việc kiểm tra khi app foreground (ONBOARD-01/AC4) vẫn giữ nguyên, PERMISSION-CHECK là cơ chế BỔ SUNG, không thay thế.
- Không có màn hình riêng quản lý danh sách "đã xác nhận Không phải lừa đảo".
- Ngưỡng độ dài tối thiểu cho đầu số: 4 ký tự số (không tính "+"/mã quốc gia).
- Đầu số được chuẩn hóa theo cùng quy tắc `PhoneNumberNormalizer` dùng cho số cụ thể.
- Thêm đầu số khớp contact → cảnh báo trước khi xác nhận (cùng pattern với BLOCK-01).
- Export khi blocklist rỗng → disable nút Export.
- JSON export có trường `schema_version`; import từ chối version mới hơn, tự xử lý tương thích xuôi với version cũ hơn được hỗ trợ.
- Không giới hạn số lượng entry khi import ở Phase 1.
- Prefix không áp dụng hồi tố cho lịch sử cuộc gọi đã lưu.
- CONTACTS-PRIORITY mặc định BẬT khi cài app lần đầu; chỉ ảnh hưởng tới khớp prefix với contact, không ảnh hưởng tới khớp exact (chặn chủ động vẫn luôn thắng).
- BLOCK-TOGGLE (chặn số lạ/ẩn danh/quốc tế) mặc định TẮT cả 3 khi cài app lần đầu — tính năng aggressive, để người dùng chủ động bật.
- PERMISSION-CHECK chạy định kỳ mỗi ngày 1 lần (không phải thời gian thực) — cân bằng giữa phát hiện sớm và tiết kiệm pin/tuân thủ ràng buộc App Standby/Doze của Android.
- PERMISSION-CHECK giám sát TẤT CẢ quyền quan trọng (không chỉ Call Screening Role và Overlay) — bao gồm cả quyền Overlay `SYSTEM_ALERT_WINDOW` nay được xác nhận chính thức là cần thiết cho SCAM-DETECT-05.
- Thông báo nhắc cấp lại quyền LẶP LẠI ở mỗi chu kỳ kiểm tra (mỗi ngày) nếu quyền vẫn chưa được cấp lại, không chỉ báo một lần.
- Mã quốc gia tham chiếu cho "số quốc tế" cố định là +84 (Việt Nam) cho Phase 1, không có cấu hình đổi quốc gia.
- BLOCK-02 (blocklist) luôn được kiểm tra trước BLOCK-TOGGLE — một số đã bị chặn bởi blocklist thì không cần đánh giá thêm bởi các công tắc BLOCK-TOGGLE.

## Bước tiếp theo
- **Cảnh báo cascade**: các thay đổi trong lần cập nhật này (SCAM-DETECT-05 kèm ngắt gọi; BLOCK-03 chặn theo đầu số; BLOCK-04 export/import; CONTACTS-PRIORITY; BLOCK-TOGGLE; PERMISSION-CHECK) đã lan tới các tài liệu sau — CẦN rà soát/cập nhật lại trước khi coi Phase 1 nhất quán end-to-end:
  - `docs/srs/SRS-SafeCall-Phase1.md` — thêm FR cho BLOCK-03/BLOCK-04/CONTACTS-PRIORITY/BLOCK-TOGGLE/PERMISSION-CHECK, cập nhật FR logic chặn real-time (exact + prefix + override theo cài đặt + 3 công tắc nâng cao); cập nhật FR Onboarding (ONBOARD-01) để thêm quyền Overlay; cập nhật NFR liên quan tới tài nguyên hệ thống (pin, lịch trình nền) cho tác vụ định kỳ.
  - `docs/design/HLD-SafeCall-Phase1.md` — thêm luồng export (tạo JSON + Share Sheet) và import (đọc/validate/merge) là thành phần mới; cập nhật mô tả module chặn real-time để tính đến setting CONTACTS-PRIORITY và pipeline BLOCK-TOGGLE (số lạ/ẩn danh/quốc tế); thêm màn hình Cài đặt (Settings) nếu chưa có — nay chứa cả CONTACTS-PRIORITY và 3 công tắc BLOCK-TOGGLE; thêm thành phần mới "tác vụ nền kiểm tra quyền định kỳ" (PERMISSION-CHECK).
  - `docs/design/LLD-SafeCall-Phase1.md` — thiết kế thuật toán matching (exact + prefix, cấu trúc tra cứu hiệu quả), validate/parse JSON, xử lý file corrupt, logic merge; bổ sung bước kiểm tra contact + cờ CONTACTS-PRIORITY và 3 cờ BLOCK-TOGGLE vào pipeline quyết định chặn (thứ tự: BLOCK-02 blocklist → CONTACTS-PRIORITY exception → BLOCK-TOGGLE → SCAM-DETECT-01/05); xác định cách phát hiện số ẩn danh/quốc tế từ `CallScreeningService`/Telecom API; thiết kế cơ chế `WorkManager` periodic cho PERMISSION-CHECK (tần suất mỗi ngày, tương thích App Standby/Doze, cách kiểm tra từng loại quyền qua API Android tương ứng).
  - `docs/design/ERD-SafeCall-Phase1.md` — **ảnh hưởng lớn nhất**: bảng `blocklist` cần thêm cột `entry_type` (exact/prefix), xem lại UNIQUE constraint theo từng loại; cần thêm nơi lưu setting CONTACTS-PRIORITY và 3 cờ BLOCK-TOGGLE (bảng/preferences key); CALLHISTORY cần thêm lý do chặn mới (contact-priority-bypass, block-toggle-unknown/anonymous/international); cần thêm nơi lưu trạng thái quyền đã thông báo (để biết quyền nào đang thiếu, tránh gửi trùng thông báo trong cùng chu kỳ).
  - `docs/design/UIUX-SafeCall-Phase1.md` — màn hình quản lý blocklist thêm lựa chọn loại entry, UI Export/Import; thêm màn hình Cài đặt cho toggle CONTACTS-PRIORITY và 3 công tắc BLOCK-TOGGLE; thêm nội dung thông báo nhắc cấp lại quyền (PERMISSION-CHECK) và màn hình đích khi người dùng nhấn vào thông báo; thêm quyền Overlay vào luồng onboarding.
  - `docs/test/TestPlan-SafeCall-Phase1.md` — test case cho ranh giới prefix match, export/import round-trip, file corrupt, schema version; test case cho CONTACTS-PRIORITY BẬT/TẮT với contact khớp exact vs prefix; test case cho từng công tắc BLOCK-TOGGLE (BẬT/TẮT, miễn trừ contact, khớp nhiều điều kiện cùng lúc); test case cho PERMISSION-CHECK (giả lập thu hồi quyền, xác nhận thông báo lặp lại đúng chu kỳ, dừng thông báo khi cấp lại).
  - `docs/planning/estimate.md`, `docs/planning/WBS-SafeCall-Phase1.md` — thêm effort.
  - `docs/adr/ADR-003-local-only-blocklist.md` — bổ sung ghi chú: export/import thủ công không mâu thuẫn với local-only, nhưng cập nhật lại phần Consequences về privacy khi export.
  - **Quyết định "ngoài phạm vi" cũ về số ẩn danh (ghi trong `docs/srs/SRS-SafeCall-Phase1.md` Phần 5.2 và ADR liên quan nếu có) cần được rà soát lại** — không còn hoàn toàn đúng sau khi BLOCK-TOGGLE đưa việc chặn thẳng số ẩn danh vào phạm vi Phase 1 (qua công tắc riêng, không qua matching exact/prefix).
  - **Câu hỏi mở trước đây về sự cần thiết của quyền Overlay cho SCAM-DETECT-05 (SRS Phần 7.1.1) nay đã được xác nhận là CẦN THIẾT** (quyết định #11) — cần `system-architect` xác nhận tiếp tính khả thi kỹ thuật triển khai (không còn là câu hỏi về việc có cần quyền này hay không).
