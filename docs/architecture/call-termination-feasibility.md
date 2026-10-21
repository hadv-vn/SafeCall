# Xác nhận khả thi kỹ thuật: Ngắt cuộc gọi đang đổ chuông qua TelecomManager.endCall()
(SCAM-DETECT-05 / FR-8 — input cho ADR và low-level-designer)

## Kết luận chung
KHẢ THI, với các điều kiện/ràng buộc cụ thể nêu ở mục cuối. Giả định trong `scam-call-blocking.md` (dùng `TelecomManager.endCall()` + quyền runtime `ANSWER_PHONE_CALLS`, KHÔNG cần default dialer/`ROLE_DIALER`) là ĐÚNG về mặt API hợp đồng công khai của Android — nhưng có giới hạn version, giới hạn ngữ nghĩa "foreground call" (không có call-ID), và cần được thiết kế để xử lý trung thực các trường hợp race/thất bại, thay vì giả định "ngắt luôn thành công".

## 1. TelecomManager.endCall() với quyền ANSWER_PHONE_CALLS — giới hạn version

- `TelecomManager.endCall()` là API public: `@RequiresPermission(anyOf = {ANSWER_PHONE_CALLS, MODIFY_PHONE_STATE})`. Khác với `acceptRingingCall()` (yêu cầu `MODIFY_PHONE_STATE` — quyền signature-level, hoặc app phải là default dialer) — `endCall()` KHÔNG ràng buộc default dialer trong contract public, xác nhận đúng giả định của requirements.
- `ANSWER_PHONE_CALLS` là quyền runtime (dangerous, nhóm PHONE), có từ API 26 (Android 8.0) — xin qua luồng runtime permission thông thường, không cần `RoleManager`/`ROLE_DIALER`.
- **Giới hạn version quan trọng**: method `endCall()` chỉ có từ **API 28 (Android 9)** trở lên. Baseline test hiện tại (API 29 và API 31+) đều an toàn (trên ngưỡng 28), nhưng `minSdkVersion` thực tế của project chưa được khai báo ở đâu — nếu thấp hơn 28, cần feature-gate theo `Build.VERSION.SDK_INT >= 28`, dưới ngưỡng đó áp dụng nhánh fallback đã có trong AC (hiển thị overlay nhưng không ngắt được).
- **Giới hạn ngữ nghĩa cố hữu**: `endCall()` KHÔNG nhận tham số call-ID — theo javadoc: "nếu có cuộc gọi đang đổ chuông, ngắt cuộc gọi đó; nếu không, kết thúc cuộc gọi đang ở foreground". Phù hợp use case chính (1 cuộc gọi đến, không có cuộc gọi khác), nhưng hành vi theo tài liệu, chưa kiểm chứng thực nghiệm trên nhiều OEM (Samsung/Xiaomi/Oppo có telecom stack tùy biến) — cần test trên thiết bị thật, không chỉ emulator.

## 2. Tương tác với CallScreeningService đã có
Không xung đột — hai cơ chế loại trừ lẫn nhau tự nhiên theo giai đoạn: `CallScreeningService` can thiệp TRƯỚC khi đổ chuông (nếu đã `disallowCall=true`, cuộc gọi không bao giờ đạt RINGING, overlay mới không kích hoạt). Overlay/`endCall()` chỉ tác động SAU khi đã cho qua. Cả hai đọc chung `BlocklistRepository` nên tính loại trừ được đảm bảo bởi invariant dữ liệu, không cần kênh đồng bộ runtime mới.

## 3. Rủi ro race khi endCall() được gọi đúng lúc trạng thái cuộc gọi đổi
- **Cuộc gọi tự kết thúc trước khi lệnh tới**: `endCall()` trả `false` vô hại, không crash. Cần tách riêng outcome: ghi blocklist luôn idempotent-thành công, nhưng "ngắt cuộc gọi" có thể không còn áp dụng — không mặc định hiển thị "Đã chặn" như ngắt thành công.
- **Người dùng tự bắt máy đúng lúc gửi lệnh ngắt**: rủi ro UX nghiêm trọng — cần tái xác thực trạng thái cuộc gọi (vẫn RINGING, đúng số) ngay TRƯỚC khi gọi `endCall()`; nếu trạng thái đã đổi, KHÔNG gọi nữa.
- **Call-waiting/nhiều cuộc gọi đồng thời**: không có call-ID nên không đảm bảo chính xác tuyệt đối — giới hạn cố hữu của API công khai (độ chính xác theo call-ID cần `InCallService`/default dialer — đúng điều muốn tránh). Chấp nhận là giới hạn đã biết của Phase 1, ghi rõ trong ADR.
- Hướng xử lý kiến trúc (chi tiết thuộc `low-level-designer`): (a) tái xác thực trạng thái+số ngay trước `endCall()`; (b) kiểm tra giá trị trả về, theo dõi trạng thái sau đó để ghi outcome thật vào `call_history` (mở rộng `ResolveResult` với outcome mới cho nhánh ngắt gọi); (c) mở rộng `PendingPromptRegistry`/`resolvePromptAtomically` để coi "cuộc gọi tự đổi trạng thái" là một race participant ngang hàng với "user tap Có".

## 4. Thành phần chịu trách nhiệm gọi endCall()
`CallMonitorService` (đã có trong HLD/LLD, độc lập vòng đời Flutter engine, sở hữu `PendingPromptRegistry`) nên sở hữu logic này — không phải Flutter/Dart layer. Lý do: theo ADR-001/LLD, "đường đảm bảo" cho hành động an toàn-quan trọng luôn native-to-native; hành động ngắt cuộc gọi không thể hoàn tác nên không nên phụ thuộc Flutter engine còn sống. Overlay mới (`RingingScamOverlay`, native — Window overlay hoặc full-screen-intent Notification) khi người dùng chọn "Có" nên forward vào cùng điểm xử lý atomic trong `CallMonitorService` (mở rộng `resolvePromptAtomically`) để vừa ghi blocklist vừa gọi `endCall()` trong cùng luồng được bảo vệ race.

## 5. Rủi ro bảo mật/quyền lực
- App không phải default dialer nhưng có thể đơn phương ngắt bất kỳ cuộc gọi đang đổ chuông nào — bug (match sai số, race ở mục 3) có thể cắt một cuộc gọi hợp lệ, KHÔNG THỂ HOÀN TÁC, không có cách người dùng can thiệp kịp — mức nghiêm trọng cao hơn hẳn bug thông thường.
- `ANSWER_PHONE_CALLS` không có quyền hẹp hơn — một khi cấp, mọi đoạn code gọi `endCall()` đều có hiệu lực. Nên giới hạn: **chỉ MỘT điểm gọi `endCall()` duy nhất** trong toàn bộ codebase (qua `CallMonitorService`), dễ review/audit.
- `ScamConfirmationReceiver` nhận thêm trách nhiệm kích hoạt `endCall()` → mức độ tin cậy tăng — cần `security-auditor` review cấu hình `exported`/intent-filter khi code tồn tại, tránh app khác lợi dụng để DoS cục bộ.

## Điều kiện/ràng buộc cần ADR ghi lại
1. Xác nhận `minSdkVersion` ≥ 28, hoặc chốt feature-gate theo `SDK_INT` cho riêng hành vi ngắt cuộc gọi.
2. Chấp nhận rõ ràng giới hạn "không call-ID, chỉ tác động foreground call" — call-waiting không được đảm bảo — là rủi ro còn tồn đọng đã biết của Phase 1.
3. Chốt nguyên tắc "một điểm gọi `endCall()` duy nhất" qua `CallMonitorService` — ranh giới kiến trúc, không phân tán logic.
4. Chốt yêu cầu tái xác thực trạng thái cuộc gọi ngay trước khi thực thi `endCall()`, ghi outcome thật (không giả định luôn thành công) vào `call_history`.
5. Cần test trên thiết bị thật (không chỉ emulator) cho `endCall()` do phụ thuộc telecom stack theo OEM — bổ sung vào Test Plan.

## Ngoài phạm vi phân tích này
- Thiết kế class/sequence/pseudocode chi tiết cho luồng ngắt cuộc gọi — thuộc `low-level-designer`.
- Cập nhật SRS/HLD/LLD/ERD/UIUX/TestPlan/WBS theo cascade — thực hiện sau khi ADR được chốt.
- Review cấu hình exported/intent-filter thật của `ScamConfirmationReceiver` sau khi code tồn tại — thuộc `security-auditor`.
