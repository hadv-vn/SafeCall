# Mini Dashboard — SAFECALL

> Theo Workbook EX-05. ≥5 KPI, mỗi KPI có **công thức + nguồn dữ liệu** — KPI nào thiếu nguồn số liệu thật
> ghi rõ **N/A**, không suy diễn/bịa số thay thế (luật chống hallucination của chương trình).

## Bảng KPI

| # | KPI | Công thức | Nguồn dữ liệu | Giá trị |
|---|---|---|---|---|
| 1 | **Nén (Compression)** | giờ truyền thống ước tính ÷ giờ thật đã dùng | Tử số: `estimate.md`/`WBS-SafeCall-Phase1.md` PERT-weighted. Mẫu số: `TELEMETRY-SAFECALL.md` mục "Thời gian" | **N/A** — không đổi, 2026-11-08: vẫn không có đồng hồ bấm giờ thật cho bất kỳ phiên nào (kể cả phiên FR-11/12/13 2026-11-06..08) |
| 2 | **Rework Rate** | số vòng lặp có phát sinh rework ÷ tổng số vòng lặp quan sát được | `TELEMETRY-SAFECALL.md` 3 khối đã ghi (gốc ~5 vòng rework ở #1-#4; bổ sung lần 1: 3 vòng, 0 rework; bổ sung lần 2: 1 vòng, 0 rework) | **4/9 ≈ 44.4%** (không đổi — vẫn đúng số đã sửa lần trước). **Giới hạn vẫn còn, lớn hơn trước:** `TELEMETRY-SAFECALL.md` vẫn **không có khối nào** cho việc làm FR-11/FR-12/FR-13 2026-11-06→08, và nay cộng thêm 3 commit mới (`79daea2` FR-13 backend, `7eefd83` FR-13 Flutter, `78691bd` Settings UI, đóng đúng 2 gap mà lượt audit trước phát hiện) cũng không có khối Telemetry nào — mẫu số 9 càng ngày càng không phủ được phần việc gần nhất. |
| 3 | **Human-edit Rate** | số dòng code người dùng tự sửa ÷ tổng thay đổi code | `TELEMETRY-SAFECALL.md` mục "Human-edit" (code) + `RISK-SAFECALL.md` mục "Log self-review/approve" (tài liệu) | **0%** cho CODE sản phẩm (Dart/Kotlin) — không đổi. **Cập nhật 2026-11-08 — phát hiện mới, không ép vào cùng con số vì khác loại:** lần đầu tiên có bằng chứng người dùng **tự tay sửa trực tiếp** tài liệu (không phải code): PM (hadv-vn) sửa `RISK-SAFECALL.md`, `estimate.md`, `WBS-SafeCall-Phase1.md` ngày 2026-11-06 (ghi rõ trong `RISK-SAFECALL.md` mục "Log self-review/approve"). Đây là review evidence thật cho 3 file đó — xem `KNOWLEDGE-HEALTH-SAFECALL.md` c5 — nhưng KHÔNG tính vào KPI này vì công thức gốc chỉ đo "code", không đo "tài liệu". |
| 4 | **Test Coverage theo FR** | số FR trạng thái Tested-Pass ÷ tổng số FR | `RTM-SAFECALL.md` (FR-1, FR-5, FR-7, FR-9, FR-11, FR-12 = Tested-Pass) đối chiếu `SRS-SafeCall-Phase1.md` v6.1 (tổng FR hiện tại) | **6/13 ≈ 46.2%** — tăng mạnh so với lượt trước (2/13), nhưng vì 2 lượt audit TRƯỚC đã bỏ sót cập nhật RTM, không phải vì vừa làm xong 4 FR trong 1 lượt. **Cập nhật 2026-11-08 (lượt 3 — verify tay FR-11/FR-12):** gạt switch thật qua UI (`adb shell input tap`, tọa độ lấy từ `uiautomator dump`) trên `SettingsScreen`, xác nhận ghi đúng `blocklist_meta` qua `sqlite3` trên file DB thật, rồi gọi thật qua `adb emu gsm call` — cả 3/4 cờ (`contacts_priority`, `block_unknown`, `block_international`) cho kết quả đúng ở Call Log hệ thống (`type` 3↔6). FR-11 nâng thẳng lên ✅ Tested-Pass; FR-12 nâng lên ✅ Tested-Pass **(một phần)** vì cờ thứ 4 (`block_anonymous`) không test được bằng công cụ emulator (không có lệnh set `handlePresentation=RESTRICTED`). **Riêng FR-1/FR-9 lên Tested-Pass là do một lượt verify KHÁC, TRƯỚC lượt này** (commit `4fe7e62`, chưa được 2 lượt audit Dashboard trước phản ánh — đúng kiểu Freshness gap mà mục "Ghi chú trung thực" dưới đã cảnh báo nhiều lần). Mẫu số 13 không đổi. Còn 🟡 Code-only: FR-10, FR-13. |
| 5 | **Governance Gap Rate** | số Task chạy lệch Leash (thực tế ≠ đáng lẽ) ÷ tổng Task ≥ L3 | `DELEGATION-MAP-SAFECALL.md` (vẫn 3 dòng ⚠️ lịch sử: 1.1.2, 1.3.1, 4.1.1 / 17 Task ≥L3 — mtime file vẫn 2026-10-25) | **3/17 ≈ 17.6%** — con số không đổi vì Delegation Map vẫn chưa được ai sửa (xác nhận lại 2026-11-08: mtime vẫn 2026-10-25), nhưng khoảng hở thật **lớn hơn lần trước**: ngoài 2 Task Layer-0 đã phát hiện trước đây (pipeline `CallScreeningService`, migration `blocklist_meta`), nay có thêm **3 Task Layer-0 mới** từ các commit `79daea2`/`7eefd83`/`78691bd` chưa hề được xét Leash: (a) `SafeCallApplication.kt` — Application class mới, đăng ký trong Manifest, chạy từ `onCreate()` của toàn bộ app; (b) `PermissionCheckWorker.kt`/`androidx.work` — tác vụ nền định kỳ 24h, đọc/ghi permission state hệ thống; (c) `SettingsScreen`/`SettingsService` — ghi trực tiếp vào `blocklist_meta` (bảng mà `CallScreeningService` hot-path đọc mỗi cuộc gọi). Cả 5 Task Layer-0 chưa qua Delegation Map vẫn không cộng vào tử số (Delegation Map chưa "biết" tới) nhưng rủi ro governance thật đang tích lũy, không giảm. **Cập nhật 2026-11-08 (lượt 4 — sửa claim SAI của chính dòng KPI này):** câu "mtime vẫn 2026-10-25" ở cột Nguồn dữ liệu **đã SAI** tại thời điểm viết lại lượt này — `DELEGATION-MAP-SAFECALL.md` thực ra ĐÃ được sửa (mtime nay 20:09, có thêm đúng 5 dòng ⚠️ đã nêu + mục "Sign-off retroactive 5 gap ⚠️ mới"), chỉ là việc sửa đó xảy ra sau khi câu này được viết, và không ai quay lại cập nhật câu này theo — đúng kiểu lỗi mà chính Dashboard này được tạo ra để bắt (tin theo claim cũ của chính mình thay vì verify lại nguồn gốc trước khi viết tiếp). Đếm lại trực tiếp từ bảng Delegation Map hiện tại: **22 Task ≥L3** (17 cũ + 5 mới), trong đó **8 dòng ⚠️ lệch Leash** (3 cũ: 1.1.2/1.3.1/4.1.1 + 5 mới: 9.2.1-10.2.2/9.3.2-10.3.1/11.2.2/11.1.1-11.2.3/9.1.x-10.1.x) — tất cả 8 đều đã "✅ Đóng retroactive" (người dùng tự xem bằng chứng rồi duyệt qua `AskUserQuestion`), nhưng theo đúng quy ước "đóng ≠ xoá lịch sử" mà chính Delegation Map tự đặt ra, vẫn tính đủ vào tử số. **Giá trị đúng: 8/22 ≈ 36.4%** — tăng mạnh so với 17.6%, không phải vì governance xấu đi thêm, mà vì lượt trước tính thiếu (chưa cộng 5 gap mới dù chúng đã có trong file). |
| 6 | **Risk Open Rate** | số rủi ro có mitigation ghi "chưa chạy/chưa xác nhận" ÷ tổng số rủi ro | `RISK-SAFECALL.md` (14 rủi ro: R1-R10 chính + R11-R14 AI-sinh) | **13/14 ≈ 92.9%** — **giảm từ 100%, lần đầu tiên KHÔNG còn 100% kể từ khi Risk Register được tạo.** R9 (lệch `PhoneNumberNormalizer` Dart/Kotlin) nay **đóng hoàn toàn** (L×I: 4→0) sau khi tự chạy `flutter test` thật 2026-11-08 — 64/64 PASS, cộng với `gradle testDebugUnitTest` đã xanh từ trước, đúng điều kiện đóng hẳn mà chính Risk Register đã tự định nghĩa trước ("khi CẢ HAI phía cùng chạy xanh"). 13 rủi ro còn lại vẫn "chưa chạy/chưa xác nhận" — không có rủi ro thứ 2 nào đóng được trong lượt này. **Vẫn chưa có risk nào cho khoảng hở Settings UI/FR-13 vừa đóng** (ví dụ: `PermissionCheckWorker` chạy nền 24h nhưng chưa test qua `WorkManager` thật — `androidx.work:work-testing` chưa được dùng) — đề xuất PM xem xét bổ sung, không tự thêm. |
| 7 | **Automated Test Case Count** | số test case tự động hiện có trên toàn repo, phân biệt rõ "đã viết" vs "đã chạy thật qua toolchain" | `test/fixtures/phone_normalizer_golden.json` (58 case) + `test/settings/settings_models_test.dart` (6 test) + 4 class Kotlin `src/test/kotlin/...` (đếm `@Test`) + `build/app/test-results/testDebugUnitTest/TEST-*.xml` (bằng chứng CHẠY thật, Kotlin) + output trực tiếp lệnh `flutter test` (bằng chứng CHẠY thật, Dart) | **Kotlin: 40 `@Test` (CallScreeningServiceTest 20 + PromptEligibilityEvaluatorTest 13 + PermissionStatusCheckerTest 6 MỚI + PhoneNumberNormalizerGoldenTest 1) — PASS thật qua `gradle testDebugUnitTest`, 4 file XML mtime 14:38, `failures="0"` cả 4. Dart: 64 test (58 golden-file + 6 settings_models_test.dart MỚI) — PASS thật, tự chạy `flutter test` trực tiếp 2026-11-08, output `+64: All tests passed!`.** **Thay đổi quan trọng nhất của lượt audit này:** đây là lần ĐẦU TIÊN **CẢ HAI toolchain** (Kotlin lẫn Dart) chạy test thật và xanh 100% cùng lúc — lần trước chỉ có Kotlin, Dart "chưa có bằng chứng chạy" dù đã được WEEKLY ưu tiên là next-step. Lý do trước đó ghi "không có flutter trong môi trường AI" **không còn đúng** — Flutter có sẵn ở `D:\flutter`, chỉ là chưa ai thử gọi. |

## Quyết định rút ra từ dashboard (bắt buộc theo EX-05)

**Không release/publish Play Store ở trạng thái hiện tại.** Căn cứ: Test Coverage theo FR chỉ 20% (KPI #4) và
Risk Open Rate 100% (KPI #6) — nghĩa là phần lớn rủi ro kỹ thuật đã liệt kê (đặc biệt R2/R1, Top-2 theo L×I)
chưa có bằng chứng kiểm chứng thật nào ngoài 1 lượt test tay trên 1 emulator. Ưu tiên tiếp theo: đóng Task
8.1.1 (system/E2E qua `adb emu gsm call`, làm được ngay, không cần thiết bị thật) trước khi tính đến bất kỳ
mốc release nào.

**Cập nhật 2026-11-08 — vẫn giữ nguyên quyết định KHÔNG release, lý do còn mạnh hơn trước:** ngoài 2 căn cứ
cũ (vẫn đúng, chưa đổi), nay có thêm bằng chứng cụ thể: (1) SRS đã chốt 13 FR (v6.1) nhưng FR-13 có **0 dòng
code**; (2) FR-11/FR-12 có pipeline + test tự động PASS thật, nhưng **thiếu màn hình Cài đặt** — người dùng
thật không có cách nào bật/tắt 4 cờ cấu hình đã code sẵn; (3) ít nhất 2 Task mới chạm Layer 0 (hot-path chặn
gọi, schema migration) được build mà không qua Delegation Map để xét Leash, khác hẳn nguyên tắc dự án tự đặt
ra sau 3 gap cũ. Điểm sáng duy nhất của lượt audit này: `gradle testDebugUnitTest` đã chạy thật lần đầu, 34/34
test Kotlin xanh — một phần của nền tảng kiểm chứng đang hình thành, nhưng chưa đủ để đổi quyết định release.

**Cập nhật 2026-11-08 (lượt 2, cùng ngày) — VẪN giữ nguyên quyết định KHÔNG release, nhưng tiến bộ thật và cụ
thể hơn nhiều:** cả 2 gap CODE nêu ở lượt trước (thiếu màn hình Cài đặt, FR-13 0% code) **đã được đóng bằng
code thật** qua 3 commit `79daea2`/`7eefd83`/`78691bd` — xem RTM FR-11/FR-12/FR-13. Tự chạy `flutter test`
thật lần đầu (64/64 PASS) xác nhận phía Dart cũng xanh, không chỉ Kotlin — đóng hẳn Risk R9 (KPI #6 lần đầu
<100%). Lý do VẪN không release: (1) toàn bộ 3 FR mới + Settings UI mới chỉ ở mức unit-test (mock) PASS,
**chưa ai verify tay qua emulator thật** (gạt switch, xác nhận pipeline đọc đúng giá trị mới; trigger
`PermissionCheckWorker` qua `WorkManager` thật); (2) khoảng hở Governance Gap (KPI #5) **tăng thêm**, không
giảm — nay có 5 Task Layer-0 (không phải 2) build ngoài Delegation Map; (3) toàn bộ rủi ro cao nhất chưa đổi
(R2/R1/R11 vẫn Top-3, chưa test đa OEM/trạng thái cuộc gọi thật). Ưu tiên tiếp theo không đổi: Task 8.1.1.

**Cập nhật 2026-11-08 (lượt 3, cùng ngày) — verify tay FR-11/FR-12, VẪN giữ nguyên quyết định KHÔNG release:**
gap verify tay nêu ở lượt 2 nay đóng được **2/3 FR mới** — FR-11 (`CONTACTS_PRIORITY`) và FR-12 (`BLOCK-TOGGLE`,
một phần) đã verify tay thật qua emulator (UI tap thật + `adb emu gsm call`, xem RTM/Decision Log khối #7 chi
tiết). FR-13 (`PermissionCheckWorker`) **vẫn chưa** verify — không thuộc phạm vi lượt test này. Lý do VẪN
không release: (1) FR-13 còn mồ côi test thao tác tay duy nhất trong nhóm 3 FR mới; (2) phát hiện phụ mới
trong chính lượt test này — `CallMonitorService.handleCallEnded()` không ghi `call_history` cho cuộc gọi
"cho qua rồi kết thúc" (badge `decisionReason` trên `CallHistoryScreen` không lên dữ liệu), nghi vấn liên quan
tới cùng hiện tượng `SQLiteLog: database is locked` đã thấy — CHƯA điều tra xong, thuộc Luồng 2 (FR-2/FR-8),
tự nó đã là 1 lý do không release vì FR-2/FR-3/FR-4/FR-6/FR-8 vẫn 100% mồ côi test từ trước; (3) Governance Gap
(KPI #5) và rủi ro cao nhất (R1/R2/R11) không đổi so với lượt 2. Ưu tiên tiếp theo: Task 8.1.1 (vẫn đúng), và
điều tra riêng phát hiện phụ ở Decision Log khối #7 trước khi coi Luồng 2 là "sẵn sàng test".

**Cập nhật 2026-11-08 (lượt 4, cùng ngày) — VẪN giữ nguyên quyết định KHÔNG release, căn cứ mạnh hơn, không
yếu hơn:** sửa đúng KPI #5 (17.6%→36.4%, xem bảng KPI — lỗi tính thiếu của lượt trước, không phải governance
xấu đi thêm) cho thấy số Task Layer-0 lệch Leash thật **gần gấp 3 lần** con số Dashboard từng báo cáo trong
2 lượt audit liên tiếp trước đó. Có thêm 1 tài liệu mới (`docs/testing/ManualQATestScript-SafeCall-Phase1.md`,
commit `36a4ab7`) cung cấp kịch bản `adb` cụ thể cho FR-2/3/4/6/8/9/10/13 — đây là tiến bộ thật về CHUẨN BỊ
test, nhưng **chưa có bằng chứng đã chạy** kịch bản nào trong đó, nên không đổi bất kỳ trạng thái Test nào
trong RTM. Ưu tiên tiếp theo không đổi: điều tra Decision Log khối #7 (bug `call_history` nghi SQLite lock),
sau đó chạy thật kịch bản QA mới này trên emulator.

## Phát hiện hallucination (bắt buộc theo EX-05 — luyện phản xạ chống bịa số)

**Số bị bắt:** Khi dựng KPI #1 (Nén), phản xạ đầu tiên (sai) là tự ước lượng "giờ thật đã dùng ≈ 40 giờ" để
ra được một con số Nén "đẹp" cho dashboard (ví dụ 151.5 person-day × 8h ÷ 40h ≈ tỷ lệ nén X). **Con số "~40
giờ" này KHÔNG có trong bất kỳ nguồn dữ liệu nào** — `TELEMETRY-SAFECALL.md` ghi rõ ràng "Thời gian: N/A —
không có đồng hồ bấm giờ thật". Đã phát hiện và sửa: thay "~40 giờ" bằng **N/A** ở KPI #1, không tính tỷ lệ
Nén giả, thay vì thay bằng một số ước lượng khác.

## Ghi chú trung thực

- 7 KPI trên đều tính tay từ tài liệu đã có (Telemetry/RTM/Delegation Map/Risk/fixture test), không dùng công cụ đo coverage/thời gian chuyên dụng nào.
- KPI #1 là ví dụ rõ nhất cho nguyên tắc "thiếu nguồn ⇒ N/A" — không có KPI nào trong 7 KPI trên bị thay bằng số suy diễn.
- **Cập nhật 2026-10-25:** KPI #7 là KPI đầu tiên có xu hướng tăng thật (0→58) kể từ khi Dashboard này được tạo — nhưng cố ý KHÔNG gộp vào KPI #4 (Test Coverage theo FR) vì 58 case này kiểm 1 hàm nội bộ (`PhoneNumberNormalizer`), không phải test ở mức FR/luồng người dùng, và chưa được xác nhận chạy qua toolchain thật.
- **Sửa lỗi stale data 2026-10-25 (phát hiện khi đối chiếu lại với khung AI Transformation):** KPI #2 và KPI #5 bị tính từ dữ liệu cũ — khi `TELEMETRY-SAFECALL.md` và `DELEGATION-MAP-SAFECALL.md` được bổ sung thêm dòng cùng ngày 2026-10-25 (phục vụ KPI #7), 2 KPI này KHÔNG được tính lại theo mẫu số mới, dẫn tới số liệu sai: KPI #2 ghi 4/5≈80% thay vì đúng 4/8=50%; KPI #5 ghi 3/15=20% thay vì đúng 3/17≈17.6%. Đã sửa lại đúng số ở bảng trên. Đây là ví dụ thật của rủi ro "Change Coupling"/"Freshness" — xem `KNOWLEDGE-HEALTH-SAFECALL.md` (mục c2/c3).
- **Cập nhật 2026-11-08 (audit toàn diện cho FR-11/FR-12/FR-13):** phát hiện chính file `.md` này (KPI #2, KPI #6) đã LỆCH với `dashboard/data.json` từ trước lượt audit này — 2 "bản sao" của cùng một KPI, một được sửa đúng, một bị bỏ quên không sửa theo. Đây là minh chứng sống cho đúng rủi ro mà `KNOWLEDGE-HEALTH-SAFECALL.md` cảnh báo ở c3 (Freshness): có NHIỀU bản sao dữ liệu (file `.md` tường thuật + `data.json` máy đọc) thì càng dễ lệch nhau nếu không có quy trình đồng bộ bắt buộc. KPI #4 và KPI #7 cho thấy rõ nhất khoảng cách giữa "SRS/code tiến rất nhanh" (13 FR, 34 test Kotlin chạy xanh trong 2 ngày 2026-11-06→08) và "governance tracking đứng yên" (RTM/Delegation Map/Telemetry không đổi 1 dòng nào suốt 2 tuần) — đúng hiện tượng mà Workbook §8.2 gọi là "kho kiến thức không còn là harness đáng tin cho AI".

- **Cập nhật 2026-11-08 (lượt 2, cùng ngày — theo yêu cầu người dùng "update lại dashboard, health check"):** phát hiện chính: `RTM-SAFECALL.md` đã được cập nhật lên mtime 14:40 (sau khi `.md` này + `KNOWLEDGE-HEALTH-SAFECALL.md` + `RISK-SAFECALL.md` + `dashboard/data.json`/`index.html` được sửa ở lượt audit TRƯỚC, mtime 14:06-14:12) — nghĩa là chính 2 file "theo dõi sức khoẻ" này đã BỊ LẠC HẬU so với RTM chỉ vài chục phút sau khi vừa được sửa, đúng kiểu rủi ro Freshness mà cả 2 file này được tạo ra để đo. Đã đối chiếu lại RTM mới nhất và cập nhật đúng theo.
- **Quyết định không tự fabricate trong lượt này:** trước khi sửa KPI #7, đã tự chạy `flutter test` (không suy diễn từ tên file test hay tin theo mô tả commit message "flutter test PASS" trong RTM) — xác nhận bằng output lệnh thật, đúng nguyên tắc "verify nguồn gốc, không tin nguồn thứ cấp" đã áp dụng xuyên suốt các lần audit trước.

## Phát hiện hallucination (bổ sung 2026-11-08)

**Suýt bắt nhầm theo chiều lạc quan:** khi thấy `CallScreeningServiceTest.kt` có 20 test PASS thật (bằng chứng `TEST-*.xml`, không phải suy diễn), phản ứng đầu tiên là muốn nâng ngay FR-11/FR-12 trong RTM lên "✅ Tested-Pass" giống FR-5/FR-7. **Đã dừng lại vì:** "Tested-Pass" trong quy ước RTM nghĩa là "đã verify tay thật qua emulator" (hành vi người dùng cuối-tới-cuối), còn 20 test này là **unit test có mock** (`newSpyService`, `forceLazyValue`) — verify đúng LOGIC pipeline, không verify được màn hình Cài đặt (vì màn hình đó **không tồn tại**, nên AC1 của FR-11/FR-12 — "tôi mở màn hình Cài đặt, thấy công tắc" — không thể nào Tested-Pass được, bất kể unit test có xanh cỡ nào). Không gán nhãn RTM mới cho FR-11/FR-12 trong lượt audit này — để nguyên là phát hiện cần `low-level-designer`/người thật cập nhật RTM với một trạng thái mới phản ánh đúng "unit-test-pass nhưng thiếu UI", không ép vào 1 trong 3 trạng thái cũ (Tested-Pass/Code-only/Not-started) vốn không mô tả đúng tình huống này.

## Phát hiện hallucination (bổ sung 2026-11-08, lượt 3 — verify tay FR-11/FR-12)

**Suýt bắt nhầm theo chiều lạc quan lần 2:** khi thấy Call Log hệ thống đổi đúng `type` (6↔3) theo từng cờ ở
cả 4 case, phản ứng đầu tiên là coi FR-12 đã Tested-Pass ĐỦ 3/3 AC phụ (số lạ, số quốc tế, số ẩn danh) vì "cùng
1 cơ chế code, test 2/3 thì cái thứ 3 chắc cũng đúng". **Đã dừng lại vì:** nhánh `block_anonymous_enabled`
không hề được thực thi qua bất kỳ lệnh `adb emu gsm call` nào trong lượt test này (không có cách giả lập số
ẩn danh qua console emulator) — suy diễn "chắc cũng đúng" từ 2 case đã test sang case thứ 3 CHƯA test là đúng
loại hallucination mà Dashboard này được tạo ra để bắt. Đã ghi rõ FR-12 là "Tested-Pass (một phần)", không
phải Tested-Pass đầy đủ, và liệt kê rõ `block_anonymous_enabled` vẫn chỉ có bằng chứng unit test mock.

## Phát hiện hallucination (bổ sung 2026-11-08, lượt 4 — theo yêu cầu người dùng "update lại dashboard, health check" lần 3)

**Tự bắt được claim SAI của chính Dashboard này, không phải của nguồn ngoài:** trước khi sửa KPI #5, đã định
chép lại nguyên văn "3/17 ≈ 17.6%, mtime Delegation Map vẫn 2026-10-25" từ lượt audit trước — vì câu đó ĐÃ
từng đúng. Dừng lại vì nguyên tắc "verify nguồn gốc trước khi viết tiếp" (đã áp dụng ở c7/Knowledge Health)
áp dụng ngay cả với câu do CHÍNH Dashboard này viết ra trước đó: mở lại `DELEGATION-MAP-SAFECALL.md` thật thì
file đã đổi (mtime 20:09, +5 dòng ⚠️ mới) — claim cũ đã stale mà không ai phát hiện. Nếu không mở lại file
gốc mà chỉ tin theo chính lịch sử Dashboard, KPI #5 sẽ tiếp tục báo sai 17.6% thay vì đúng 36.4% — một ví dụ
cụ thể cho thấy rủi ro Freshness (`KNOWLEDGE-HEALTH-SAFECALL.md` c3) không chỉ xảy ra GIỮA các file khác
nhau, mà có thể xảy ra NGAY TRONG một file khi nó trích dẫn lại chính nó qua nhiều lượt audit.

## Ghi chú trung thực (bổ sung 2026-11-08, lượt 4)

- Commit mới nhất tính tới lượt này (`36a4ab7`, `docs(testing): add manual QA test script for FR-9/FR-10/FR-13 and call-screening flow`) chỉ thêm **1 tài liệu kịch bản test tay** (`docs/testing/ManualQATestScript-SafeCall-Phase1.md`, 173 dòng) — KHÔNG có bằng chứng đã CHẠY kịch bản này. Không nâng bất kỳ KPI nào (Test Coverage theo FR, Automated Test Case Count) dựa trên tài liệu này — nó là kế hoạch thực thi cụ thể hơn, không phải kết quả test. Xem `KNOWLEDGE-HEALTH-SAFECALL.md` mục Spot-check c7 câu 6 cho đánh giá retrieval liên quan.
- Governance Gap Rate (KPI #5) tăng từ 17.6%→36.4% trong lượt này **không phải vì dự án làm sai thêm** — toàn bộ 8 gap đều đã được trình bày và ký duyệt retroactive qua `AskUserQuestion` (xem `DELEGATION-MAP-SAFECALL.md`). Tăng là vì lượt audit trước tính thiếu 5 gap đã tồn tại sẵn trong file nguồn.
