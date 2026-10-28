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
| c1 | **Traceability Coverage** | Requirement quan trọng (FR+NFR) nối được tới code/test trong RTM | (Req có trace ÷ tổng Req quan trọng) × 100% | **16/17 ≈ 94.1%** *(cập nhật 2026-10-25, lần 3 — sau khi stakeholder xác nhận NFR-5/6/7)* — 10 FR + NFR-1/2/3/4/6/7 đều có Design quyết định + code/test thật (hoặc kế hoạch test) trong `RTM-SAFECALL.md`; **chỉ NFR-5 còn KHÔNG trace** — nhưng nay vì stakeholder **chủ động defer sang Phase 2**, không phải vì thiếu sót | `RTM-SAFECALL.md` đối chiếu `SRS-SafeCall-Phase1.md` §4 v3.1 (changelog, quyết định stakeholder 2026-10-25) | ≥80% — **PASS**, tăng từ 58.8%→82.4%→94.1% qua 2 lần sửa |
| c2 | **Change Coupling** | Code/doc đổi thì doc liên quan có đổi theo không | (Change set có doc update ÷ tổng change set ảnh hưởng doc) × 100% | **N/A** — không tính được: repo chỉ có 3 commit (`git log`: `0cc6211`→`c16bf95`→`fbb5555`), gần như toàn bộ `docs/governance/` + `test/` + code Phase 1-8 đang là **1 khối uncommitted** (`git status`), không có lịch sử change-set nhỏ để đối chiếu | `git log --oneline`, `git status --short` (chạy thật 2026-10-25) | Không áp dụng được — thiếu dữ liệu, không phải 0% |
| c3 | **Freshness** | Tài liệu governance còn khớp với dữ liệu mới nhất trong các file khác | (Doc fresh ÷ tổng doc) × 100% | **8/8 = 100% tại thời điểm audit 2026-10-25** — sau khi sửa 2 lỗi stale (KPI Rework Rate, Governance Gap Rate trong `DASHBOARD-SAFECALL.md`) phát hiện trong chính audit này. Đây là số liệu **point-in-time**, không phải cơ chế tự động | Đối chiếu tay toàn bộ 8 file `docs/governance/*.md` ngày 2026-10-25 | ≥85% — **PASS (vừa sửa xong, chưa có cơ chế giữ fresh lâu dài)** |
| c4 | **Orphan Rate** | Requirement/tài liệu nền không có code/test hoặc không có tài liệu tương ứng | (Orphan req + module chưa có doc) ÷ (tổng req + module) | **1/18 ≈ 5.6%** *(cập nhật 2026-10-25, lần 3)* — tử số: chỉ còn **NFR-5** orphan toàn diện (deferred sang Phase 2 theo quyết định stakeholder, không phải thiếu sót); NFR-6/7 không còn orphan (có Design quyết định rõ); "module chưa có doc" đã đóng từ lần sửa trước; mẫu số: 17 req + 1 hạng mục Module Map | `RTM-SAFECALL.md` (mục "NFR mồ côi"), `MODULEMAP-SAFECALL.md` | ≤15% — **PASS** (trước đó 44.4%→22.2%→16.7%→5.6% qua 3 lần sửa cùng ngày) |
| c5 | **Review Evidence** | Artefact có dấu vết người sửa/duyệt | (Artefact có trace review ÷ tổng) × 100% | **N/A** — không có review-log tách biệt theo artefact. `TELEMETRY-SAFECALL.md` ghi **Human-edit = 0** cho toàn bộ Phase 1-8 (người dùng duyệt ở tầng quyết định — chọn mức ủy quyền, trả lời `AskUserQuestion` — không sửa trực tiếp dòng nào). Không có cách tính "% artefact có review" mà không suy diễn | `TELEMETRY-SAFECALL.md` (cả 2 khối) | ≥90% — không tính được, không ép về 0% hay 100% |
| c6 | **Decision Coverage** | Thay đổi/quyết định lớn có ADR hoặc Decision Log tương ứng | (Thay đổi lớn có ADR/Decision Log ÷ tổng) × 100% | **8/8 = 100%** *(sửa 2026-10-25, lần 2)* — 7 quyết định kiến trúc lớn có ADR (`ADR-001`..`ADR-007`); quyết định Export/Share Sheet (FR-10) **đã được `ADR-003` phản ánh từ 2026-10-21** (mục "Cập nhật 2026-10-21"), **sớm hơn** cả `RISK-SAFECALL.md`/`WEEKLY-SAFECALL.md` — 2 file đó từng ghi sai là "chưa cập nhật", đã verify lại file ADR thật và sửa | `docs/adr/ADR-001`..`ADR-007` (đọc trực tiếp nội dung, không chỉ tin theo `RISK-SAFECALL.md`) | ≥75% — **PASS**, 0 gap (trước đó tưởng có 1 gap nhưng đó là lỗi của Risk/Weekly, không phải của ADR-003) |
| c7 | **Retrieval Quality** | Hỏi AI về 1 chức năng, có tìm đúng-đủ spec/code/test/decision không | Spot-check 5-10 câu: đúng-đủ ÷ tổng | **7/8 ≈ 87.5%** — spot-check thật 2026-10-25 (2 câu × 4 hạng mục, xem chi tiết dưới); 6 Kotlin file được liệt kê đã **verify tồn tại thật qua `find`**, không chỉ chép lại từ RTM | Xem mục "Spot-check c7" dưới | ≥70% — **PASS** |

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

## Quyết định rút ra từ Knowledge Health (theo đúng tinh thần EX-05 áp cho §8.2)

**Cập nhật 2026-10-25 (lần 3 — stakeholder xác nhận NFR-5/6/7 qua `AskUserQuestion`):** đây là lần đóng gap
ĐÚNG cách — không phải AI tự đặt số liệu để kéo KPI đẹp hơn, mà một quyết định thật được hỏi và trả lời trực
tiếp. Kết quả: **c1 82.4%→94.1%**, **c4 16.7%→5.6% (FAIL→PASS)**. Toàn bộ 7 KPI Knowledge Health giờ chỉ còn
2 KPI không PASS: **c2 N/A** (thiếu lịch sử commit, không phải lỗi) và **c4 đã PASS** — thực ra giờ tất cả đã
PASS hoặc N/A hợp lệ, không còn FAIL nào. Việc còn mở thật, không thể đóng tiếp bằng tài liệu: (1) chạy WBS
Task 7.7.3 thật (đóng test cho cả NFR-4 và NFR-6 cùng lúc); (2) review đối chiếu chi tiết Nghị định 13/2023
với thiết kế hiện tại (NFR-7 mới có khung áp dụng, chưa có review); (3) đóng 3 gap governance (⚠️ Leash A/A+)
— vẫn cần người thật review code/config, không phải việc viết tài liệu.

## Ghi chú trung thực

- c2 và c5 cố ý để **N/A** thay vì ép ra một con số — đúng luật "thiếu nguồn ⇒ N/A" đã áp dụng nhất quán từ `DASHBOARD-SAFECALL.md`. Không có review-log hay lịch sử commit nhỏ-lẻ để tính 2 KPI này mà không suy diễn.
- c3 (Freshness) chỉ có giá trị tại **1 thời điểm** (2026-10-25, ngay sau khi sửa 2 lỗi stale) — không có cơ chế tự động giữ cho các lần cập nhật sau cũng fresh. Nếu Delegation Map/Telemetry/RTM được sửa tiếp mà không rà lại Dashboard, Freshness sẽ tụt lại dưới 100% mà không ai biết — đây chính là lỗ hổng mà bản thân Knowledge Health KHÔNG tự giải quyết được (cần chạy lại file này mỗi lần có thay đổi lớn, không phải viết 1 lần rồi quên).
- c1 và c4 ban đầu dùng cùng 1 phát hiện gốc (7 NFR không được trace). Sau khi bổ sung RTM, chúng **tách ra đúng như kỳ vọng**: c1 (có trace hay không) PASS vì NFR-1..4 nay có code/test thật; c4 (orphan thật, 0 thiết kế+0 code) vẫn FAIL vì NFR-5..7 là orphan *thật*, không phải lỗi thiếu trace — viết thêm trace không giải quyết được một requirement chưa từng được định nghĩa.
- c7 là KPI duy nhất trong 7 KPI này được **chạy thật** (spot-check + verify file tồn tại qua `find`), không chỉ đọc lại bảng RTM rồi tin theo — đúng yêu cầu "Prompt kiểm c7" của Program §8.2 (không trả lời gì ngoài những gì có trong kho tài liệu).
- **Bài học c6 (2026-10-25, lần 2):** lần tính c6 đầu tiên của chính file này cũng mắc lỗi — tin theo `RISK-SAFECALL.md` nói "ADR-003 chưa cập nhật" mà không mở `ADR-003-local-only-blocklist.md` ra đọc trực tiếp. Đây là ví dụ thật, ngay trong file đo lường "có bịa số/trace sai không", của chính lỗi mà c7 được thiết kế để bắt (tin nguồn thứ cấp thay vì verify nguồn gốc). Đã sửa bằng cách đọc file ADR thật trước khi kết luận.
