# Knowledge Health — SAFECALL

> Theo Program §8.2 ("sức khoẻ kho kiến thức"). 7 KPI đo việc bộ tài liệu (REQ/WBS/RTM/spec/test...) có còn là
> **harness đáng tin cho AI** hay không. KPI nào không có nguồn dữ liệu thật để tính → ghi **N/A**, không suy
> diễn/bịa số (cùng luật chống hallucination áp dụng cho `DASHBOARD-SAFECALL.md`).
>
> **Bối cảnh ra đời file này:** được tạo ngay sau khi phát hiện `DASHBOARD-SAFECALL.md` có 2 KPI tính từ số
> liệu cũ (Rework Rate, Governance Gap Rate) — hậu quả trực tiếp của việc không có cơ chế theo dõi Change
> Coupling/Freshness. File này vừa là KPI mới, vừa là bằng chứng cho thấy vì sao KPI này cần tồn tại.

## Bảng KPI

| # | KPI | Đo gì | Công thức | Giá trị thật | Nguồn | Target |
|---|---|---|---|---|---|---|
| c1 | **Traceability Coverage** | Requirement quan trọng (FR+NFR) nối được tới code/test trong RTM | (Req có trace ÷ tổng Req quan trọng) × 100% | **19/20 = 95%** *(cập nhật 2026-11-08, lần 2 — tăng từ 80%)* — `RTM-SAFECALL.md` nay **đã có đủ dòng FR-11/FR-12/FR-13** (gap lượt trước đã đóng): cả 3 đều có Design+Code+Test (🟡 Code-only) ghi trong RTM, tính là "có trace" đúng quy ước đã dùng cho FR-1/FR-9/FR-10. Tử số: 10 FR cũ + FR-11/12/13 + NFR-1/2/3/4/6/7 = 19. Chỉ còn **NFR-5** không trace (deferred chủ động, không phải thiếu sót) | `RTM-SAFECALL.md` (đã cập nhật) đối chiếu `SRS-SafeCall-Phase1.md` v6.1 | ≥80% — **PASS rõ ràng**, không còn "PASS giả" như lượt trước vì RTM đã khớp thực tế |
| c2 | **Change Coupling** | Code/doc đổi thì doc liên quan có đổi theo không | (Change set có doc GOVERNANCE-TRACKING update TRONG CÙNG COMMIT ÷ tổng change set ảnh hưởng code) × 100% | **0/10 = 0%** *(cập nhật 2026-11-08, lần 2 — mẫu số tăng 7→10 với 3 commit mới)* — 3 commit mới (`79daea2` FR-13 backend, `7eefd83` FR-13 Flutter, `78691bd` Settings UI) đều **chỉ đổi code** (`android/`, `lib/`), **0/3 đụng tới `docs/governance/`** — đúng khớp pattern cũ (code và governance-tracking luôn ở commit khác nhau). Commit governance-only kế tiếp (`53fcd9c`) cũng xác nhận pattern: luôn SAU, không bao giờ CÙNG | `git diff --name-only` từng cặp commit liên tiếp (`2f8aefa..79daea2..7eefd83..78691bd..53fcd9c`) | ≥70% — **FAIL, không đổi** — pattern lặp lại nhất quán, không phải ngẫu nhiên |
| c3 | **Freshness** | Tài liệu governance còn khớp với dữ liệu mới nhất trong các file khác | (Doc fresh ÷ tổng doc) × 100% | **7/10 = 70%** *(cập nhật 2026-11-08, lần 3 — tăng từ 50%)* — xét 10 file `docs/governance/*.md`: **Fresh (7):** `DOR-SAFECALL.md` (không ảnh hưởng), `RTM-SAFECALL.md` (đã có FR-1/FR-9/FR-11/FR-12 nâng lên ✅ Tested-Pass, verify tay thật), `DECISION-LOG-SAFECALL.md` (đã có khối #6 + khối #7), `DELEGATION-MAP-SAFECALL.md` (đã có 5 Task Layer-0 mới + mục "Sign-off retroactive 5 gap ⚠️ mới"), `MODULEMAP-SAFECALL.md` (đã liệt kê module Settings + `PermissionCheckWorker`/`SafeCallApplication`), `DASHBOARD-SAFECALL.md` (tự sửa KPI #5 sai — 17.6%→36.4% đúng — trong lượt này), `KNOWLEDGE-HEALTH-SAFECALL.md` (file đang sửa, lượt này). **Stale (3):** `RISK-SAFECALL.md` (chưa có risk nào cho bug `CallMonitorService.handleCallEnded()`/`call_history` phát hiện ở Decision Log khối #7, nghi SQLite lock contention — cần PM gán L/I, không tự thêm ở đây), `TELEMETRY-SAFECALL.md` (0 khối cho phiên verify tay FR-11/FR-12 lẫn phiên viết `ManualQATestScript-SafeCall-Phase1.md`, commit `36a4ab7`), `WEEKLY-SAFECALL.md` (next-step "verify tay FR-11/FR-12" vẫn ghi mở — ĐÃ XONG theo RTM mới, chưa gạch bỏ; chưa liệt kê bug khối #7 là bottleneck mới) | Đối chiếu tay 10 file với RTM/Decision Log/Delegation Map/ModuleMap mới nhất, `git log` (tới commit `36a4ab7`), 2026-11-08 | ≥85% — **vẫn FAIL, cải thiện tiếp (50%→70%)** — 3 file KPI/tracking chính (Dashboard, Knowledge Health, và cả Decision Log/Delegation Map/ModuleMap đã tự sửa bởi phiên khác trước lượt này) nay fresh; chỉ còn Risk/Telemetry/Weekly ngoài phạm vi 2 file này sửa |
| c4 | **Orphan Rate** | Requirement/tài liệu nền không có code/test hoặc không có tài liệu tương ứng | (Orphan req + module chưa có doc) ÷ (tổng req + module) | **1/20 = 5%** *(cập nhật 2026-11-08, lần 2 — giảm từ 10%, tốt hơn cả mốc 5.6% cũ)* — FR-13 **không còn orphan CODE**: `PermissionCheckWorker.kt`/`PermissionStatusChecker.kt`/`SafeCallApplication.kt` đã verify tồn tại thật qua Glob/Grep + 6 test PASS. Chỉ còn **NFR-5** orphan (deferred chủ động, không phải thiếu sót) | `RTM-SAFECALL.md` (đã cập nhật), Glob/Grep xác nhận `PermissionCheckWorker` tồn tại thật | ≤15% — **PASS, cải thiện** |
| c5 | **Review Evidence** | Artefact có dấu vết người sửa/duyệt | (Artefact có trace review ÷ tổng) × 100% | **Vẫn N/A cho tỷ lệ toàn cục** (không đổi logic) — thêm 1 dòng review evidence mới: AI self-review đóng hẳn R9 trong `RISK-SAFECALL.md` (2026-11-08, lần 2), dựa trên bằng chứng `flutter test` tự chạy thật, không suy diễn | `RISK-SAFECALL.md` mục "Log self-review/approve" (nay 6 dòng, +1 từ lượt trước) | ≥90% — không tính %, xu hướng review evidence tiếp tục tăng |
| c6 | **Decision Coverage** | Thay đổi/quyết định lớn có ADR hoặc Decision Log tương ứng | (Thay đổi lớn có ADR/Decision Log ÷ tổng) × 100% | **9/9 = 100%** (không đổi) — `ADR-008` đã cover cả việc triển khai UI/Worker thật (3 commit mới chỉ là THỰC HIỆN quyết định đã có trong ADR-008, không phải quyết định kiến trúc mới cần ADR riêng) | `docs/adr/ADR-001`..`ADR-008` | ≥75% — **PASS**, không đổi |
| c7 | **Retrieval Quality** | Hỏi AI về 1 chức năng, có tìm đúng-đủ spec/code/test/decision không | Spot-check 5-10 câu: đúng-đủ ÷ tổng | **23/24 ≈ 95.8%** *(cập nhật 2026-11-08, lần 3 — tăng từ 95%, xem "Spot-check c7" câu 6 mới)* — câu 6 (FR-9 AC2/AC5, sau khi `ManualQATestScript-SafeCall-Phase1.md` ra đời) tìm đúng-đủ **CẢ 4/4 mục**, bao gồm đúng việc phân biệt "đã có kịch bản test tay cụ thể" với "đã chạy thật" — không bị lạc quan hoá thành Tested-Pass | Xem mục "Spot-check c7" dưới | ≥70% — **PASS**, tăng tiếp |

## Spot-check c7 (bắt buộc theo §8.2 — chạy thật, không suy diễn)

**Câu 1 — "FR-8: xác nhận lừa đảo lúc đổ chuông + ngắt cuộc gọi"**
- Spec: `SRS-SafeCall-Phase1.md` FR-8 (AC1-AC10) — ✅ đúng.
- Code: `RingingScamOverlay.kt`, `RingingEligibility.kt`, `RingingCallNumberHolder.kt`, `CallTerminationOutcome.kt`, `CallMonitorService.kt` — ✅ **đã verify tồn tại thật** bằng `find` trên đĩa (không chỉ tin theo `RTM-SAFECALL.md`).
- Test: ⬜ Not-started (theo `RTM-SAFECALL.md`) — ✅ đúng, khớp `RISK-SAFECALL.md` (R2 "chưa test đa OEM thật").
- Decision record: `ADR-005` (call-termination-mechanism), `ADR-007` (ringing-overlay-mechanism) — ✅ đúng, cả 2 file tồn tại trong `docs/adr/`.

**Câu 2 — "FR-9: chặn theo đầu số (prefix)"**
- Spec: `SRS-SafeCall-Phase1.md` FR-9 (AC1-AC8) — ✅ đúng.
- Code: `EntryType.kt`, `BlocklistPrefixIndex.kt` — ✅ **đã verify tồn tại thật** bằng `find`.
- Test: 🟡 Code-only (RTM) + 6/58 case golden-file mới — ✅ đúng.
- Decision record: ❌ **không tìm được** — ngưỡng "4 ký tự số tối thiểu" chỉ là giả định mặc định ghi trong `SRS §7.1`, **không có ADR nào** cho riêng quyết định này (khác FR-8 vốn có ADR-005/007). Đây là retrieval đúng theo nghĩa "không bịa ra một ADR không tồn tại", nhưng lộ ra 1 lỗ hổng: không có decision record chính thức cho ngưỡng prefix.

**Tổng: 7/8 mục đúng-đủ** (1 mục thiếu ở câu 2 là phát hiện thật — không phải lỗi retrieval mà là gap nguồn, giống tinh thần "N/A thay vì bịa").

**Câu 3 *(mới, 2026-11-08)* — "FR-11: Cơ chế ưu tiên Danh bạ (CONTACTS-PRIORITY)"**
- Spec: `SRS-SafeCall-Phase1.md` v6.1, FR-11 (AC1-AC4), mục thuật ngữ "CONTACTS-PRIORITY" (1.3) — ✅ đúng.
- Code: `android/.../callscreening/db/SafeCallSchema.kt` (cột `contacts_priority_enabled`), `BlocklistRepository.kt` (`getSettingsFlags`), `CallScreeningService.kt` (bước 4 trong pipeline 6 bước) — ✅ **đã verify tồn tại thật qua Grep**. **Nhưng thiếu `SettingsScreen` (Dart)** — đã Glob toàn bộ `lib/**/*.dart`, không có file nào tên Settings — AC1 của FR-11 ("tôi mở màn hình Cài đặt, thấy công tắc") **không có UI để test**. Đây là gap CODE thật, không phải retrieval bỏ sót.
- Test: `CallScreeningServiceTest.kt` — `exactMatch_doesNotCheckContactsPriority`, `prefixMatch_contactsPriorityDisabled_blocksAndRecordsEntryTypePrefixWithValue`, `prefixMatch_contactsPriorityEnabledAndIsContact_allowsWithDecisionReasonContactsPriority` — ✅ 3 test trực tiếp, **đã verify CHẠY THẬT và PASS** qua `build/app/test-results/.../TEST-...CallScreeningServiceTest.xml` (2026-11-08).
- Decision record: `docs/adr/ADR-008` mục "Quyết định 1" — ✅ đúng, đọc trực tiếp nội dung file.

**Câu 4 *(mới, 2026-11-08)* — "FR-12: Bật/Tắt các chế độ chặn nâng cao (BLOCK-TOGGLE)"**
- Spec: `SRS-SafeCall-Phase1.md` v6.1, FR-12 (AC1-AC7) — ✅ đúng.
- Code: `SafeCallSchema.kt` (3 cột `block_unknown/anonymous/international_enabled`), `CallScreeningService.kt` (bước 1 xử lý số ẩn danh, bước 5 xử lý số lạ/quốc tế) — ✅ verify thật qua Grep. **Cùng gap với câu 3**: không có `SettingsScreen` nào để bật/tắt 3 công tắc này.
- Test: `CallScreeningServiceTest.kt` — `presentationRestricted_blockAnonymousEnabled_blocksAndRecordsBlockToggleAnonymous`, `presentationUnknown_blockAnonymousDisabled_allowsAndSetsRinging`, và các test tương tự cho `block_unknown`/`block_international` — ✅ verify CHẠY THẬT PASS qua cùng file `TEST-*.xml` (20 test tổng, `failures="0"`).
- Decision record: `docs/adr/ADR-008` — ✅ đúng (cùng ADR với FR-11, mục "Decision 1" bảng 4 cột).

**Tổng câu 3+4: 7/8 mục đúng-đủ** (Code ở cả 2 câu đều "đúng nhưng không đủ" — phát hiện thật 1 gap lặp lại 2 lần, không phải 2 gap riêng). **Cộng dồn câu 1-4: 15/16 ≈ 93.75%.**

**Câu 5 *(mới, 2026-11-08, lần 2)* — "FR-13: Kiểm tra trạng thái Quyền định kỳ (PERMISSION-CHECK)"**
- Spec: `SRS-SafeCall-Phase1.md` v6.0-v6.1, FR-13 (AC1-AC3) — ✅ đúng.
- Code: `PermissionStatusChecker.kt`, `PermissionCheckWorker.kt`, `SafeCallApplication.kt` — ✅ **đã verify tồn tại thật qua Glob** (không có nữa như lần trước). Không còn gap Code.
- Test: `PermissionStatusCheckerTest.kt` (6 test, MockK) — ✅ **đã verify CHẠY THẬT PASS** qua `TEST-com.safecall.callscreening.PermissionStatusCheckerTest.xml` (`failures="0"`, mtime 14:38 2026-11-08).
- Decision record: `docs/adr/ADR-008` mục "Quyết định 2" — ✅ đúng, đọc trực tiếp nội dung.

**Tổng câu 5: 4/4 mục đúng-đủ** — lần đầu tiên 1 câu spot-check không phát hiện gap nào (kể cả gap thật) vì code/UI/Worker đã đóng đủ. **Cộng dồn câu 1-5: 19/20 = 95%.**

**Câu 6 *(mới, 2026-11-08, lần 3)* — "FR-9 AC2/AC5: đã có kịch bản test tay cụ thể cho validate ngưỡng prefix/cảnh báo trùng contact chưa?"**
- Spec: `SRS-SafeCall-Phase1.md` AC2 (dòng 158) + AC5 (dòng 161) — ✅ đúng, đọc trực tiếp.
- Code: ngưỡng thật nằm ở `phone_number_normalizer.dart` (tổng độ dài ≥5 ký tự tính cả `+`) — ✅ khớp với RTM FR-9 "ngưỡng 4 ký tự tối thiểu".
- Kịch bản test tay: `docs/testing/ManualQATestScript-SafeCall-Phase1.md` mục 2 ("FR-9 AC2/AC5 — validate prefix trên UI") — ✅ **có kịch bản `adb`/thao tác UI cụ thể**, đã verify tồn tại thật qua Read, không suy diễn từ tên commit.
- Trạng thái thật: **CHƯA chạy** — tài liệu tự ghi "Ngày soạn 2026-11-08", không có mục nào ghi kết quả thực thi; RTM FR-9 vẫn ghi "Vẫn chưa verify: AC2 ... và AC5" — ✅ retrieval đúng, không bị cám dỗ nâng FR-9 lên "AC2/AC5 Tested-Pass" chỉ vì đã có kịch bản chạy thử cụ thể (đây là loại hallucination lạc quan giống khối #7 Decision Log, mục tiêu c7 được tạo ra để bắt).

**Tổng câu 6: 4/4 mục đúng-đủ.** **Cộng dồn câu 1-6: 23/24 ≈ 95.8%.**

## Quyết định rút ra từ Knowledge Health (theo đúng tinh thần EX-05 áp cho §8.2)

**Cập nhật 2026-10-25 (lần 3 — stakeholder xác nhận NFR-5/6/7 qua `AskUserQuestion`):** đây là lần đóng gap
ĐÚNG cách — không phải AI tự đặt số liệu để kéo KPI đẹp hơn, mà một quyết định thật được hỏi và trả lời trực
tiếp. Kết quả: **c1 82.4%→94.1%**, **c4 16.7%→5.6% (FAIL→PASS)**. Toàn bộ 7 KPI Knowledge Health giờ chỉ còn
2 KPI không PASS: **c2 N/A** (thiếu lịch sử commit, không phải lỗi) và **c4 đã PASS** — thực ra giờ tất cả đã
PASS hoặc N/A hợp lệ, không còn FAIL nào. Việc còn mở thật, không thể đóng tiếp bằng tài liệu: (1) chạy WBS
Task 7.7.3 thật (đóng test cho cả NFR-4 và NFR-6 cùng lúc); (2) review đối chiếu chi tiết Nghị định 13/2023
với thiết kế hiện tại (NFR-7 mới có khung áp dụng, chưa có review); (3) đóng 3 gap governance (⚠️ Leash A/A+)
— vẫn cần người thật review code/config, không phải việc viết tài liệu.

**Cập nhật 2026-11-08 (audit toàn diện — SRS đã lên v6.1 với FR-11/FR-12/FR-13, governance tracking đứng
yên):** đây là lần đầu tiên "bức tranh lạc quan" của lần audit trước (7/7 KPI PASS/N/A hợp lệ, không FAIL
nào) bị đảo ngược — **không phải vì chất lượng kỹ thuật đi xuống**, mà vì tốc độ ra quyết định nghiệp vụ
(3 FR mới trong 2 ngày, qua `AskUserQuestion` với stakeholder) đã vượt xa tốc độ cập nhật tài liệu tracking.
Kết quả: **c2 N/A→0%/57.1% (tính được thật, FAIL)**, **c3 100%\*→10% (FAIL rõ)**, **c1 94.1%→80% (PASS sát
ngưỡng)**, **c4 5.6%→10% (vẫn PASS nhưng xấu đi, orphan loại mới — FR-13 — đáng lo hơn NFR-5)**. Ngược lại,
**c6 giữ 100%** (ADR-008 đã kịp cover cả 2 quyết định kiến trúc mới) và **c7 tăng nhẹ 87.5%→93.75%** (retrieval
vẫn tìm đúng sự thật, bao gồm cả gap). **Bài học rút ra, khác hẳn các lần trước:** những lần sửa KPI trước đều
là AI tự phát hiện lỗi của CHÍNH MÌNH (tính sai, quên mẫu số). Lần này, nguồn gốc gap là ở NGƯỜI — 3 quyết
định nghiệp vụ được chốt rất nhanh (2026-11-06, qua `AskUserQuestion`) nhưng không có bước nào trong quy trình
yêu cầu đồng bộ lại RTM/Delegation Map/Telemetry SAU MỖI quyết định — khác với cascade SRS→HLD→LLD→ERD→UIUX→
WBS vốn có bước "Phụ lục 7.3" tự nhắc cập nhật. Khuyến nghị cụ thể, không mơ hồ: (1) cập nhật RTM thêm dòng
FR-11/FR-12/FR-13 (có đủ dữ liệu thật để làm NGAY — 33 test Kotlin đã chạy PASS); (2) bổ sung ≥2 risk mới vào
`RISK-SAFECALL.md` (Settings UI thiếu, FR-13 0% code) — cần PM gán L/I, không tự làm ở đây; (3) bổ sung ≥2
task Phase 9/10 vào `DELEGATION-MAP-SAFECALL.md` để xét Leash RETROACTIVE giống 3 gap cũ, trước khi coi là
"đã ký duyệt".

**Cập nhật 2026-11-08 (lần 2, cùng ngày — theo yêu cầu người dùng "update lại dashboard, health check"):**
khuyến nghị (1) ở trên **đã được thực hiện** — nhưng bởi một tác nhân/phiên khác, KHÔNG phải lượt audit này:
khi lượt audit này bắt đầu, `RTM-SAFECALL.md` đã có sẵn FR-11/FR-12/FR-13 (mtime 14:40, mới hơn cả 2 file
Dashboard/Knowledge Health vừa sửa ở lượt trước, mtime 14:06-14:12) — và 3 commit code thật
(`79daea2`/`7eefd83`/`78691bd`) đã đóng đúng 2 gap mà lượt audit trước phát hiện (Settings UI, FR-13 code).
**Phát hiện quan trọng của lượt này:** chính `DASHBOARD-SAFECALL.md`/`KNOWLEDGE-HEALTH-SAFECALL.md` — 2 file
TỒN TẠI để phát hiện staleness — đã trở thành NGUỒN STALENESS chỉ vài chục phút sau khi được sửa, vì RTM tiếp
tục tiến trong khi 2 file này đứng yên. Đây là bằng chứng sống cho thấy Freshness (c3) không phải việc làm
một lần: bất kỳ file nào trong 10 file governance có thể trở thành "nguồn cũ" ngay khi 1 file khác được sửa
sau nó, bất kể file đó mới được sửa bao lâu. Đã tự chạy `flutter test` thật (chưa ai làm trước đó — RTM claim
"PASS" nhưng thiếu bằng chứng trích dẫn) → xác nhận đúng + đóng hẳn Risk R9. Khuyến nghị (2)/(3) ở trên **vẫn
CHƯA được thực hiện** — Risk Register vẫn chưa có risk riêng cho khoảng hở verify-tay còn lại (Settings/
Worker chưa test qua emulator/`WorkManager` thật), Delegation Map vẫn chưa xét Leash cho 5 Task Layer-0.

**Cập nhật 2026-11-08 (lần 3, cùng ngày — theo yêu cầu người dùng lần 3 "update lại dashboard, health check"):**
khuyến nghị (3) ở lần 2 ("xét Leash RETROACTIVE cho 5 Task Layer-0") **đã được thực hiện** — bởi một phiên
khác, không phải lượt audit này: `DELEGATION-MAP-SAFECALL.md` nay có đủ 5 dòng ⚠️ mới + mục "Sign-off
retroactive 5 gap ⚠️ mới", người dùng đã tự duyệt qua `AskUserQuestion`. Freshness (c3) tăng tiếp 50%→70%.
**Phát hiện đáng chú ý nhất của lượt này KHÔNG nằm ở 7 KPI** mà ở việc đối chiếu `DASHBOARD-SAFECALL.md` với
`DELEGATION-MAP-SAFECALL.md` thật: KPI #5 (Governance Gap Rate) của Dashboard đang báo "3/17≈17.6%, Delegation
Map chưa sửa" — một claim ĐÚNG khi được viết, nhưng SAI tại thời điểm audit này vì Delegation Map đã được một
phiên khác cập nhật sau đó. Đã sửa lại đúng thành **8/22≈36.4%** trực tiếp trong `DASHBOARD-SAFECALL.md` (xem
file đó, mục "Phát hiện hallucination, lượt 4"). Đây là một dạng Freshness MỚI chưa từng ghi nhận: không phải
2 file khác nhau lệch nhau (c3 gốc), mà một file **trích dẫn lại chính claim cũ của nó** qua nhiều lượt audit
mà không verify lại nguồn gốc — rủi ro này áp dụng cho CẢ `DASHBOARD-SAFECALL.md` VÀ chính file
`KNOWLEDGE-HEALTH-SAFECALL.md` này ở mọi lượt audit tiếp theo. Còn mở, chưa đóng: (1) `RISK-SAFECALL.md` vẫn
thiếu risk cho bug `CallMonitorService.handleCallEnded()` (Decision Log khối #7) — cần PM gán L/I; (2)
`TELEMETRY-SAFECALL.md`/`WEEKLY-SAFECALL.md` vẫn chưa phản ánh phiên verify tay FR-11/FR-12 lẫn phiên viết
`ManualQATestScript-SafeCall-Phase1.md` — nằm ngoài phạm vi 2 file Dashboard/Knowledge Health mà lượt này
sửa, giống đúng quy luật đã lặp lại 2 lần trước.

## Ghi chú trung thực

- c2 và c5 cố ý để **N/A** thay vì ép ra một con số — đúng luật "thiếu nguồn ⇒ N/A" đã áp dụng nhất quán từ `DASHBOARD-SAFECALL.md`. Không có review-log hay lịch sử commit nhỏ-lẻ để tính 2 KPI này mà không suy diễn.
- c3 (Freshness) chỉ có giá trị tại **1 thời điểm** (2026-10-25, ngay sau khi sửa 2 lỗi stale) — không có cơ chế tự động giữ cho các lần cập nhật sau cũng fresh. Nếu Delegation Map/Telemetry/RTM được sửa tiếp mà không rà lại Dashboard, Freshness sẽ tụt lại dưới 100% mà không ai biết — đây chính là lỗ hổng mà bản thân Knowledge Health KHÔNG tự giải quyết được (cần chạy lại file này mỗi lần có thay đổi lớn, không phải viết 1 lần rồi quên).
- c1 và c4 ban đầu dùng cùng 1 phát hiện gốc (7 NFR không được trace). Sau khi bổ sung RTM, chúng **tách ra đúng như kỳ vọng**: c1 (có trace hay không) PASS vì NFR-1..4 nay có code/test thật; c4 (orphan thật, 0 thiết kế+0 code) vẫn FAIL vì NFR-5..7 là orphan *thật*, không phải lỗi thiếu trace — viết thêm trace không giải quyết được một requirement chưa từng được định nghĩa.
- c7 là KPI duy nhất trong 7 KPI này được **chạy thật** (spot-check + verify file tồn tại qua `find`), không chỉ đọc lại bảng RTM rồi tin theo — đúng yêu cầu "Prompt kiểm c7" của Program §8.2 (không trả lời gì ngoài những gì có trong kho tài liệu).
- **Bài học c6 (2026-10-25, lần 2):** lần tính c6 đầu tiên của chính file này cũng mắc lỗi — tin theo `RISK-SAFECALL.md` nói "ADR-003 chưa cập nhật" mà không mở `ADR-003-local-only-blocklist.md` ra đọc trực tiếp. Đây là ví dụ thật, ngay trong file đo lường "có bịa số/trace sai không", của chính lỗi mà c7 được thiết kế để bắt (tin nguồn thứ cấp thay vì verify nguồn gốc). Đã sửa bằng cách đọc file ADR thật trước khi kết luận.
- **Bài học mới 2026-11-08:** phát hiện 2 file mirror (`DASHBOARD-SAFECALL.md` tường thuật vs `dashboard/data.json` máy đọc) đã lệch nhau ở KPI Rework Rate và Risk Open Rate TỪ TRƯỚC lượt audit này — một bên đã sửa đúng, một bên bị bỏ quên. Không ai phát hiện ra vì không có bước nào đối chiếu 2 file này với nhau, chỉ đối chiếu từng file với nguồn gốc. Áp dụng ngay: mỗi lần sửa KPI, phải sửa ĐỒNG THỜI cả `.md` và `data.json`/`index.html` (đã làm trong lượt audit 2026-11-08 này), và lần kiểm tiếp theo nên tự thêm bước "diff 2 file mirror" vào checklist audit — không chỉ đối chiếu `.md` với nguồn, mà còn đối chiếu `.md` với `data.json`.
- **Bài học mới 2026-11-08 (lần 2):** trước khi sửa KPI #7/c1/c4 lần này, đã CHỦ ĐỘNG tự chạy `flutter test` thay vì tin theo câu "flutter test PASS" đã có sẵn trong `RTM-SAFECALL.md` (câu đó đúng, nhưng lúc viết không trích được bằng chứng chạy thật như cách đã làm cho gradle) — đúng tinh thần c7/"verify nguồn gốc, không tin nguồn thứ cấp" áp dụng NGAY CẢ KHI nguồn thứ cấp (RTM) là do audit trước của chính AI viết ra, không chỉ khi nguồn là của người khác.
- **Bài học mới 2026-11-08 (lần 3):** mở rộng thêm 1 bậc từ bài học lần 2 — không chỉ "không tin nguồn thứ cấp do CHÍNH MÌNH viết ở audit TRƯỚC", mà "không tin nguồn thứ cấp do CHÍNH MÌNH viết Ở CÙNG FILE, MỚI VÀI DÒNG TRƯỚC trong cùng lượt audit". Khi tính lại c3 (Freshness) cho `DASHBOARD-SAFECALL.md`, thay vì chỉ đọc lại KPI #5 của chính Dashboard rồi tính Freshness dựa trên đó, đã mở trực tiếp `DELEGATION-MAP-SAFECALL.md` để đối chiếu — phát hiện KPI #5 đó sai. Nếu chỉ tin theo Dashboard tự báo "Fresh", c3 của Knowledge Health sẽ gián tiếp kế thừa đúng lỗi mà nó được tạo ra để bắt.
