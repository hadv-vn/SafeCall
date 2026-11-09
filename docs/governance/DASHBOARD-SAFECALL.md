# Mini Dashboard — SAFECALL

> Theo Workbook EX-05. ≥5 KPI, mỗi KPI có **công thức + nguồn dữ liệu** — KPI nào thiếu nguồn số liệu thật
> ghi rõ **N/A**, không suy diễn/bịa số thay thế (luật chống hallucination của chương trình).

## Bảng KPI

| # | KPI | Công thức | Nguồn dữ liệu | Giá trị |
|---|---|---|---|---|
| 1 | **Nén (Compression)** | giờ truyền thống ước tính ÷ giờ thật đã dùng | Tử số: `estimate.md`/`WBS-SafeCall-Phase1.md` PERT-weighted. Mẫu số: `TELEMETRY-SAFECALL.md` mục "Thời gian" | **N/A** — không đổi, 2026-11-08: vẫn không có đồng hồ bấm giờ thật cho bất kỳ phiên nào (kể cả phiên FR-11/12/13 2026-11-06..08) |
| 2 | **Rework Rate** | số vòng lặp có phát sinh rework ÷ tổng số vòng lặp quan sát được | `TELEMETRY-SAFECALL.md` 5 khối đã ghi (gốc ~5 vòng rework ở #1-#4; bổ sung lần 1: 3 vòng, 0 rework; bổ sung lần 2: 1 vòng, 0 rework; cascade FR-11/12/13: 1 vòng rework CODE + 2 vòng audit, 0 rework; lượt 3-4 verify tay: 2 vòng, 0 rework; viết ManualQATestScript: 1 vòng, 0 rework) | **Cập nhật 2026-11-08 (lượt 5):** mẫu số trước đó (9) bị tính thiếu — thực ra cascade FR-11/12/13 (2026-11-06→08) đã có khối Telemetry riêng (xem file, phần "Phần bổ sung — 2026-11-06→08") đóng góp thêm 1 vòng rework (`4fe7e62`) + 2 vòng audit = 3 vòng, 0 rework mới; KPI này ở lượt audit trước (`53fcd9c`/`ff7a7e0`/`a53cb68`) **không được tính lại theo mẫu số mới** — lặp lại đúng kiểu lỗi "quên cập nhật KPI#2/#5 khi mẫu số đổi" mà mục Ghi chú trung thực 2026-10-25 từng cảnh báo. Đã bổ sung 2 khối Telemetry còn thiếu (lượt verify tay 3-4 → Decision Log Khối #7: 2 vòng, 0 rework; viết `ManualQATestScript` commit `36a4ab7`: 1 vòng, 0 rework) và tính lại từ đầu bằng cách đếm trực tiếp mọi khối trong `TELEMETRY-SAFECALL.md`: tử số (vòng có rework) = 4 (#1-#4 gốc) + 1 (`4fe7e62`) = **5**; mẫu số = 5 (gốc) + 3 (bổ sung lần 1) + 1 (bổ sung lần 2) + 3 (cascade) + 2 (verify tay 3-4) + 1 (ManualQATestScript) = **15**. **Giá trị đúng: 5/15 ≈ 33.3%** — giảm so với 44.4% báo trước, không phải vì rework ít đi, mà vì mẫu số trước đó bỏ sót 6 vòng không-rework đã có sẵn trong nguồn (đúng kiểu lỗi Freshness, không phải cải thiện thật). **Giới hạn còn lại:** commit `df47a61` (instrumented test FR-12 AC3 chặn số ẩn danh, ngay sau lượt 3-4) vẫn **chưa có khối Telemetry riêng** — mẫu số 15 vẫn chưa phủ hết; cần thêm 1 khối nữa cho commit này ở lượt audit kế tiếp. |
| 3 | **Human-edit Rate** | số dòng code người dùng tự sửa ÷ tổng thay đổi code | `TELEMETRY-SAFECALL.md` mục "Human-edit" (code) + `RISK-SAFECALL.md` mục "Log self-review/approve" (tài liệu) | **0%** cho CODE sản phẩm (Dart/Kotlin) — không đổi. **Cập nhật 2026-11-08 — phát hiện mới, không ép vào cùng con số vì khác loại:** lần đầu tiên có bằng chứng người dùng **tự tay sửa trực tiếp** tài liệu (không phải code): PM (hadv-vn) sửa `RISK-SAFECALL.md`, `estimate.md`, `WBS-SafeCall-Phase1.md` ngày 2026-11-06 (ghi rõ trong `RISK-SAFECALL.md` mục "Log self-review/approve"). Đây là review evidence thật cho 3 file đó — xem `KNOWLEDGE-HEALTH-SAFECALL.md` c5 — nhưng KHÔNG tính vào KPI này vì công thức gốc chỉ đo "code", không đo "tài liệu". |
| 4 | **Test Coverage theo FR** | số FR trạng thái Tested-Pass ÷ tổng số FR | `RTM-SAFECALL.md` (cập nhật 2026-11-09 — toàn bộ FR-1..FR-13 = Tested-Pass) đối chiếu `SRS-SafeCall-Phase1.md` v6.1 (tổng FR hiện tại) | **13/13 = 100%** — tăng từ 46.2% (6/13). **Cập nhật 2026-11-09 (verify tay FR-2/FR-3/FR-4/FR-6/FR-8/FR-10):** đóng toàn bộ FR còn lại qua chuỗi verify tay thật trên `emulator-5554` (`adb emu gsm call` + tap UI thật + `sqlite3` + system file picker/Share Sheet thật), KHÔNG mock. Phát hiện và SỬA 3 bug sản phẩm thật trong quá trình: (1) `AnonymousCallBlockingInstrumentedTest`/`TestConnectionService` bị Telecom từ chối `SecurityException` do khai báo sai package (androidTest thay vì debug) — sửa xong, 4/4 test PASS thật qua `am instrument`; (2) `TelephonyCallbackObserver` đăng ký callback chạy trên Binder thread hệ thống thay vì main thread, khiến `RingingScamOverlay.show()` crash ngầm mỗi lần có cuộc gọi đổ chuông — đây chính là nguyên nhân khiến **FR-2/FR-3/FR-4 không bao giờ chạy được** trước lượt này; (3) `CallLogNumberResolver` nhét `"LIMIT 1"` sai cú pháp vào `sortOrder` của `ContentResolver.query()`, bị `CallLogProvider` (API 34) từ chối 100% — ĐÂY MỚI LÀ root cause thật của bug "mất `call_history`" ở Decision Log khối #7 (giả thuyết gốc "SQLite lock" SAI, xem Decision Log #8 đính chính). Sau khi sửa cả 3: FR-2/FR-3/FR-4 Tested-Pass (AC1); FR-6 Tested-Pass (3/9 badge, gồm bằng chứng gián tiếp cho AC3 không-hồi-tố); FR-10 Tested-Pass (7/11 AC, qua Share Sheet + file picker thật, kể cả 1 lần chạm nhầm file lại tình cờ test được AC8); **FR-8 Tested-Pass trên emulator (5/10 AC) — bao gồm cả AC5, `TelecomManager.endCall()` THỰC SỰ ngắt được cuộc gọi trên AVD**. **CẢNH BÁO quan trọng, không suy diễn rộng:** `endCall()` chạy được trên AVD KHÔNG thay thế yêu cầu bắt buộc của ADR-005 (test đa OEM trên thiết bị thật, Task 6.5.3 WBS vẫn chưa làm) — xem RTM dòng FR-8 để biết chi tiết 3 rủi ro race chưa test được trên emulator. |
| 5 | **Governance Gap Rate** | số Task chạy lệch Leash (thực tế ≠ đáng lẽ) ÷ tổng Task ≥ L3 | `DELEGATION-MAP-SAFECALL.md` (vẫn 3 dòng ⚠️ lịch sử: 1.1.2, 1.3.1, 4.1.1 / 17 Task ≥L3 — mtime file vẫn 2026-10-25) | **3/17 ≈ 17.6%** — con số không đổi vì Delegation Map vẫn chưa được ai sửa (xác nhận lại 2026-11-08: mtime vẫn 2026-10-25), nhưng khoảng hở thật **lớn hơn lần trước**: ngoài 2 Task Layer-0 đã phát hiện trước đây (pipeline `CallScreeningService`, migration `blocklist_meta`), nay có thêm **3 Task Layer-0 mới** từ các commit `79daea2`/`7eefd83`/`78691bd` chưa hề được xét Leash: (a) `SafeCallApplication.kt` — Application class mới, đăng ký trong Manifest, chạy từ `onCreate()` của toàn bộ app; (b) `PermissionCheckWorker.kt`/`androidx.work` — tác vụ nền định kỳ 24h, đọc/ghi permission state hệ thống; (c) `SettingsScreen`/`SettingsService` — ghi trực tiếp vào `blocklist_meta` (bảng mà `CallScreeningService` hot-path đọc mỗi cuộc gọi). Cả 5 Task Layer-0 chưa qua Delegation Map vẫn không cộng vào tử số (Delegation Map chưa "biết" tới) nhưng rủi ro governance thật đang tích lũy, không giảm. **Cập nhật 2026-11-08 (lượt 4 — sửa claim SAI của chính dòng KPI này):** câu "mtime vẫn 2026-10-25" ở cột Nguồn dữ liệu **đã SAI** tại thời điểm viết lại lượt này — `DELEGATION-MAP-SAFECALL.md` thực ra ĐÃ được sửa (mtime nay 20:09, có thêm đúng 5 dòng ⚠️ đã nêu + mục "Sign-off retroactive 5 gap ⚠️ mới"), chỉ là việc sửa đó xảy ra sau khi câu này được viết, và không ai quay lại cập nhật câu này theo — đúng kiểu lỗi mà chính Dashboard này được tạo ra để bắt (tin theo claim cũ của chính mình thay vì verify lại nguồn gốc trước khi viết tiếp). Đếm lại trực tiếp từ bảng Delegation Map hiện tại: **22 Task ≥L3** (17 cũ + 5 mới), trong đó **8 dòng ⚠️ lệch Leash** (3 cũ: 1.1.2/1.3.1/4.1.1 + 5 mới: 9.2.1-10.2.2/9.3.2-10.3.1/11.2.2/11.1.1-11.2.3/9.1.x-10.1.x) — tất cả 8 đều đã "✅ Đóng retroactive" (người dùng tự xem bằng chứng rồi duyệt qua `AskUserQuestion`), nhưng theo đúng quy ước "đóng ≠ xoá lịch sử" mà chính Delegation Map tự đặt ra, vẫn tính đủ vào tử số. **Giá trị đúng: 8/22 ≈ 36.4%** — tăng mạnh so với 17.6%, không phải vì governance xấu đi thêm, mà vì lượt trước tính thiếu (chưa cộng 5 gap mới dù chúng đã có trong file). **Cập nhật 2026-11-08 (lượt 6 — hướng cải thiện tới):** tử số 8 đã bị khoá cứng vào lịch sử (quy ước "đóng ≠ xoá lịch sử" ở `DELEGATION-MAP-SAFECALL.md`), retroactive sign-off không bao giờ làm chỉ số này giảm được. Đã thêm mục **"Cổng chặn Pre-Build cho Task mới"** vào `DELEGATION-MAP-SAFECALL.md` (hiệu lực từ 2026-11-08): mọi Task ≥L3 mới phải được xét Leash A+ TRƯỚC khi build, không phải retroactive sau khi phát hiện. Nếu tuân thủ đúng, các Task mới sẽ chỉ cộng vào mẫu số (hiện 22) mà không cộng thêm vào tử số (hiện 8) → tỷ lệ pha loãng dần. Lượt audit kế tiếp cần kiểm tra: có Task ≥L3 mới nào KHÔNG qua cổng này không — nếu có, đó là tái diễn lần 3 và là tín hiệu cổng chưa được tuân thủ, không phải lỗi thiết kế. |
| 6 | **Risk Open Rate** | số rủi ro có mitigation ghi "chưa chạy/chưa xác nhận" ÷ tổng số rủi ro | `RISK-SAFECALL.md` (14 rủi ro: R1-R10 chính + R11-R14 AI-sinh) | **13/14 ≈ 92.9%** — không đổi trong lượt 2026-11-09 (không tự sửa `RISK-SAFECALL.md`). **Phát hiện cần PM xử lý:** giả thuyết gốc cho bug Decision Log khối #7 ("nghi SQLiteLog lock") đã được xác nhận SAI khi verify tay FR-2/FR-3/FR-4 — nguyên nhân thật là 2 bug khác hẳn (threading sai trong `TelephonyCallbackObserver`, cú pháp `LIMIT 1` sai trong `CallLogNumberResolver`), cả hai ĐÃ SỬA và verify PASS. Risk Register vẫn chưa có entry riêng cho rủi ro này dù nay đã đóng — đề xuất PM bổ sung rồi đóng ngay (không tự thêm/sửa `RISK-SAFECALL.md`, giữ nguyên tắc không tự ý sửa tài liệu người khác sở hữu). |
| 7 | **Automated Test Case Count** | số test case tự động hiện có trên toàn repo, phân biệt rõ "đã viết" vs "đã chạy thật qua toolchain" | `test/fixtures/phone_normalizer_golden.json` (58 case) + `test/settings/settings_models_test.dart` (6 test) + 5 class Kotlin `src/test/kotlin/...` + 2 class instrumented `src/androidTest/kotlin/...` (đếm `@Test`) + output trực tiếp `adb shell am instrument` (bằng chứng CHẠY thật, instrumented, 2026-11-09) | **110 case, 100% PASS — tăng từ 104.** Kotlin unit: 40 (không đổi, kế thừa lượt trước, chưa chạy lại trong lượt này). Dart: 64 (không đổi, kế thừa lượt trước, chưa chạy lại trong lượt này). **MỚI 2026-11-09 — instrumented (androidTest, framework thật không mock): 6 case, LẦN ĐẦU có bằng chứng CHẠY thật** qua `adb shell am instrument`: `AnonymousCallBlockingInstrumentedTest` (4 case, commit `df47a61` — KPI #7 lượt 5 từng ghi rõ "đã viết, chưa chạy"; lượt này phát hiện bug package/UID khiến nó LUÔN fail, sửa xong rồi chạy `OK (4 tests)`), `PermissionCheckWorkerInstrumentedTest` (2 case, MỚI hoàn toàn, dùng `androidx.work:work-testing`/`TestWorkerBuilder`, chạy qua 2 lần `am instrument` riêng do giới hạn OS — đổi quyền runtime từ trong process instrumentation khiến process tự bị kill). Không cộng Dart/Kotlin unit test vào thành tích "mới" vì không re-run trong lượt này — chỉ cộng đúng 6 case instrumented thật sự mới có bằng chứng chạy. |

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

**Cập nhật 2026-11-08 (lượt 5, cùng ngày) — VẪN giữ nguyên quyết định KHÔNG release:** sửa KPI #2 (Rework
Rate 44.4%→33.3%, xem bảng — cùng loại lỗi "mẫu số cũ bị tính thiếu" như KPI #5 ở lượt 4, không phải rework
giảm thật) sau khi một phiên khác bổ sung đủ 2 khối Telemetry còn thiếu. Thêm 1 bằng chứng kỹ thuật mới đáng
chú ý: commit `df47a61` dùng `ConnectionService` MANAGED tiêm cuộc gọi trực tiếp vào Telecom framework để viết
instrumented test cho đúng gap "số ẩn danh không giả lập được qua `adb emu gsm call`" — một cách giải quyết
sáng tạo cho giới hạn công cụ đã ghi nhận ở lượt 3, nhưng **tự bản thân commit đó đã tự nhận chưa chạy test
này**. Không đổi quyết định release vì: (1) bug `call_history` (khối #7) vẫn CHƯA điều tra; (2) test mới vẫn
CHƯA có bằng chứng chạy; (3) `RISK-SAFECALL.md`/`WEEKLY-SAFECALL.md` vẫn chưa cập nhật theo các phát hiện của
2 lượt gần nhất. Ưu tiên không đổi: điều tra khối #7, rồi chạy thật `connectedDebugAndroidTest` + kịch bản
`ManualQATestScript`.

**Cập nhật 2026-11-09 — VẪN giữ nguyên quyết định KHÔNG release, nhưng vì lý do khác hẳn trước:** căn cứ cũ
"Test Coverage theo FR chỉ 46.2%" **không còn đúng** — nay 100% (13/13), toàn bộ FR đã có ít nhất 1 lần verify
tay thật, và bug "mất `call_history`" (khối #7) đã được điều tra ra root cause thật và sửa xong (không phải
"database is locked" như giả thuyết gốc — xem Decision Log #8). Đây là tiến bộ thật, không phải ảo giác số
liệu kiểu Freshness gap như các lượt trước. **Nhưng quyết định VẪN là KHÔNG release**, vì 3 căn cứ MỚI, độc
lập với Test Coverage: (1) FR-8/AC5 (`endCall()`) mới chỉ chạy PASS trên emulator (AVD) — ADR-005 yêu cầu
tường minh test đa OEM trên thiết bị thật trước khi release vì hành vi telecom stack khác nhau theo OEM, Task
6.5.3 WBS **vẫn chưa thực hiện**, và 3 rủi ro race (tự kết thúc/người dùng tự bắt máy/call-waiting) không giả
lập được trên emulator; (2) Governance Gap Rate (KPI #5) vẫn 36.4%, không đổi, và phát sinh thêm 1 nghi vấn
gap MỚI chưa xác nhận — 3 file Layer-0 hot-path (`CallMonitorService.kt`, `TelephonyCallbackObserver.kt`,
`CallLogNumberResolver.kt`) bị sửa ngay trong lúc verify tay, không rõ có qua đúng cổng Pre-Build Leash Gate
hay không; (3) nhiều AC phụ của từng FR (không phải AC lõi) vẫn chưa verify riêng — "Tested-Pass" ở mức FR
không đồng nghĩa "mọi AC đã test". Ưu tiên tiếp theo: Task 6.5.3 (test đa OEM thật cho FR-8), PM xác nhận gap
Leash mới, cập nhật `RISK-SAFECALL.md` với root cause đúng cho bug khối #7/#8.

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

## Ghi chú trung thực (bổ sung 2026-11-08, lượt 5)

- Trước khi tin theo claim "KPI #2 đã sửa đúng (5/15≈33.3%)" do một phiên khác viết, đã mở lại `TELEMETRY-SAFECALL.md` thật và đếm tay từng khối để xác nhận — đúng như claim: 5 khối hiện có (gốc + bổ sung lần 1 + lần 2 + cascade + verify tay 3-4 + ManualQATestScript), đếm lại ra đúng 5 vòng rework / 15 vòng tổng. Không tự tin theo số liệu do phiên khác tự báo mà không verify, cùng nguyên tắc đã áp dụng cho chính KPI #5 ở lượt 4.
- `TELEMETRY-SAFECALL.md` tự ghi rõ trong khối cuối: commit `df47a61` (instrumented test số ẩn danh) **vẫn chưa có khối Telemetry riêng** — một gap Freshness MỚI, phát sinh ngay sau khi 2 gap cũ vừa được đóng. Đúng mẫu hình đã lặp lại nhiều lần: đóng 1 gap Freshness thường kéo theo 1 gap mới ngay sau đó vì tiến độ code/commit nhanh hơn tiến độ đồng bộ tài liệu.

## Phát hiện hallucination (bổ sung 2026-11-09)

**Suýt bắt nhầm theo chiều lạc quan lần 3 — nguy hiểm nhất từ trước tới nay vì liên quan trực tiếp an toàn
người dùng:** khi `TelecomManager.endCall()` chạy THÀNH CÔNG trên `emulator-5554` cho FR-8/AC5 (Call Log đổi
đúng `RINGING→IDLE`, `CallTerminationOutcome.SUCCESS` ghi đúng vào cả `pending_prompts` và `call_history`),
phản xạ đầu tiên là muốn coi R2/R3 (risk "chưa test đa OEM thật" cho overlay/`endCall()`) gần như đã đóng, vì
"đã chạy thành công thật rồi, không còn là code chưa test nữa". **Đã dừng lại vì:** `ADR-005` và
`call-termination-feasibility.md` xác định rõ rủi ro nằm ở **sự khác biệt telecom stack giữa các OEM**, không
phải ở việc `endCall()` có code đường chạy đúng hay không — AVD dùng telecom stack mô phỏng của chính Google,
không đại diện cho Samsung/Oppo/Xiaomi... Suy diễn "chạy được trên 1 môi trường mô phỏng ⇒ sẽ chạy được trên
mọi thiết bị thật" đúng loại hallucination mà Dashboard này được tạo ra để bắt — đặc biệt nguy hiểm ở đây vì
hành động `endCall()` không thể hoàn tác (NFR-3) và test thiếu trên OEM nào đó có thể nghĩa là SafeCall báo
"đã chặn và ngắt" nhưng thực ra cuộc gọi lừa đảo vẫn tiếp diễn. Đã giữ nguyên R2/R3 ở điểm L×I cũ trong
`risks` (không tự hạ), chỉ sửa `label` ghi rõ "chạy được trên emulator, CHƯA verify đa OEM" thay vì xoá/giảm
mức độ rủi ro, và thêm dòng cảnh báo tường minh ở KPI #4 + mục "Quyết định" để không ai đọc nhanh rồi hiểu
nhầm 100% Test Coverage nghĩa là an toàn để release.

## Ghi chú trung thực (bổ sung 2026-11-09)

- Không tự sửa `RISK-SAFECALL.md`, `TELEMETRY-SAFECALL.md`, `KNOWLEDGE-HEALTH-SAFECALL.md`, `WEEKLY-SAFECALL.md` trong lượt này dù phát hiện nhiều chỗ cần cập nhật theo (root cause đúng cho bug khối #7, thiếu khối Telemetry cho 6 commit mới, Freshness/Retrieval Quality chưa đối chiếu lại) — đúng phạm vi người dùng yêu cầu ("Cập nhật Dashboard") và nguyên tắc không tự ý sửa tài liệu ngoài phạm vi được giao. Đã liệt kê đầy đủ các việc còn thiếu vào `weeklyNextSteps` thay vì tự làm luôn.
- KPI #2 (Rework Rate) và mục Knowledge Health (c1-c7, trừ không đổi) **cố ý để nguyên "không đổi trong lượt này"** thay vì tự tính lại — lượt verify 2026-11-09 là 6 commit code/docs mới chưa có khối Telemetry riêng, tính lại Rework Rate mà không có khối Telemetry nguồn sẽ là suy diễn, không phải verify từ nguồn thật.
- `DASHBOARD-SAFECALL.md`, `dashboard/data.json`, `dashboard/index.html` đã được đối chiếu để đảm bảo đồng bộ 3 chiều (đã verify bằng cách so sánh trực tiếp nội dung khối JSON nhúng trong `index.html` với `data.json`, khớp byte-để-byte) — tránh lặp lại đúng lỗi "2 bản sao lệch nhau" mà chính file này từng tự bắt ở lượt 2026-11-08.
