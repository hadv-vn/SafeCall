# Weekly Report — SAFECALL

> Theo Workbook EX-05. **Ghi chú trung thực:** dự án này không chạy theo sprint/tuần thật — build dồn trong
> 1 đợt (2026-10-20 → 2026-10-25). Report này là **snapshot trạng thái tại thời điểm hiện tại**, viết theo
> đúng khung "Weekly Report" của chương trình, không phải báo cáo tuần thật có nhịp lặp lại.

## Tiến độ: Kế hoạch vs Thực tế

| | Kế hoạch (theo `WBS-SafeCall-Phase1.md`) | Thực tế |
|---|---|---|
| Thiết kế (bước 0-6 SDLC) | SRS, ADR, HLD, LLD, ERD, UIUX, Test Plan, estimate, WBS | ✅ Đủ cả 9 tài liệu |
| Code Phase 1-8 | 8 Phase, ~151.5 person-day PERT-weighted | ✅ Code đã viết đủ 8 Phase (22 file Dart + 32 file Kotlin) |
| Build + demo | Build xanh, cài chạy được | ✅ Build thành công, cài & test tay trên `SafeCall_API34` (emulator). **Tái xác nhận 2026-10-25 (lần 2):** rebuild sau khi bump AGP/Kotlin/compileSdk — vẫn xanh, cài lại trên `emulator-5554`, không regression |
| Test System/E2E thật (8.1) | Script `adb emu gsm call`, test SIM thật | ❌ Chưa chạy |
| Test đa OEM (6.5.3/6.5.4) | Samsung/Xiaomi/Oppo | ❌ Chưa làm — cần thiết bị vật lý |
| Sanity-check hiệu năng (7.7.3) | 10.000 entry prefix | ❌ Chưa chạy |
| Security review + UAT (8.2/8.3) | Người đích thân duyệt | ❌ Chưa làm |
| Release signing | signingConfig thật | 🟡 Đã dựng cơ chế gradle (`RELEASE-SIGNING-SAFECALL.md`), chờ người dùng tự tạo keystore |
| Golden-file test `PhoneNumberNormalizer` (R9) | Đối chiếu Dart ↔ Kotlin, 58 case | ✅ **Đóng hẳn 2026-11-08** — cả `gradle testDebugUnitTest` (Kotlin) và `flutter test` (Dart, 64/64) đã chạy thật và PASS. R9 → L×I=0 |
| FR-11 (Contacts Priority) + FR-12 (Block-Toggle) | Pipeline + Settings UI (Phase 9/10) | 🟡 Code+UI xong (`2efcc3d`, `78691bd`), unit-test (mock) PASS (20 test), **chưa verify tay qua emulator** |
| FR-13 (Permission-Check) | Worker định kỳ 24h + notification (Phase 11) | 🟡 Code xong (`79daea2`/`7eefd83`), unit-test PASS (6 test), **chưa verify `WorkManager` chạy thật** |
| Hành vi chặn cuộc gọi thật (FR-1/FR-9) | Mô phỏng qua `adb emu gsm call` | ✅ **Lần đầu verify 2026-11-08** (`4fe7e62`) — EXACT/PREFIX blocking xác nhận hoạt động đúng; nhưng phát hiện bug fail-open nghiêm trọng ẩn từ 2026-10-23, xem Decision Log #6 |

## Điểm nghẽn (bottleneck)

1. **Không có test code tự động nào đã CHẠY thật** (58 case golden-file đã viết nhưng chưa qua `flutter test`/`gradle test`) — mọi xác nhận "chạy đúng" hiện dựa 100% vào test tay, vẫn chỉ 1 emulator (`SafeCall_API34`/`emulator-5554`). **Cập nhật 2026-10-25 (lần 2):** đã có lượt test tay thứ 2 (rebuild + cài lại + thêm 1 entry PREFIX qua UI, đóng một phần gap FR-9 — xem `RTM-SAFECALL.md`), nhưng vẫn là test tay thủ công, chưa phải tự động.
2. **4 vòng test kỹ thuật quan trọng chưa chạy** (race condition 4.4.2, hiệu năng 7.7.3, crash-giữa-import 7.7.5, system/E2E 8.1) — đều là test có thể làm trên emulator, không bị chặn bởi thiết bị vật lý.
3. ~~2 gap governance đã xảy ra thật~~ — **đã ký duyệt retroactive 2026-10-25** (Task 1.1.2/1.3.1/4.1.1 chạm quyền hệ thống/DB-layer chạy ở Leash A thay vì A+ đáng lẽ phải có); người dùng tự xem bằng chứng kỹ thuật thật rồi xác nhận chấp nhận đóng — xem `DELEGATION-MAP-SAFECALL.md` mục "Sign-off retroactive". Lịch sử lệch Leash không đổi, chỉ không còn là gap *chưa ai review*.
4. **1 vi phạm hard-stop thật** (AI tự suy luận lại pseudocode LLD bị mất, không dừng hỏi) — khối #5.
5. **Bug fail-open nghiêm trọng, ẩn ~2 tuần (2026-10-23 → 2026-11-08)** — `execSQL()` dùng sai cho PRAGMA trả dòng khiến EXACT/PREFIX blocking hoàn toàn không hoạt động trên thiết bị thật; chỉ phát hiện được khi lần đầu mô phỏng cuộc gọi thật qua `adb emu gsm call`, không phải qua build hay test tay UI — xem Decision Log khối #6. **Bài học:** "build xanh + test tay UI" không đủ để tin một tính năng hot-path hoạt động đúng.
6. **5 Task mới chạm Layer 0 (cascade FR-11/12/13) chưa qua Delegation Map để xét Leash A+** — xem `DELEGATION-MAP-SAFECALL.md` mục "5 gap ⚠️ mới (bổ sung 2026-11-08)", đang chờ sign-off retroactive, chưa tự coi là đã duyệt.

## Rủi ro (dẫn từ `RISK-SAFECALL.md`, Top-5 theo L×I)

1. R2 (20) — Overlay lúc đổ chuông chưa test đa OEM thật.
2. R1 (16) — Độ tin cậy phát hiện trạng thái cuộc gọi API 29 vs 31+ chưa có test hồi quy.
3. R11 (16) — Hệ quả của "viết trước, không verify" — đã xảy ra, cần gate chặt hơn cho Phase còn lại.
4. R4 (12) — Race condition prompt chưa test có kiểm soát.
5. R3/R6/R12 (10, đồng điểm) — endCall() chưa verify OEM / hiệu năng matching chưa sanity-check / hard-stop vi phạm trước đó.

> *(R9 rời Top-5 sau cập nhật 2026-10-25: điểm giảm 12→8 nhờ golden-file test, và **đóng hẳn về L×I=0 ngày
> 2026-11-08** sau khi cả `gradle testDebugUnitTest` và `flutter test` (64/64) đều chạy thật và PASS — rủi ro
> đầu tiên trong Risk Register đạt điểm 0, Risk Open Rate lần đầu rời khỏi 100% (còn 13/14≈92.9%). R13 rời
> Top-5 sau cập nhật 2026-10-25 lần 2: điểm giảm 12→8 sau khi 3 gap governance được ký duyệt retroactive —
> xem `RISK-SAFECALL.md`. R13 vẫn 🟡 chưa đóng hẳn 0 vì cơ chế chặn A+ tự động TRƯỚC build vẫn chưa tồn tại —
> **càng rõ hơn sau khi cascade FR-11/12/13 lại thêm 5 Task Layer-0 mới chạy ở Leash A, xem
> `DELEGATION-MAP-SAFECALL.md`** — rủi ro R13 cảnh báo không phải lý thuyết, nó vừa tái diễn.)*

## Tuần tới / Bước kế tiếp (next steps)

1. ~~Chạy `flutter test`/`gradle test` thật để đóng hẳn R9~~ — **đã làm 2026-11-08**: 64/64 (Dart) + 1/1 (Kotlin) PASS, R9 → L×I=0. Xem `RISK-SAFECALL.md`.
2. ~~Chạy script `adb emu gsm call` để đóng Task 8.1.1~~ — **đã làm 2026-11-08 (một phần)**: mô phỏng cuộc gọi thật lần đầu, nhưng phát hiện bug fail-open nghiêm trọng (Decision Log #6) thay vì chỉ xác nhận hoạt động đúng. Task race condition 4.4.2 **vẫn chưa làm**.
3. Người dùng tự tạo keystore theo `docs/release/RELEASE-SIGNING-SAFECALL.md` — AI không tự làm được (secret). **Vẫn mở.**
4. Xác nhận với stakeholder 2 quyết định thiết kế còn treo: ngưỡng prefix 4 ký tự (R5), `source=IMPORTED` luôn gán khi import (LLD v1.3). **Vẫn mở.**
5. ~~Cập nhật `ADR-003`~~ — đã đóng 2026-10-25.
6. Khi có thiết bị OEM thật: ưu tiên R2 trước (điểm rủi ro cao nhất, overlay FR-8). **Vẫn mở — vẫn là R2 cao nhất Top-5.**
7. ~~Bổ sung RTM cho 7 NFR~~ — đã làm 2026-10-25; **bổ sung tiếp 2026-11-08**: thêm FR-11/FR-12/FR-13.
8. ~~Viết `MODULEMAP-SAFECALL.md`~~ — đã làm 2026-10-25; **cập nhật tiếp 2026-11-08**: thêm module Settings + `PermissionCheckWorker`/`SafeCallApplication`.
9. Xác nhận NFR-5/6/7 với stakeholder — đã làm 2026-10-25. Còn mở: chạy Task 7.7.3 (test NFR-4+NFR-6, **làm được ngay, không cần thiết bị — chưa làm**), review chi tiết Nghị định 13 (NFR-7).
10. ~~Ký duyệt retroactive 3 gap governance (1.1.2/1.3.1/4.1.1)~~ — đã làm 2026-10-25.
11. ~~Sign-off retroactive 5 gap Layer-0 mới~~ (9.2.1/10.2.2, 9.3.2/10.3.1, 11.2.2, 11.1.1/11.2.3, 9.1.x/10.1.x) — **đã làm 2026-11-08**: người dùng tự xem bằng chứng qua `AskUserQuestion`, chấp nhận đóng retroactive cả 5. Xem `DELEGATION-MAP-SAFECALL.md` mục "Sign-off retroactive 5 gap ⚠️ mới". Quy tắc A+ TRƯỚC build vẫn chưa có cơ chế tự động — đây là lần thứ 2 gap này tái diễn, không phải lần đầu.
12. *(Mới 2026-11-08)* Verify tay qua emulator thật cho FR-11/FR-12 (gạt switch Settings, xác nhận pipeline đọc đúng giá trị) và FR-13 (trigger `PermissionCheckWorker` qua `WorkManager` thật hoặc `androidx.work:work-testing`) — nâng từ "Code-only, not verified" lên "Tested-Pass" trong RTM.
13. *(Mới 2026-11-08)* Chạy sanity-check hiệu năng 10.000 entry PREFIX (Task 7.7.3, đóng R6) — làm được ngay trên emulator, không cần thiết bị thật, chưa ai làm.
14. *(Mới 2026-11-08)* `RTM-SAFECALL.md` dòng FR-1 vẫn ghi "chưa có thao tác gọi/giả lập cuộc gọi thật" — **đã STALE**, vì `adb emu gsm call` đã chạy thật ở `4fe7e62` (xác nhận EXACT/PREFIX blocking hoạt động đúng sau fix). Cần cập nhật RTM FR-1, nằm ngoài phạm vi 5 file sửa trong lượt này.

## Đóng góp của AI (bắt buộc nêu rõ theo EX-05)

- AI viết 100% code Phase 1-8 (Dart + Kotlin) theo uỷ quyền "viết trước, không verify build" — tiết kiệm effort code thủ công đáng kể so với baseline PERT-weighted ~151.5 person-day, nhưng **không có số giờ thật để so sánh tiết kiệm bao nhiêu** (xem Dashboard — KPI Nén = N/A).
- AI tự phát hiện + tự sửa 4 lỗi code thật qua build/test thật (không phải người dùng tự sửa dòng code nào — `Human-edit = 0`).
- AI tự viết toàn bộ 10 tài liệu governance (Decision Log, Telemetry, Risk, Delegation Map, DOR, RTM, Weekly, Dashboard) — người dùng tham gia ở tầng quyết định (chọn mức ủy quyền, xác nhận hướng đi), không tự tay soạn từng dòng.
- **1 vi phạm hard-stop thật của AI** (khối #5, Decision Log) — không che giấu, ghi nhận đầy đủ như một sự kiện governance cần giám sát chặt hơn ở các Phase còn lại.
- **Cập nhật 2026-10-25:** AI viết bộ test tự động ĐẦU TIÊN của dự án (golden-file, 58 case, đóng Risk R9 một phần) và dùng đúng cổng hiểu trước khi code (hỏi rẽ nhánh qua `AskUserQuestion` khi yêu cầu còn mơ hồ giữa 3 hướng khác nhau, 1 trong 3 hướng sẽ vi phạm quyết định "local-only") — đối lập trực tiếp với vi phạm hard-stop ở khối #5 trước đó, cho thấy bài học đã được áp dụng.
- **Cập nhật 2026-11-08:** AI tự chạy `flutter test` thật lần đầu (không tin theo claim cũ chưa trích được bằng chứng), đóng hẳn R9. AI cũng tự phát hiện (không phải người chỉ ra) bug fail-open nghiêm trọng ẩn ~2 tuần khi lần đầu chủ động mô phỏng cuộc gọi thật qua `adb emu gsm call` (Decision Log #6) — một ví dụ cụ thể cho thấy giá trị của việc chủ động verify hành vi thật, không chỉ tin vào "build xanh"/test tay UI. Ở mặt governance, AI KHÔNG tự ký duyệt retroactive cho 5 gap Layer-0 mới phát hiện cùng ngày (`DELEGATION-MAP-SAFECALL.md`) — đúng bài học rút ra từ rủi ro R13 (rubber-stamping), để chờ người dùng quyết định. **Phát hiện vận hành đáng lưu ý:** 2 phiên Claude Code khác nhau đã chỉnh cùng bộ file governance trên cùng working directory cách nhau ~1 giờ trong cùng ngày — chưa có cơ chế điều phối giữa các phiên, rủi ro xung đột/ghi đè nếu tiếp diễn.
