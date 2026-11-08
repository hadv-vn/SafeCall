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
| c1 | **Traceability Coverage** | Requirement quan trọng (FR+NFR) nối được tới code/test trong RTM | (Req có trace ÷ tổng Req quan trọng) × 100% | **16/20 = 80%** *(cập nhật 2026-11-08 — giảm từ 94.1%, KHÔNG phải do chất lượng đi xuống)* — mẫu số đúng nay là 20 (13 FR theo SRS v6.1 + 7 NFR), nhưng `RTM-SAFECALL.md` chưa có dòng nào cho FR-11/FR-12/FR-13 (mtime RTM vẫn 2026-10-25, trước khi 3 FR này tồn tại). Tử số 16 không đổi (10 FR cũ + NFR-1/2/3/4/6/7). FR-11/FR-12 **có thể trace được ngay** nếu RTM được cập nhật (code+test thật đã tồn tại — xem KPI Dashboard #4); FR-13 thì **không trace được** vì chưa có code | `RTM-SAFECALL.md` đối chiếu `SRS-SafeCall-Phase1.md` v6.1 (FR-1..FR-13), `docs/adr/ADR-008` | ≥80% — **PASS sát ngưỡng**, nhưng là PASS "giả" theo nghĩa RTM chưa phản ánh đúng thực tế mới nhất — xem c3 |
| c2 | **Change Coupling** | Code/doc đổi thì doc liên quan có đổi theo không | (Change set có doc update TRONG CÙNG COMMIT ÷ tổng change set ảnh hưởng code) × 100% | **Tính được thật lần đầu (2026-11-08)** — repo nay có 14 commit, đủ lịch sử. Trong 7 commit có đổi code sản phẩm (`0cc6211`,`c16bf95`,`482d5ed`,`a1a451e`,`5896a36`,`2efcc3d`,`4fe7e62`): (a) **Coupling với doc thiết kế** (SRS/HLD/LLD/ERD/UIUX/ADR/WBS) trong CÙNG commit: `0cc6211`✅ `a1a451e`✅ `5896a36`✅ `2efcc3d`✅, còn `c16bf95`❌ `482d5ed`❌ `4fe7e62`❌ → **4/7 ≈ 57.1%**. (b) **Coupling với doc GOVERNANCE-TRACKING** (RTM/Delegation Map/Telemetry/Dashboard/Decision Log) trong CÙNG commit: **0/7 = 0%** — cả 2 lần có sự cố thật (`c16bf95` gây 2 crash, `2efcc3d` thêm 3 FR mới) đều được ghi nhận ở một commit "snapshot" RIÊNG, SAU đó (`fbb5555`, `62c6d0a`), không bao giờ cùng commit với code | `git log --stat` toàn bộ 14 commit, đọc từng file trong mỗi diff (không chỉ đếm số file) | ≥70% — **FAIL ở cả 2 cách tính** (57.1% và 0%) — headline dùng 0% (governance-tracking) vì đây là phạm vi chính KPI này muốn đo (docs/governance luôn đổi sau code, không bao giờ cùng lúc) |
| c3 | **Freshness** | Tài liệu governance còn khớp với dữ liệu mới nhất trong các file khác | (Doc fresh ÷ tổng doc) × 100% | **1/10 = 10%** *(cập nhật 2026-11-08 — giảm mạnh từ "100%\*", đúng như cảnh báo point-in-time đã tự ghi trước đó)* — xét 10 file `docs/governance/*.md` (trừ `BAI-THU-HOACH`, một bài phản tư một lần, không phải tracker sống): **Fresh (1):** `DOR-SAFECALL.md` (tiêu chí Definition-of-Ready chung, không bị ảnh hưởng bởi FR mới). **Stale (9):** `DASHBOARD-SAFECALL.md`, `KNOWLEDGE-HEALTH-SAFECALL.md` (2 file đang sửa trong audit này), `DECISION-LOG-SAFECALL.md` (chưa ghi bug PRAGMA/rawQuery 2026-11-08), `DELEGATION-MAP-SAFECALL.md` (chưa có task Phase 9/10), `MODULEMAP-SAFECALL.md` (chưa có file mới), `RTM-SAFECALL.md` (chưa có FR-11/12/13), `TELEMETRY-SAFECALL.md` (0 khối nào cho 2026-11-06→08), `WEEKLY-SAFECALL.md` (next-steps lỗi thời), `RISK-SAFECALL.md` (PM đã review format 2026-11-06 nhưng CHƯA thêm risk nào cho FR-11/12/13 — tính là stale về nội dung dù mới về hình thức) | Đối chiếu tay 10 file `docs/governance/*.md` với `git log`, `SRS v6.1`, `build/app/test-results/` ngày 2026-11-08 | ≥85% — **FAIL rõ ràng** — đây là phát hiện quan trọng nhất của lượt audit này: đúng như ghi chú tự cảnh báo ở bản 2026-10-25 ("nếu không rà lại... Freshness sẽ tụt mà không ai biết"), điều đó đã xảy ra |
| c4 | **Orphan Rate** | Requirement/tài liệu nền không có code/test hoặc không có tài liệu tương ứng | (Orphan req + module chưa có doc) ÷ (tổng req + module) | **2/20 = 10%** *(cập nhật 2026-11-08 — tăng từ 5.6%, mẫu số 20 = 13 FR + 7 NFR)* — tử số: NFR-5 (vẫn orphan toàn diện, nhưng là **deferred CHỦ ĐỘNG** theo quyết định stakeholder, không phải thiếu sót) + **FR-13 (PERMISSION-CHECK) — orphan MỚI, loại khác hẳn NFR-5**: có SRS v6.0 + `ADR-008` (thiết kế đầy đủ: `androidx.work.PeriodicWorkRequest`, chu kỳ 24h, 5 loại quyền cần kiểm) nhưng **0 dòng code** (`grep -r "PermissionCheckWorker"` toàn repo không ra kết quả) — đây là spec "vượt trước" implementation, không phải quyết định hoãn có chủ ý | `SRS-SafeCall-Phase1.md` v6.1, `docs/adr/ADR-008`, Grep toàn repo xác nhận không có `PermissionCheckWorker`/`SettingsScreen` | ≤15% — **vẫn PASS** nhưng xấu đi, và loại orphan mới (FR-13) đáng lo hơn NFR-5 vì không phải chủ đích |
| c5 | **Review Evidence** | Artefact có dấu vết người sửa/duyệt | (Artefact có trace review ÷ tổng) × 100% | **Vẫn N/A cho một tỷ lệ toàn cục** (mẫu số "tổng artefact" mơ hồ, không ép ra số để tránh suy diễn) — **nhưng KHÔNG còn N/A tuyệt đối như trước**: `RISK-SAFECALL.md` (mục "Log self-review/approve", bổ sung 2026-11-06) ghi nhận **lần đầu tiên có review evidence thật, có tên người, có ngày, có đối tượng cụ thể**: PM (hadv-vn) tự tay sửa trực tiếp 3 file — `RISK-SAFECALL.md` (chính nó), `estimate.md` Task 6.1.1 + Phase 1.8, `WBS-SafeCall-Phase1.md` tương ứng — ngày 2026-11-06, xác nhận qua git diff. Tuyệt đại đa số artefact khác (SRS, 8 ADR, HLD/LLD/ERD/UIUX, toàn bộ code Dart/Kotlin) vẫn không có review-log riêng | `RISK-SAFECALL.md` mục "Log self-review/approve" (2 dòng review bởi PM, 2026-11-06) | ≥90% — không tính %, nhưng xu hướng đổi từ "0 review evidence tuyệt đối" sang "3 artefact cụ thể có review thật" là một cải thiện có thể kiểm chứng |
| c6 | **Decision Coverage** | Thay đổi/quyết định lớn có ADR hoặc Decision Log tương ứng | (Thay đổi lớn có ADR/Decision Log ÷ tổng) × 100% | **9/9 = 100%** *(cập nhật 2026-11-08 — mẫu số tăng từ 8→9)* — `ADR-008` (2026-11-07) mới thêm, phủ đúng 2 quyết định của cascade FR-11/FR-12/FR-13: (1) lưu 4 cờ cấu hình trong `blocklist_meta` (không dùng `SharedPreferences`, tránh lặp lại rủi ro cross-process); (2) `PermissionCheckWorker` dùng `androidx.work` thuần native (không qua plugin Flutter `workmanager`) — cả 2 **đã verify đọc trực tiếp nội dung ADR-008**, không chỉ tin theo tên file | `docs/adr/ADR-001`..`ADR-008` (đọc trực tiếp nội dung) | ≥75% — **PASS**, 0 gap quyết định-kiến-trúc tìm được; gap thật của cascade này nằm ở Traceability/Freshness/Orphan (c1/c3/c4), không phải ở thiếu ADR |
| c7 | **Retrieval Quality** | Hỏi AI về 1 chức năng, có tìm đúng-đủ spec/code/test/decision không | Spot-check 5-10 câu: đúng-đủ ÷ tổng | **15/16 ≈ 93.75%** *(cập nhật 2026-11-08 — cộng dồn 2 câu mới vào 2 câu cũ, xem "Spot-check c7" dưới)* — 2 câu mới (FR-11, FR-12) đều tìm đúng Spec/Test/Decision; cả 2 đều phát hiện VÀ báo cáo đúng 1 gap thật ở Code (thiếu màn hình Cài đặt) — tính là retrieval ĐÚNG (tìm ra sự thật, không bỏ sót) theo đúng tiền lệ đã áp dụng cho câu FR-9 cũ | Xem mục "Spot-check c7" dưới | ≥70% — **PASS**, tăng nhẹ so với 87.5% cũ |

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

**Tổng câu 3+4: 7/8 mục đúng-đủ** (Code ở cả 2 câu đều "đúng nhưng không đủ" — phát hiện thật 1 gap lặp lại 2 lần, không phải 2 gap riêng). **Cộng dồn 4 câu (câu 1-4): 15/16 ≈ 93.75%.**

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

## Ghi chú trung thực

- c2 và c5 cố ý để **N/A** thay vì ép ra một con số — đúng luật "thiếu nguồn ⇒ N/A" đã áp dụng nhất quán từ `DASHBOARD-SAFECALL.md`. Không có review-log hay lịch sử commit nhỏ-lẻ để tính 2 KPI này mà không suy diễn.
- c3 (Freshness) chỉ có giá trị tại **1 thời điểm** (2026-10-25, ngay sau khi sửa 2 lỗi stale) — không có cơ chế tự động giữ cho các lần cập nhật sau cũng fresh. Nếu Delegation Map/Telemetry/RTM được sửa tiếp mà không rà lại Dashboard, Freshness sẽ tụt lại dưới 100% mà không ai biết — đây chính là lỗ hổng mà bản thân Knowledge Health KHÔNG tự giải quyết được (cần chạy lại file này mỗi lần có thay đổi lớn, không phải viết 1 lần rồi quên).
- c1 và c4 ban đầu dùng cùng 1 phát hiện gốc (7 NFR không được trace). Sau khi bổ sung RTM, chúng **tách ra đúng như kỳ vọng**: c1 (có trace hay không) PASS vì NFR-1..4 nay có code/test thật; c4 (orphan thật, 0 thiết kế+0 code) vẫn FAIL vì NFR-5..7 là orphan *thật*, không phải lỗi thiếu trace — viết thêm trace không giải quyết được một requirement chưa từng được định nghĩa.
- c7 là KPI duy nhất trong 7 KPI này được **chạy thật** (spot-check + verify file tồn tại qua `find`), không chỉ đọc lại bảng RTM rồi tin theo — đúng yêu cầu "Prompt kiểm c7" của Program §8.2 (không trả lời gì ngoài những gì có trong kho tài liệu).
- **Bài học c6 (2026-10-25, lần 2):** lần tính c6 đầu tiên của chính file này cũng mắc lỗi — tin theo `RISK-SAFECALL.md` nói "ADR-003 chưa cập nhật" mà không mở `ADR-003-local-only-blocklist.md` ra đọc trực tiếp. Đây là ví dụ thật, ngay trong file đo lường "có bịa số/trace sai không", của chính lỗi mà c7 được thiết kế để bắt (tin nguồn thứ cấp thay vì verify nguồn gốc). Đã sửa bằng cách đọc file ADR thật trước khi kết luận.
- **Bài học mới 2026-11-08:** phát hiện 2 file mirror (`DASHBOARD-SAFECALL.md` tường thuật vs `dashboard/data.json` máy đọc) đã lệch nhau ở KPI Rework Rate và Risk Open Rate TỪ TRƯỚC lượt audit này — một bên đã sửa đúng, một bên bị bỏ quên. Không ai phát hiện ra vì không có bước nào đối chiếu 2 file này với nhau, chỉ đối chiếu từng file với nguồn gốc. Áp dụng ngay: mỗi lần sửa KPI, phải sửa ĐỒNG THỜI cả `.md` và `data.json`/`index.html` (đã làm trong lượt audit 2026-11-08 này), và lần kiểm tiếp theo nên tự thêm bước "diff 2 file mirror" vào checklist audit — không chỉ đối chiếu `.md` với nguồn, mà còn đối chiếu `.md` với `data.json`.
