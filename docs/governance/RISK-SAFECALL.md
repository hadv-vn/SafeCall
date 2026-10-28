# Risk Register — SAFECALL

> Theo Workbook EX-04. ≥10 rủi ro, mỗi rủi ro: Likelihood (1–5) · Impact (1–5) · điểm L×I · mitigation khả thi
> (không sáo rỗng) · owner. Nhóm rủi ro-AI riêng có thêm cột "mức ủy quyền AI" + "cổng fail-closed" — vì
> mitigation cho rủi ro-AI phải là **cơ chế kiểm được**, không phải lời hứa.

## Bảng rủi ro đầy đủ

| # | Rủi ro | Nhóm | L (1-5) | I (1-5) | L×I | Mitigation | Owner |
|---|---|---|---|---|---|---|---|
| R1 | Độ tin cậy phát hiện cuộc gọi kết thúc/đang đổ chuông khi app chạy background khác nhau giữa API 29 (`PhoneStateListener`) và API 31+ (`TelephonyCallback`) — NFR-4, câu hỏi mở SRS 7.1 | Kỹ thuật | 4 | 4 | 16 | Giữ 2 implementation riêng (`PhoneStateListenerObserver`/`TelephonyCallbackObserver`) đã code; **chưa có** test hồi quy tự động trên cả 2 API level — cần bổ sung CI matrix hoặc test tay bắt buộc mỗi API level trước release | Dev lead |
| R2 | Overlay lúc đổ chuông (FR-8) không hiển thị đúng lúc/bị OEM custom ROM (Samsung One UI, Xiaomi HyperOS, Oppo ColorOS) chặn `SYSTEM_ALERT_WINDOW` hoặc throttle background | Kỹ thuật/Tương thích | 4 | 5 | 20 | WBS Task 6.5.3 đã định nghĩa ma trận BẮT BUỘC 3 thiết bị OEM khác nhau trước khi coi Phase 6 là Done — **chưa thực hiện** (nằm trong nhóm "không doable bởi agent" theo memory dự án) | QA/PM |
| R3 | Tính khả thi kỹ thuật `TelecomManager.endCall()` chỉ với `ANSWER_PHONE_CALLS` + `ROLE_CALL_SCREENING`, không cần vai trò Dialer mặc định — SRS 7.1.1 tự ghi "input giả định khả thi nhưng CHƯA được `system-architect` xác nhận" | Kỹ thuật | 2 | 5 | 10 | ADR-005 ghi nhận single call-site `attemptEndCall`; đã verify được trên 1 emulator (API 34) nhưng **chưa verify trên thiết bị thật đa OEM** — rủi ro còn treo đúng như SRS đã tự cảnh báo | Dev lead |
| R4 | Race condition giữa prompt sau-cuộc-gọi (FR-2/POST_CALL) và overlay lúc-đổ-chuông (FR-8/RINGING) cho cùng một số — cơ chế `PendingPromptRegistry`/`resolvePromptAtomically` (first-resolution-wins) | Kỹ thuật | 3 | 4 | 12 | Thiết kế atomic 2 lớp (in-memory CAS + DB `UPDATE WHERE resolved=0`) đã code; WBS Task 6.5.2/4.4.2 yêu cầu integration test race ≥20 lần lặp — **chưa chạy** (chỉ build+test tay UI, chưa test race có kiểm soát) | Dev lead |
| R5 | Chặn nhầm nhiều số không liên quan khi thêm đầu số (prefix) quá ngắn — FR-9, đã khuếch đại rủi ro nêu ở NFR-3 | Nghiệp vụ/UX | 3 | 3 | 9 | Ngưỡng tối thiểu 4 ký tự số (FR-9/AC2) + cảnh báo khi khớp contact (FR-9/AC5) — **nhưng ngưỡng 4 là mặc định chưa xác nhận với stakeholder** (SRS 7.1 tự ghi rõ) | PM |
| R6 | Hiệu năng tra cứu matching (exact+prefix) suy giảm khi blocklist lớn (tăng nhanh sau import FR-10) vượt ngân sách hot-path ~1500ms | Kỹ thuật/Hiệu năng | 2 | 5 | 10 | `BlocklistPrefixIndex` (trie in-memory) đã code theo đúng LLD; WBS Task 7.7.3 yêu cầu sanity-check BẮT BUỘC với 10.000 entry PREFIX trước khi coi Phase 7 Done — **chưa chạy** | Dev lead |
| R7 | Dữ liệu cá nhân bên thứ 3 (số điện thoại người gọi) bị truyền ra ngoài thiết bị khi Export (FR-10) qua Share Sheet do người dùng tự chọn đích — SRS tự đánh dấu "cần rà soát lại ADR-003" | Dữ liệu/Pháp lý | 3 | 3 | 9 | File export không mã hoá (quyết định đã chốt cho Phase 1, chấp nhận là nợ rủi ro Phase 1); **ADR-003 ĐÃ được cập nhật từ 2026-10-21** (mục "Cập nhật 2026-10-21") ghi nhận đầy đủ ngoại lệ này + lý do không mã hoá — **sửa 2026-10-25: dòng này trước đó ghi sai "chưa cập nhật", đã verify lại file `ADR-003-local-only-blocklist.md` thật và xác nhận gap tài liệu KHÔNG còn tồn tại** | PM |
| R8 | Import bị gián đoạn giữa chừng (crash/force-close) để lại blocklist ở trạng thái "nửa vời" — FR-10/AC10 | Dữ liệu | 2 | 4 | 8 | `mergeEntriesInTransaction` (ACID qua transaction) đã code theo LLD; WBS Task 7.7.5 yêu cầu test thiết bị thật mô phỏng crash 3-5 lần — **chưa chạy** | Dev lead |
| R9 | Lệch kết quả `PhoneNumberNormalizer` giữa Dart và Kotlin (hai bản song song, phải cho output giống nhau tuyệt đối để UNIQUE constraint đúng) — rủi ro tự `estimate.md` nêu rõ | Kỹ thuật/Dữ liệu | 2 | 4 | 8 | 🟡 **Đang đóng (2026-10-25):** đã viết golden-file test 58 case (đầu số thật Viettel/Mobifone/VinaPhone/Vietnamobile/Gmobile/iTelecom) tại `test/fixtures/phone_normalizer_golden.json` + `test/blocklist/phone_number_normalizer_golden_test.dart` (Dart) + `android/app/src/test/kotlin/.../PhoneNumberNormalizerGoldenTest.kt` (Kotlin) — tự verify bằng oracle Python độc lập, 58/58 khớp. **CHƯA đóng hẳn:** chưa chạy qua `flutter test`/`gradle test` thật (môi trường AI không có 2 toolchain này) — L hạ từ 3→2 vì test đã sẵn sàng bắt lệch nếu có, nhưng chưa có bằng chứng thực thi | Dev lead |
| R10 | Tiến độ: 2 vòng "fix phát sinh" có worst-case rất cao bị ẩn trong WBS (Task 6.5.4 worst 6 MD nếu OEM fail AC5/AC10; Task 7.7.4 worst 4 MD nếu performance sanity-check fail) — có thể đội effort đáng kể nếu cả 2 đều fail | Tiến độ | 3 | 3 | 9 | Buffer đã có trong PERT-weighted (~151.5 MD) nhưng **chưa trade-off lại** nếu cả 2 vòng fix đều kích hoạt cùng lúc — cần theo dõi khi vào giai đoạn test thiết bị thật | PM |

## Nhóm rủi ro AI-sinh (bắt buộc theo EX-04 §2)

> Mitigation ở đây PHẢI là cổng kiểm được, không phải lời hứa "sẽ cẩn thận hơn".

| # | Rủi ro AI | L (1-5) | I (1-5) | L×I | Mức ủy quyền AI được phép | Cổng fail-closed | Owner |
|---|---|---|---|---|---|---|---|
| R11 | AI viết toàn bộ code Phase 1-8 theo lựa chọn "viết trước, không verify build" — nhiều lỗi (dependency version, framework API, logic PRAGMA) chỉ lộ ra khi build/test thật, không phải lúc viết | 4 | 4 | 16 | **L3/Leash A tối đa** cho việc viết code mới — KHÔNG được tự coi "Done" trước khi build thật chạy xanh. Đã xảy ra đúng như dự đoán — xem `DECISION-LOG-SAFECALL.md` khối #1-#4 | Build/compile PASS + chạy thật trên emulator/thiết bị trước khi đánh dấu Done cho bất kỳ Phase nào | Dev lead |
| R12 | AI tự suy luận lại pseudocode của 4 hàm lõi khi phát hiện LLD v1.3 bị mất nội dung gốc, thay vì DỪNG hỏi người dùng/designer — vi phạm nguyên tắc hard-stop | 2 | 5 | 10 | **L1/Draft tối đa** khi phát hiện tài liệu input bị thiếu/mất ở mức "logic lõi" — PHẢI dừng và báo, không tự đoán. Đã vi phạm — xem `DECISION-LOG-SAFECALL.md` khối #5 | Trước khi triển khai code dựa trên pseudocode tự suy luận: bắt buộc người dùng/`low-level-designer` xác nhận lại logic khớp ý định gốc | PM + Dev lead |
| R13 | Rubber-stamping ở tầng governance: vì Human-edit = 0 trong toàn giai đoạn build, nếu không có Decision Log/Delegation Map retroactive, các gap thật (permission/manifest và DB-layer chạy ở Leash A trong khi đáng lẽ A+) sẽ không ai phát hiện | 2 | 4 | 8 | 🟡 **Giảm 2026-10-25:** 3 gap cụ thể (1.1.2/1.3.1/4.1.1) đã được **người dùng tự xem bằng chứng kỹ thuật thật rồi ký duyệt** qua `AskUserQuestion` (xem `DELEGATION-MAP-SAFECALL.md` mục "Sign-off retroactive") — không còn là rủi ro "chưa ai phát hiện" cho 3 task NÀY. L giảm 3→2 vì đây chỉ là bằng chứng cho 3 task đã xảy ra, KHÔNG phải cơ chế tự động chặn cho Task tương lai — cơ chế phòng ngừa (flag A+ TRƯỚC build) vẫn chưa tồn tại | Mọi task chạm permission/manifest hoặc DB schema (xem `DELEGATION-MAP-SAFECALL.md`, `MODULEMAP-SAFECALL.md`) phải được flag A+ và có người duyệt TRƯỚC khi coi Done, không phải sau khi crash mới phát hiện | PM |
| R14 | Dữ liệu nhạy cảm (số điện thoại thật, nội dung logcat thiết bị thật) có thể vô tình bị dán vào prompt AI trong lúc debug nếu dùng thiết bị thật thay vì emulator ở các vòng test OEM sắp tới | 2 | 4 | 8 | **L0/Observe** cho bất kỳ dữ liệu từ thiết bị thật của người dùng thực — AI chỉ được xem dữ liệu đã ẩn danh/giả lập (emulator, số test) | Trước khi dán log/dữ liệu từ thiết bị thật vào prompt: phải tự kiểm tra không còn số điện thoại/thông tin cá nhân thật nào trong đó | Dev lead |

## Top-5 ưu tiên (theo điểm L×I)

1. **R2 (20)** — Overlay lúc đổ chuông chưa test đa OEM thật.
2. **R1 (16)** — Độ tin cậy phát hiện trạng thái cuộc gọi khác nhau giữa API 29/31+, chưa có test hồi quy.
3. **R11 (16)** — Hệ quả trực tiếp của quyết định "viết trước, không verify" — đã xảy ra, cần gate chặt hơn cho các Phase còn lại (test call-flow thật qua `adb emu gsm call` chưa làm).
4. **R4 (12)** — Race condition prompt chưa test có kiểm soát.
5. **R3 / R6 / R12 (10, đồng điểm 3 chiều)** — lần lượt: `endCall()` chưa verify đa OEM; hiệu năng matching chưa sanity-check; AI từng tự suy luận pseudocode thay vì dừng hỏi (hard-stop vi phạm).

> *(R9 rời khỏi Top-5 sau cập nhật 2026-10-25: điểm giảm 12→8 nhờ golden-file test đã viết. R13 rời khỏi Top-5
> sau cập nhật 2026-10-25 lần 2: điểm giảm 12→8 sau khi 3 gap governance cụ thể được người dùng ký duyệt
> retroactive — xem mục "Nhóm rủi ro AI-sinh" ở trên. Cả hai vẫn còn trong Risk Register, chỉ không còn Top-5;
> vị trí #5 nay là 1 bộ 3 rủi ro đồng điểm 10, không phải 1 rủi ro đơn — ghi đúng thực tế thay vì chọn tuỳ ý
> 1 trong 3 để có đúng "Top-5" gọn gàng.)*

## Ghi chú trung thực

- Không có rủi ro nào ở bảng trên được đo bằng số liệu thật (ví dụ tỷ lệ crash thực tế) — toàn bộ Likelihood/Impact là đánh giá định tính dựa trên mức độ hoàn thành thật của các WBS Task kiểm thử tương ứng (nhiều Task kiểm thử quan trọng — 6.5.3, 7.7.3, 7.7.5, 4.4.2 — **ghi "chưa chạy"** vì thực tế mới chỉ build+test tay UI, chưa chạm luồng cuộc gọi thật).
- R11, R12, R13 không phải rủi ro giả định — cả ba đã **thật sự xảy ra** trong phiên build vừa qua (xem `DECISION-LOG-SAFECALL.md`), không phải suy đoán phòng ngừa.
- **Cập nhật 2026-10-25:** R9 là rủi ro đầu tiên trong bảng có mitigation tiến triển thật (test đã viết) kể từ khi Risk Register này được tạo — không chỉ dừng ở mô tả "cần làm X". Vẫn giữ nguyên tắc không hạ điểm về 0 cho tới khi có bằng chứng thực thi qua toolchain thật (`flutter test`/`gradle test`), tránh lặp lại kiểu rubber-stamp mà R13 đang cảnh báo.
- **Sửa lỗi dữ liệu sai 2026-10-25 (R7):** mitigation của R7 từng ghi "ADR-003 chưa được cập nhật" — kiểm lại file `docs/adr/ADR-003-local-only-blocklist.md` thật thì ADR-003 đã có mục cập nhật đầy đủ cho ngoại lệ Export từ **2026-10-21**, trước cả khi dòng R7 này được viết. Đây là lỗi tự thân của Risk Register (khẳng định một gap tài liệu mà không mở file nguồn ra kiểm), không phải lỗi của ADR-003 — đã sửa lại mitigation ở bảng trên cho khớp sự thật.
