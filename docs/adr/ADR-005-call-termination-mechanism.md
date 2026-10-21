# ADR-005: Dùng TelecomManager.endCall() + quyền ANSWER_PHONE_CALLS để ngắt cuộc gọi đang đổ chuông (SCAM-DETECT-05/FR-8)

| | |
|---|---|
| **Ngày** | 2026-10-21 |
| **Trạng thái** | Accepted |

## Context

SCAM-DETECT-05 (bổ sung sau khi các tài liệu requirements ban đầu đã viết, xem `docs/requirements/scam-call-blocking.md`, quyết định đã chốt #5 và #6) yêu cầu: khi người dùng xác nhận "Có, đây là lừa đảo" trong lúc một cuộc gọi từ số lạ đang đổ chuông (chưa bắt máy), số đó phải được thêm vào blocklist NGAY LẬP TỨC (giống SCAM-DETECT-02), VÀ cuộc gọi đang đổ chuông đó phải bị ngắt/từ chối ngay lập tức — không chỉ ảnh hưởng tới các lần gọi sau. Đây là tùy chọn thêm song song với luồng hỏi xác nhận sau cuộc gọi (SCAM-DETECT-01/02/03), không thay thế luồng đó.

Yêu cầu đặt ra giả định kỹ thuật cần xác nhận: dùng `TelecomManager.endCall()` với quyền runtime `ANSWER_PHONE_CALLS` (quyền mới so với kế hoạch ban đầu), KHÔNG cần giữ vai trò default dialer (`ROLE_DIALER`) của thiết bị — vì trở thành default dialer là một thay đổi hành vi lớn, xâm lấn, và không nhất quán với mô hình `CallScreeningService` hiện có của app (ADR-001, ADR-002). `system-architect` đã phân tích khả thi (`docs/architecture/call-termination-feasibility.md`) và xác nhận:

- Giả định ĐÚNG về mặt hợp đồng API công khai: `endCall()` được khai báo `@RequiresPermission(anyOf = {ANSWER_PHONE_CALLS, MODIFY_PHONE_STATE})`, không ràng buộc default dialer — khác với `acceptRingingCall()` vốn đòi `MODIFY_PHONE_STATE` (quyền signature-level hoặc phải là default dialer).
- **Giới hạn version**: `endCall()` chỉ khả dụng từ API 28 (Android 9) trở lên, dù `ANSWER_PHONE_CALLS` tự nó có từ API 26.
- **Giới hạn ngữ nghĩa cố hữu**: `endCall()` không nhận call-ID — theo javadoc, nó ngắt cuộc gọi đang đổ chuông nếu có, nếu không thì kết thúc cuộc gọi đang ở foreground. Phù hợp use case chính (một cuộc gọi đến, không có cuộc gọi khác), nhưng không đảm bảo đúng tuyệt đối khi có call-waiting/nhiều cuộc gọi đồng thời — hành vi theo tài liệu, chưa kiểm chứng thực nghiệm trên nhiều telecom stack OEM (Samsung/Xiaomi/Oppo...).
- **Rủi ro race**: cuộc gọi có thể tự kết thúc hoặc được người dùng tự bắt máy đúng lúc lệnh ngắt được gửi đi — cần tái xác thực trạng thái cuộc gọi (vẫn đang RINGING, đúng số) ngay TRƯỚC khi gọi `endCall()`, và ghi nhận outcome thật (không giả định luôn ngắt thành công) vào `call_history`.
- **Rủi ro quyền lực/bảo mật**: `ANSWER_PHONE_CALLS` không có phạm vi hẹp hơn — một khi cấp, mọi đoạn code gọi `endCall()` đều có hiệu lực, và app có thể đơn phương ngắt một cuộc gọi hợp lệ do bug (match sai số, race) mà không có cách người dùng can thiệp kịp — mức nghiêm trọng cao hơn bug thông thường vì hành động không thể hoàn tác. Khuyến nghị giới hạn đúng MỘT điểm gọi `endCall()` trong toàn bộ codebase để dễ audit.
- Cần test trên thiết bị thật (không chỉ emulator) vì hành vi phụ thuộc telecom stack theo OEM.

**Phương án thay thế đã xem xét và bị loại**: dùng `acceptRingingCall()`/trở thành default dialer (`ROLE_DIALER`) để có quyền điều khiển cuộc gọi đầy đủ hơn (kể cả call-ID chính xác qua `InCallService`) — bị loại vì đòi hỏi quyền signature-level hoặc vai trò default dialer, xâm lấn hơn hẳn mô hình `CallScreeningService` hiện tại của SafeCall (ADR-001/ADR-002), tăng rào cản cấp quyền/onboarding, và đi ngược định hướng "không cần giữ vai trò default dialer" đã được stakeholder xác nhận trong requirements.

## Decision

Dùng `TelecomManager.endCall()` kèm quyền runtime `ANSWER_PHONE_CALLS` để ngắt cuộc gọi đang đổ chuông khi người dùng xác nhận "Có, đây là lừa đảo" trong overlay lúc đổ chuông (SCAM-DETECT-05). Không yêu cầu vai trò default dialer (`ROLE_DIALER`).

Chỉ `CallMonitorService` (native Kotlin, độc lập vòng đời Flutter engine) được phép gọi API này trong toàn bộ codebase — không gọi `endCall()` qua `MethodChannel`/từ phía Flutter/Dart.

Chấp nhận giới hạn không có call-ID (không đảm bảo chính xác tuyệt đối khi có call-waiting/nhiều cuộc gọi đồng thời) như một rủi ro đã biết, còn tồn đọng của Phase 1 — không giải quyết trong phạm vi quyết định này.

`minSdkVersion` của project phải ≥ 28 (Android 9), hoặc nếu thấp hơn, phải feature-gate hành vi ngắt cuộc gọi theo `Build.VERSION.SDK_INT >= 28` (dưới ngưỡng đó, overlay vẫn hiển thị nhưng không thực hiện ngắt cuộc gọi, theo nhánh fallback đã mô tả trong AC của SCAM-DETECT-05).

## Consequences

**Tích cực:**
- Đáp ứng đúng yêu cầu SCAM-DETECT-05: ngắt cuộc gọi đang đổ chuông ngay khi người dùng xác nhận, không cần chờ tới lần gọi sau.
- Không cần trở thành default dialer — giữ nguyên mô hình ít xâm lấn hơn (`CallScreeningService` + quyền runtime) đã thiết lập từ ADR-001/ADR-002, tránh tăng thêm rào cản onboarding lớn (xin vai trò default dialer phức tạp và đáng ngại hơn nhiều với người dùng so với một quyền runtime đơn lẻ).
- Giới hạn "một điểm gọi `endCall()` duy nhất" qua `CallMonitorService` giữ nhất quán với nguyên tắc kiến trúc "đường đảm bảo native-to-native cho hành động an toàn-quan trọng" đã có trong ADR-001, dễ audit/review hơn nếu logic bị phân tán.

**Tiêu cực / trade-off:**
- Thêm một quyền nhạy cảm mới (`ANSWER_PHONE_CALLS`) vào luồng onboarding (ONBOARD-01), cần giải thích rõ lý do trước khi mở dialog hệ thống — tăng số lượng quyền người dùng phải cân nhắc cấp.
- Hành động ngắt cuộc gọi là KHÔNG THỂ HOÀN TÁC (khác với thêm vào blocklist, có thể xóa sau nếu nhấn nhầm) — bắt buộc phải tái xác thực trạng thái cuộc gọi (đúng số, vẫn đang RINGING) ngay trước khi gọi `endCall()`, và phải ghi nhận outcome thật sự của lệnh ngắt (thành công/thất bại/không còn áp dụng do cuộc gọi đã tự đổi trạng thái) vào `call_history` — không được giả định mặc định là luôn thành công.
- Tăng bề mặt rủi ro bảo mật: app có thể đơn phương ngắt một cuộc gọi hợp lệ nếu có bug match sai số hoặc thua trong race điều kiện. `ScamConfirmationReceiver` và `CallMonitorService` cần được `security-auditor` review kỹ cấu hình `exported`/intent-filter khi có code thật, để tránh app khác lợi dụng kích hoạt hành vi này (ví dụ DoS cục bộ bằng cách giả broadcast xác nhận).
- Giới hạn cố hữu "không có call-ID, chỉ tác động cuộc gọi đang đổ chuông/foreground" — không đảm bảo đúng tuyệt đối khi có call-waiting/nhiều cuộc gọi đồng thời. Đây là nợ kỹ thuật/rủi ro đã biết, CHƯA được giải quyết trong Phase 1 (muốn giải quyết triệt để cần call-ID qua `InCallService`/default dialer — phương án đã bị loại ở Context).
- Ràng buộc version: `minSdkVersion` phải ≥ 28, hoặc cần thêm logic feature-gate theo `SDK_INT` riêng cho nhánh ngắt cuộc gọi — tăng độ phức tạp code nếu project cần hỗ trợ thiết bị Android 8.x trở xuống.
- Bắt buộc có vòng test trên thiết bị thật (không chỉ emulator) trước khi release, vì hành vi `endCall()` phụ thuộc telecom stack tùy biến theo OEM — phát sinh thêm effort/thời gian QA so với test thuần trên emulator.
- Logic ngắt cuộc gọi phải sống trong `CallMonitorService` và dùng chung cơ chế atomic resolve (mở rộng `resolvePromptAtomically`/`PendingPromptRegistry`) với nhánh ghi blocklist — tăng độ phức tạp của thành phần native so với chỉ ghi dữ liệu đơn thuần như trước.

## Cascade cần cập nhật (chưa thực hiện trong ADR này)

Theo cảnh báo trong `docs/requirements/scam-call-blocking.md`, các tài liệu sau chưa phản ánh SCAM-DETECT-05/quyết định này và cần rà soát/cập nhật riêng, ngoài phạm vi ADR: SRS, HLD, LLD, ERD, UI/UX, Test Plan, estimate/WBS.
