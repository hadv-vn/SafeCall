# Mini Dashboard — SAFECALL

> Theo Workbook EX-05. ≥5 KPI, mỗi KPI có **công thức + nguồn dữ liệu** — KPI nào thiếu nguồn số liệu thật
> ghi rõ **N/A**, không suy diễn/bịa số thay thế (luật chống hallucination của chương trình).

## Bảng KPI

| # | KPI | Công thức | Nguồn dữ liệu | Giá trị |
|---|---|---|---|---|
| 1 | **Nén (Compression)** | giờ truyền thống ước tính ÷ giờ thật đã dùng | Tử số: `estimate.md`/`WBS-SafeCall-Phase1.md` PERT-weighted ≈ 151.5 person-day. Mẫu số: `TELEMETRY-SAFECALL.md` mục "Thời gian" | **N/A** — mẫu số ghi rõ N/A (không có đồng hồ bấm giờ thật), không thể chia |
| 2 | **Rework Rate** | số vòng lặp có phát sinh rework ÷ tổng số vòng lặp quan sát được | `TELEMETRY-SAFECALL.md` cả 2 khối: gốc (~5 vòng, rework ở #1,#2,#3,#4) + bổ sung 2026-10-25 (3 vòng, rework = KHÔNG) | **4/8 = 50%** (đếm tay; cập nhật 2026-10-25 — trước đó ghi nhầm 4/5≈80% vì chỉ tính khối Telemetry gốc, bỏ sót khối bổ sung cùng ngày) |
| 3 | **Human-edit Rate** | số dòng code người dùng tự sửa ÷ tổng thay đổi code | `TELEMETRY-SAFECALL.md` mục "Human-edit" | **0%** (ghi kèm chú thích: không có nghĩa AI không sai — xem Telemetry) |
| 4 | **Test Coverage theo FR** | số FR trạng thái Tested-Pass ÷ tổng số FR | `RTM-SAFECALL.md` (FR-5, FR-7 + 1 phần Database = Tested-Pass; 10 FR tổng) | **2/10 = 20%** (đếm tay từ bảng RTM) |
| 5 | **Governance Gap Rate** | số Task chạy lệch Leash (thực tế ≠ đáng lẽ) ÷ tổng Task ≥ L3 | `DELEGATION-MAP-SAFECALL.md` (3 dòng ⚠️: 1.1.2, 1.3.1, 4.1.1 / 17 Task ≥L3 — đếm cả 2 dòng bổ sung 2026-10-25: gradle signing config, golden-file test) | **3/17 ≈ 17.6%** (cập nhật 2026-10-25 — trước đó ghi nhầm 3/15=20% vì mẫu số chưa tính 2 dòng Delegation Map thêm cùng ngày) |
| 6 | **Risk Open Rate** | số rủi ro (R1-R10) có mitigation ghi "chưa chạy/chưa xác nhận" ÷ tổng 10 rủi ro | `RISK-SAFECALL.md` bảng rủi ro chính | **10/10 = 100%** (không đổi — R9 tiến triển nhưng vẫn 🟡 "đang đóng", chưa đủ điều kiện tính là "đã xác nhận") |
| 7 | **Automated Test Case Count** *(mới, 2026-10-25)* | số test case tự động hiện có trên toàn repo | `test/fixtures/phone_normalizer_golden.json` (đếm phần tử `cases`) | **58** (tăng từ **0** trước 2026-10-25 — lần đầu dự án có test tự động; chưa chạy qua `flutter test`/`gradle test` thật nên chưa tính là "Tested-Pass") |

## Quyết định rút ra từ dashboard (bắt buộc theo EX-05)

**Không release/publish Play Store ở trạng thái hiện tại.** Căn cứ: Test Coverage theo FR chỉ 20% (KPI #4) và
Risk Open Rate 100% (KPI #6) — nghĩa là phần lớn rủi ro kỹ thuật đã liệt kê (đặc biệt R2/R1, Top-2 theo L×I)
chưa có bằng chứng kiểm chứng thật nào ngoài 1 lượt test tay trên 1 emulator. Ưu tiên tiếp theo: đóng Task
8.1.1 (system/E2E qua `adb emu gsm call`, làm được ngay, không cần thiết bị thật) trước khi tính đến bất kỳ
mốc release nào.

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
