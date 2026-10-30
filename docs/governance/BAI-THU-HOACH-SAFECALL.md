# Bài thu hoạch — Dự án SafeCall (ứng dụng AI trong phát triển phần mềm)

> Người thực hiện: hadv-vn · Dự án: SafeCall (chặn cuộc gọi lừa đảo, Android, Phase 1)
> Phạm vi bài thu hoạch: toàn bộ quá trình từ 2026-10-20 đến 2026-10-30, dựa trên bằng chứng thật trong
> `docs/` (SDLC artifacts), `docs/governance/` (Decision Log, Risk Register, Delegation Map, Knowledge
> Health) và lịch sử `git log` — không suy diễn, không tô vẽ lại cho đẹp hơn thực tế đã xảy ra.

## 1. Mô tả bài toán và mục tiêu của dự án

**Bài toán:** người dùng Android bị làm phiền/lừa đảo lặp lại bởi các số điện thoại mà chính họ đã từng
nhận diện là lừa đảo, nhưng không có cơ chế nào để hệ thống "nhớ" và tự chặn lại những số đó cho họ mà
không cần họ tự tay quản lý danh sách. Bài toán KHÔNG phải là xây một hệ thống phát hiện lừa đảo thông
minh (AI/phân tích nội dung cuộc gọi) — đó là quyết định loại trừ rõ ràng ngay từ `requirements-analyst`.

**Mục tiêu thực sự** (phân biệt với mô tả tính năng): một MVP cá nhân hoá, "học" dần theo trải nghiệm của
đúng một người dùng, không cộng đồng, không chia sẻ dữ liệu tự động — nhưng vẫn cho người dùng chủ động
sao lưu/chuyển dữ liệu của chính họ (export/import). Tôi cho rằng đây là điểm quan trọng nhất cần hiểu
đúng trước khi làm bất cứ gì khác: nếu hiểu sai thành "xây một blocklist chia sẻ cộng đồng" thì toàn bộ
kiến trúc (ADR-003 local-only) sẽ sai từ gốc.

**Phạm vi Phase 1 (đã chốt, không tự đổi ở các giai đoạn sau):**
- Android only — iOS bị hoãn có lý do kỹ thuật cụ thể: `CXCallDirectoryExtension` của Apple chỉ hỗ trợ
  sync định kỳ, không có real-time lookup như `CallScreeningService` của Android. Đây không phải "chưa có
  thời gian làm iOS" mà là một ràng buộc nền tảng khiến kiến trúc Phase 1 (chặn real-time) không thể áp
  dụng tương tự bên iOS — phải là một thiết kế khác ở Phase 2, không phải "port" Phase 1 sang.
- Ba đường hình thành blocklist: tự động lúc đổ chuông (SCAM-DETECT-05), tự động sau khi cuộc gọi kết thúc
  (SCAM-DETECT-01..03), và thủ công (BLOCK-01/03) + export/import (BLOCK-04).
- 10 FR, 7 ADR, 8 phase WBS/45 task, ước lượng PERT ~151.5 person-day.

## 2. Cách sử dụng AI trong quá trình thực hiện

Tôi không dùng AI như một "trợ lý gõ code theo yêu cầu rời rạc". Quyết định có chủ đích ngay từ đầu — và
đây là lần tôi phản đối cách làm đầu tiên của AI — là **bắt buộc đi qua chuỗi SDLC hình thức** bằng các
sub-agent chuyên biệt, mỗi agent tạo ra một artifact thật dưới `docs/`, thay vì để AI nhảy thẳng từ hội
thoại sang một bản kế hoạch kỹ thuật (xem [[feedback-sdlc-process]] trong memory — lần đó AI đề xuất thẳng
kiến trúc React Native/Android, tôi yêu cầu "hãy sử dụng quy trình SDLC").

Chuỗi agent đã dùng (nay đã vendor vào `.claude/agents/` của chính repo này để không chỉ còn sống trong
plugin cache toàn cục):

`requirements-analyst → adr-writer/system-architect → high-level-designer → low-level-designer →
data-modeler → ui-ux-designer → test-plan-writer → project-estimator → wbs-writer` → sau đó mới tới bước
code thật.

Ngoài chuỗi thiết kế, AI còn được giao: viết toàn bộ code Phase 1-8 (Dart + Kotlin), tự cài toolchain
(Flutter, Android SDK, Gradle) trong môi trường không có quyền admin, tự build và tự chạy tay trên emulator
thật (API 34), tự viết governance artifacts (Decision Log, Risk Register, Delegation Map, Knowledge
Health...) theo một khung "Workbook" có sẵn, và gần đây nhất: tự đổi theme UI ba lần trong cùng một ngày
theo yêu cầu trực tiếp của tôi (Light Futuristic → Cyberpunk Futuristic → Solarpunk Futuristic).

## 3. Các quyết định quan trọng và lý do lựa chọn giải pháp

Tôi chọn liệt kê những quyết định có đánh đổi thật, không phải quyết định hiển nhiên:

- **Flutter thay vì React Native (ADR-004) — đảo ngược lựa chọn ban đầu.** Hướng nghiêng ban đầu (từ hội
  thoại) là React Native, nhưng khi đi qua `system-architect` một cách hình thức, quyết định bị đảo ngược.
  Tôi đồng ý với lý do này vì bài toán này đụng rất sâu vào native API thời gian thực hai chiều
  (`CallScreeningService`, `TelecomManager`, `WindowManager` overlay) — không phải loại app mà lớp bridge
  JS của RN có lợi thế; Flutter + platform channel mỏng cho Kotlin kiểm soát trực tiếp hot-path hợp lý hơn.
- **Local-only blocklist, không backend (ADR-003).** Đây là quyết định đúng với mục tiêu "cá nhân hoá,
  không cộng đồng" ở mục 1 — nhưng nó có một hệ quả bị bỏ sót lúc đầu rồi phải vá lại: khi thêm tính năng
  Export (FR-10), dữ liệu số điện thoại bên thứ ba lần đầu tiên được CHỦ ĐỘNG đưa ra khỏi thiết bị qua kênh
  người dùng tự chọn (Share Sheet) — khác hẳn giả định ban đầu của ADR-003 là "không cần xử lý privacy vì
  không qua mạng". Tôi coi đây là một bài học thiết kế thật: một ADR chốt sớm vẫn cần được rà lại khi phạm
  vi mở rộng, không phải viết một lần rồi bất biến.
- **`TelecomManager.endCall()` không cần vai trò Dialer mặc định (ADR-005).** Chọn giải pháp này vì
  SafeCall là app `CallScreeningService`, không nên ép người dùng đổi app gọi điện mặc định chỉ để có một
  tính năng phụ (ngắt cuộc gọi lừa đảo lúc đổ chuông). Đánh đổi: giải pháp này chưa được `system-architect`
  xác nhận khả thi 100% trên mọi OEM tại thời điểm SRS viết ra — rủi ro này được giữ nguyên minh bạch trong
  Risk Register (R3) thay vì giả định đã xong.
- **Window overlay (`TYPE_APPLICATION_OVERLAY`) thay vì full-screen-intent Notification (ADR-007).** Đánh
  đổi đã biết trước: overlay loại này không vẽ đè được lên lock screen — nếu cuộc gọi lừa đảo tới lúc máy
  đang khoá, overlay chỉ lộ ra sau khi mở khoá. Chấp nhận hạn chế này thay vì notification vì overlay cho
  tương tác tức thời hơn khi máy đang mở, và mô hình quyền (`SYSTEM_ALERT_WINDOW`) đơn giản hơn để suy luận
  đúng/sai so với full-screen-intent.
- **Ngưỡng tối thiểu 4 ký tự số cho prefix blocking (FR-9).** Đây là quyết định tôi **không** coi là đã
  chốt chắc — nó là giá trị mặc định do AI tự đề xuất để không chặn tiến độ, và Knowledge Health (mục c7,
  câu 2) tự phát hiện ra: không có ADR nào ghi nhận chính thức ngưỡng này, khác với FR-8 (có ADR-005/007).
  Tôi giữ nguyên trạng thái "mặc định, chưa xác nhận" thay vì để AI tự nâng nó lên thành "quyết định đã
  chốt" — vì rủi ro thật (chặn nhầm cả dải số) lớn hơn một con số UI thông thường.
- **Ba lần đổi theme UI trong một ngày.** Đây không phải một quyết định kỹ thuật có đánh đổi sâu, nhưng
  đáng ghi vì nó cho thấy giới hạn của quy trình SDLC hình thức: `ui-ux-designer` ban đầu đề xuất Material 3
  mặc định "cần xác nhận sau" — khi tôi đổi ý thẩm mỹ ba lần liên tiếp, AI cascade đúng cả 3 lần (đổi
  `lib/theme/`, 5 file màn hình, và changelog UIUX lên tới v1.6) mà không phá vỡ ngoại lệ đã chốt riêng cho
  `RingingScamOverlay` (giữ nền tối vì lý do an toàn/độ tương phản alert, không theo theme sáng của app).

## 4. AI làm tốt gì, và chưa làm tốt gì

### AI làm tốt
- **Tuân thủ đúng chuỗi SDLC khi được yêu cầu** và tạo ra 7 ADR + SRS/HLD/LLD/ERD/UIUX/Test Plan/WBS nhất
  quán nội bộ — có thể kiểm tra chéo được (và tôi đã kiểm tra chéo được, xem mục 5).
- **Tìm và tự sửa đúng 4 lỗi thật qua build + test tay thật** (không phải suy luận lý thuyết): file
  `.md` làm hỏng resource merger, `foregroundServiceType="phoneCall"` crash `SecurityException` trên API
  34+, một lỗi logic Dart thật (`OnboardingFlowScreen` không check kết quả `requestPermission` cho bước
  settings-redirect), và lỗi PRAGMA-trong-transaction làm hỏng toàn bộ tầng đọc DB ở cả hai phía Dart/Kotlin
  cùng lúc. Cả 4 lỗi này đều được ghi lại trong Decision Log khối #1-#4 với nguyên nhân gốc, không chỉ
  "đã sửa" mà không giải thích vì sao sai.
- **Tự phát hiện ra gap governance của chính nó**, không chờ tôi chỉ ra: Knowledge Health tự nhận ra
  `DASHBOARD-SAFECALL.md` dùng 2 KPI tính từ số liệu cũ, và Risk Register R7 tự phát hiện chính nó đã từng
  khẳng định sai ("ADR-003 chưa cập nhật") mà không mở file ADR ra đọc trực tiếp — rồi tự sửa lại sau khi
  verify nguồn gốc. Đây đúng là hành vi tôi muốn: không tin nguồn thứ cấp (một ghi chú trong file khác) khi
  nguồn gốc (file ADR thật) có sẵn để đọc.
- **Minh bạch về việc chưa làm được gì**, không che giấu dưới ngôn ngữ mơ hồ: Risk Register ghi rõ R2 (test
  đa OEM thật — điểm rủi ro cao nhất, 20/25), R1, R4, R6, R8 đều ghi "chưa chạy" chứ không tự nhận là
  "Done" dù code đã viết xong.

### AI chưa làm tốt
- **Vi phạm nguyên tắc hard-stop một lần thật** (Decision Log khối #5): khi phát hiện LLD v1.3 ghi "như
  v1.1/v1.2, không đổi" cho 4 hàm pseudocode lõi nhưng bản gốc đó không còn tồn tại trên đĩa, AI đã **tự suy
  luận lại pseudocode từ SRS/ERD/UIUX** thay vì dừng lại hỏi tôi. Đây là lỗi nghiêm trọng nhất trong toàn
  bộ dự án theo đúng nghĩa governance — Risk Register xếp nó L5/Restricted, nghĩa là mức AI tuyệt đối không
  được tự quyết, nhưng AI đã tự quyết. Tôi chủ động không cho phép ghi đè lại lịch sử này.
- **Chọn "viết code trước, verify sau" khi chưa có constraint nào bắt buộc phải làm vậy** — hệ quả trực
  tiếp là cả 4 lỗi ở Decision Log #1-#4 chỉ lộ ra SAU khi build/test thật, muộn hơn mức cần thiết, và theo
  đúng dự đoán trong Risk Register (R11, điểm 16/25) rằng cách làm này sẽ gây lỗi.
- **Không tự gắn cổng người-duyệt (A+) cho các thay đổi chạm quyền hệ thống/DB-layer** trước khi build —
  Delegation Map ghi nhận rõ 3 task (1.1.2, 1.3.1, 4.1.1) đáng lẽ phải ở Leash A+ (có người duyệt manifest/
  schema trước khi cài lên thiết bị thật) nhưng thực tế chạy thẳng ở Leash A, và 2 trong 3 task đó chính là
  nguồn gốc của 2 lỗi crash/hỏng-DB nói trên. Nói cách khác: AI không tự nhận ra lúc nào công việc của nó
  đủ rủi ro để cần dừng chờ duyệt — tôi phải định nghĩa việc đó sau, hồi tố (retroactive), chứ không phải
  trước khi build như đúng quy trình.
- **Luôn có xu hướng coi "có code" = "xong"** nếu không bị tôi hỏi lại — nhiều WBS task kiểm thử quan trọng
  (6.5.3 test OEM, 7.7.3 sanity-check hiệu năng 10.000 entry, 4.4.2 race-condition ≥20 lần lặp, 7.7.5 test
  crash giữa import) đều mới chỉ ở trạng thái "đã code đúng theo LLD" nhưng CHƯA được chạy thật — nếu tôi
  không duy trì Risk Register tách biệt "code xong" ra khỏi "verify xong", dễ nhầm lẫn thành Phase đã Done.

## 5. Phản biện, kiểm chứng và đánh giá đầu ra của AI — quá trình tư duy của tôi

Tôi không chấp nhận bất kỳ tài liệu/kết luận nào của AI chỉ vì nó "nhìn có vẻ đầy đủ". Cách tôi kiểm chứng
cụ thể trong dự án này:

1. **Không để AI tự chấm bài chính nó cho các cổng rủi ro cao.** Task 8.2.1 (security review cuối) và
   8.3.1 (UAT) bị tôi cố ý giữ ở mức ủy quyền thấp (L2/L1) dù AI hoàn toàn có khả năng kỹ thuật để tự làm —
   lý do: nếu để AI tự đánh giá "code bảo mật do chính nó viết là đủ an toàn", đó là rubber-stamp, không
   phải kiểm chứng thật. Tương tự, khi AI đề xuất "đóng retroactive" 3 gap governance (1.1.2/1.3.1/4.1.1),
   tôi không duyệt theo mô tả — tôi yêu cầu xem **bằng chứng kỹ thuật thật** (nội dung file `.kt`/`.dart`
   thật tại thời điểm đó) trước khi ký `AskUserQuestion`, và quyết định giữ nguyên các dòng ⚠️ trong
   Delegation Map thay vì xoá lịch sử — vì sự thật "đã chạy sai Leash" không đổi chỉ vì sau đó được review.
2. **Buộc AI tách rõ "đã code" khỏi "đã verify bằng toolchain thật".** Ví dụ rõ nhất là R9 (lệch
   `PhoneNumberNormalizer` giữa Dart/Kotlin) — AI viết golden-file test và tự verify bằng oracle Python độc
   lập (58/58 khớp), nhưng tôi không cho hạ điểm rủi ro về 0 vì AI thừa nhận chưa chạy qua `flutter test`/
   `gradle test` thật trong môi trường nó có. Đây là một phân biệt tôi coi là bắt buộc: "test đã viết và tự
   verify logic" khác "test đã thực thi qua đúng toolchain production".
3. **Khi AI tự phát hiện lỗi của chính nó, tôi kiểm tra lại thay vì tin ngay.** Trường hợp R7 (Export có
   mâu thuẫn với ADR-003 không) — AI từng ghi "ADR-003 chưa cập nhật", sau đó tự sửa thành "đã cập nhật từ
   2026-10-21, lỗi do không đọc file gốc". Tôi coi việc AI tự lật lại kết luận cũ của chính nó là tín hiệu
   tốt (không giấu lỗi), nhưng không coi phát hiện lần hai là chân lý cuối cùng chỉ vì nó "nghe hợp lý hơn"
   — cách đúng là mở trực tiếp `ADR-003-local-only-blocklist.md` ra đọc, đúng như AI mô tả đã làm, và việc
   đó có thể tái lập được (ai cũng mở được file đó ra kiểm tra lại).
4. **Phân biệt quyết định thẩm mỹ với quyết định kiến trúc khi đánh giá rủi ro cascade.** Khi tôi đổi theme
   UI ba lần, tôi không áp cùng mức soát xét như với ADR — vì không có FR/NFR nào thay đổi, rủi ro cascade
   chỉ nằm ở việc có bỏ sót màn hình/ngoại lệ nào không (ví dụ `RingingScamOverlay` giữ nền tối). Tôi kiểm
   bằng cách đọc lại diff thật của từng lần đổi (không chỉ tin changelog UIUX tự ghi) trước khi cho phép
   commit — và xác nhận ngoại lệ overlay được giữ nguyên đúng ở cả 3 lần.
5. **Giữ nguyên bằng chứng bất lợi cho AI thay vì để nó tự xoá.** Decision Log khối #5 (vi phạm hard-stop)
   không có mục "sửa thế nào" vì đúng là không có sửa trực tiếp nào — tôi chủ động giữ nguyên trạng thái
   "ghi nhận vi phạm, chưa giải quyết" thay vì để AI tự đóng gap này bằng cách tự suy luận thêm một lần
   nữa. Đây là ranh giới tôi tự đặt ra: một lỗi loại "tự quyết logic lõi khi thiếu input" không được phép
   tự sửa bằng chính cơ chế đã gây ra lỗi đó.

**Kết luận của tôi sau quá trình này:** AI hiệu quả nhất khi được ép đi qua một quy trình có cấu trúc rõ
(SDLC + governance artifacts bắt buộc), vì chính cấu trúc đó tạo ra các điểm để tôi — hoặc AI tự nó — bắt
được sai sót trước khi chúng lan rộng. Nhưng cấu trúc đó không tự động đảm bảo đúng nếu không có người thật
đọc lại nguồn gốc (không phải tóm tắt), từ chối rubber-stamp ở các cổng rủi ro cao, và chủ động giữ lại
bằng chứng bất lợi thay vì để AI viết lại lịch sử gọn đẹp hơn thực tế.
