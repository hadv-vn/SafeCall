# High-Level Design: SafeCall Phase 1 (Android, local-only call-blocking)

### Lịch sử phiên bản
| Phiên bản | Ngày | Thay đổi |
|---|---|---|
| 1.0 | 2026-10-20 | Bản HLD gốc cho FR-1..FR-7 (ADR-001..ADR-004). |
| 1.1 | 2026-10-21 | Bổ sung FR-8/SCAM-DETECT-05: thành phần mới `RingingScamOverlay`, mở rộng `CallMonitorService`, luồng chính 4, Phase 1.6, mục bảo mật mới. |
| 1.2 | 2026-10-21 | Cập nhật theo SRS v3.0: (a) `CallScreeningService`/FR-1 khớp CẢ exact VÀ prefix, nêu rủi ro hiệu năng tra cứu (NFR-4); (b) thêm FR-9/BLOCK-03 (chặn theo đầu số) dùng CHUNG cơ chế với FR-5, chỉ khác `entry_type`; (c) thêm thành phần MỚI `BlocklistExportService`/`BlocklistImportService` (Dart, hoàn toàn trong `lib/`, KHÔNG qua MethodChannel/native) cho FR-10/BLOCK-04; (d) Luồng chính 6 (Export/Import); (e) Phase 1.7; (f) mục bảo mật về rủi ro export dữ liệu bên thứ ba, không mã hóa ở Phase 1, theo ADR-003 cập nhật. **Ngoài phạm vi lần này**: FR-6/CALLHISTORY-01 đã được SRS v3.0 cập nhật (phân biệt nguồn chặn exact/prefix) nhưng CHƯA phản ánh ở HLD — cần một lượt cập nhật riêng. |
| 1.3 | 2026-11-07 | Cập nhật theo SRS v6.0: (a) **FR-11/CONTACTS-PRIORITY**, **FR-12/BLOCK-TOGGLE**, **FR-13/PERMISSION-CHECK** mới; (b) `CallScreeningService` mở rộng pipeline quyết định chặn thành 6 bước (anonymous → BLOCK-02 exact → prefix+CONTACTS-PRIORITY → BLOCK-TOGGLE → SCAM-DETECT); (c) xác nhận `ContactsRepository`/`ContactsLookupModule` (đã có trong code, LLD v1.3) là nơi `CallScreeningService` gọi trực tiếp in-process cho tra cứu contact — KHÔNG tạo lớp mới; sơ đồ thành phần v1.2 trước đó đang THIẾU 2 node này dù đã có trong code từ cascade FR-5/FR-9 — bổ sung lại cho khớp thực tế; (d) mở rộng `BlocklistRepository` (native) đọc 4 cờ mới từ `blocklist_meta` (ADR-008); (e) thành phần MỚI: `SettingsScreen`+`SettingsService` (Dart), `PermissionCheckWorker`+`SafeCallApplication` (native); (f) Phase 1.8/1.9/1.10. **Ngoài phạm vi lần này**: cập nhật chi tiết `CallHistoryRepository`/`CallHistoryScreen` cho lý do chặn mới (FR-6/AC4, AC5) — chỉ nêu điểm chạm, chưa thiết kế schema; ranh giới `Context` vs `Activity` giữa `PermissionCheckWorker` và `PermissionsModule`/`RoleModule` — chuyển `low-level-designer`; cách tích hợp `SafeCallApplication` với `android:name="${applicationName}"` hiện tại — chuyển `system-architect`/`low-level-designer`. |
| 1.4 | 2026-11-07 | **Sửa lỗi tài liệu (document-review)**: hai ghi chú "Ngoài phạm vi" ở v1.2/v1.3 về `CallHistoryRepository` (dòng ~164, ~273) đã LỖI THỜI — không còn phản ánh đúng thực tế sau cascade FR-11/FR-12/FR-13 cùng ngày (2026-11-07): LLD v1.4 và ERD v1.3 đã thiết kế và ĐÓNG xong phần backend/schema (`wasBlocked`/`decisionReason` tường minh trên `CallRecordDraft`, cột `decision_reason` + CHECK constraint trên `call_history`). Thu hẹp phạm vi gap còn mở từ "component `CallHistoryRepository`, chưa phản ánh" xuống đúng còn lại: **chỉ tầng hiển thị UI** (`CallHistoryScreen.deriveDisplayStatus`), đã tracked độc lập ở LLD v1.4/WBS/Test Plan v1.3. Không có thay đổi kiến trúc/thành phần nào khác trong lượt này — thuần sửa text cho khớp thực tế. |

## Phương án kiến trúc đã chốt
- Flutter (UI/business logic chung, `lib/`) + native module Kotlin riêng (`android/app/src/main/kotlin/com/safecall/callscreening/`) cho toàn bộ chức năng chặn/giám sát cuộc gọi; giao tiếp qua MethodChannel/EventChannel; blocklist/lịch sử cuộc gọi local-only SQLite chung, không backend; Phase 1 chỉ Android.
- Nguồn chốt: ADR-001..004, `framework-tradeoff.md`.
- **FR-8**: `TelecomManager.endCall()` + `ANSWER_PHONE_CALLS`, không cần default dialer; chỉ một điểm gọi duy nhất. Nguồn: ADR-005, `call-termination-feasibility.md`.
- **FR-9/FR-10 (v1.2)**: không cần ADR mới — nằm trong ranh giới ADR-001 (export/import thuần Dart, không chạm native module) và ADR-003 (local-only; export/import thủ công, một lần, không mâu thuẫn tinh thần "local-only"). ADR-003 đã có mục bổ sung xác nhận điều này, kèm rủi ro privacy mới khi export.

## Bối cảnh hệ thống hiện tại
Repo chỉ có `docs/`. Không có `lib/`/`android/`. Toàn bộ thành phần Dart/Kotlin là MỚI (chưa code). Không có hạ tầng deployment.

**v1.2**: SRS lên v3.0 (FR-1/BLOCK-02 cập nhật exact+prefix, FR-6/CALLHISTORY-01 cập nhật phân biệt nguồn chặn, thêm FR-9/FR-10); ADR-003 bổ sung mục export. LLD/ERD hiện có đã mô tả `PhoneNumberNormalizer` và bảng `blocklist` với UNIQUE trên `normalized_number`, nhưng CHƯA có cột `entry_type` và CHƯA có logic Export/Import — cần cập nhật riêng sau HLD này.

## Sơ đồ thành phần

```mermaid
graph TD
  subgraph FlutterLayer["Flutter/Dart Layer - lib/"]
    UI_Onboard["OnboardingScreen + PermissionScreen"]
    UI_Block["BlocklistScreen - MO RONG v1.2 (loai entry, Export/Import)"]
    UI_History["CallHistoryScreen"]
    UI_Prompt["ScamConfirmationDialog (in-app)"]
    UI_Settings["SettingsScreen - MOI v1.3 (CONTACTS-PRIORITY + 3 BLOCK-TOGGLE)"]
    Svc_Block["BlocklistService (sqflite) - MO RONG v1.2 (entry_type)"]
    Svc_History["CallHistoryService (sqflite)"]
    Svc_Export["BlocklistExportService - MOI v1.2 FR-10"]
    Svc_Import["BlocklistImportService - MOI v1.2 FR-10"]
    Svc_Settings["SettingsService (sqflite) - MOI v1.3 (doc/ghi 4 co trong blocklist_meta)"]
    Bridge_Perm["PermissionsBridge"]
    Bridge_Event["CallEventBridge"]
  end

  subgraph BridgeLayer["Bridge Layer"]
    MC["MethodChannel"]
    EC["EventChannel"]
  end

  subgraph NativeLayer["Native Kotlin Layer - android/.../callscreening/"]
    App["SafeCallApplication - MOI v1.3 (onCreate: enqueue PeriodicWorkRequest)"]
    CSS["CallScreeningService - MO RONG v1.3 (pipeline 6 buoc: anonymous, exact, prefix+CONTACTS-PRIORITY, BLOCK-TOGGLE, SCAM-DETECT)"]
    CMS["CallMonitorService (foreground service)"]
    RSO["RingingScamOverlay (native)"]
    SCR["ScamConfirmationReceiver"]
    BLR["BlocklistRepository (DAO native) - MO RONG v1.3 (doc 4 co CONTACTS-PRIORITY/BLOCK-TOGGLE tu blocklist_meta)"]
    CHR["CallHistoryRepository (DAO native)"]
    CR["ContactsRepository - DA CO (bo sung vao so do v1.3, chua ve o HLD truoc do)"]
    CLM["ContactsLookupModule - DA CO (channel com.safecall/contacts)"]
    RM["RoleModule"]
    PM["PermissionsModule"]
    PCW["PermissionCheckWorker - MOI v1.3 FR-13 (WorkManager PeriodicWorkRequest, 24h, doc lap Flutter engine)"]
  end

  subgraph OS["Android OS / he thong"]
    TEL["Telephony subsystem (handlePresentation: RESTRICTED/UNKNOWN/PAYPHONE)"]
    NOTIF["Notification system"]
    ROLE["ROLE_CALL_SCREENING"]
    TELMGR["TelecomManager.endCall()"]
    SHARE["Android Share Sheet"]
    FILEPICKER["File picker"]
    CONTACTSOS["ContactsContract provider"]
  end

  DB[("Shared SQLite file - gom blocklist_meta")]
  TMPFILE[("Temp JSON file - app cache, ephemeral")]

  Svc_Block -->|sqflite| DB
  Svc_History -->|sqflite| DB
  Svc_Settings -->|sqflite, ghi 4 co - MOI v1.3| DB
  BLR -->|native| DB
  CHR -->|native| DB

  UI_Onboard --> Bridge_Perm
  Bridge_Perm --> MC
  MC --> RM
  MC --> PM
  RM -->|xin role| ROLE
  PM -->|xin quyen| OS

  UI_Settings --> Svc_Settings
  UI_Settings -.->|dieu huong tu Blocklist/menu - vi tri cu the: ui-ux-designer| UI_Block

  ROLE -. dieu khien .-> CSS
  TEL -->|callback, gom gia tri handlePresentation| CSS
  CSS -->|buoc 2-4: doc exact/prefix, doc 4 co| BLR
  CSS -->|buoc 4-5: isContact, 1 lan, tai su dung ket qua - MOI v1.3| CR
  CR -->|query| CONTACTSOS
  CLM --> CR
  Bridge_Perm -.-> MC
  MC --> CLM

  CMS --> CHR
  CMS --> NOTIF
  CMS --> CR
  NOTIF --> SCR
  SCR --> BLR

  CMS --> RSO
  RSO --> CMS
  CMS --> TELMGR
  CMS --> BLR
  CMS --> CHR

  CMS -. best-effort .-> EC
  EC --> Bridge_Event
  Bridge_Event --> UI_Prompt
  UI_Prompt -.-> MC
  MC -.-> BLR

  UI_Block --> Svc_Block
  UI_History --> Svc_History

  UI_Block --> Svc_Export
  UI_Block --> Svc_Import
  Svc_Export -->|doc toan bo blocklist exact+prefix| Svc_Block
  Svc_Export -->|ghi file JSON tam kem schema_version| TMPFILE
  TMPFILE --> SHARE
  Svc_Import -->|goi plugin, cho nguoi dung chon file| FILEPICKER
  FILEPICKER -.-> Svc_Import
  Svc_Import -->|validate + merge, bo qua trung| Svc_Block

  App -. enqueue luc khoi dong app - MOI v1.3 .-> PCW
  PCW -.->|can logic kiem tra qua Context, KHONG qua Activity - xem Ghi chu hieu chinh| PM
  PCW -.->|tuong tu| RM
  PCW --> |phat hien thieu quyen| NOTIF
  NOTIF -.->|PendingIntent, tap -> mo MainActivity voi extra chi dinh buoc quyen - tai su dung co che resume-denied-step da co| UI_Onboard
```

**Ghi chú hiệu chỉnh v1.3 (quan trọng)**:
1. Không có component mới `ContactsLookupRepository`. `ContactsRepository.kt` (lớp thuần, nhận `Context`, có `isNumberInContacts`/`countContactsMatchingPrefix`) đã được `ContactsLookupModule` (expose qua channel `com.safecall/contacts`, dùng cho FR-5/AC8, FR-9/AC5) VÀ `CallMonitorService` (gọi trực tiếp in-process, FR-2/AC3) dùng chung từ trước — chỉ cần `CallScreeningService` khởi tạo thêm một instance và gọi trực tiếp (FR-11).
2. `PermissionsModule`/`RoleModule` hiện nhận `Activity` trong constructor (cần cho `ActivityCompat.requestPermissions`/`RoleManager` lúc XIN quyền) — `PermissionCheckWorker` chạy nền, không có `Activity`, nên KHÔNG thể tái sử dụng trực tiếp các hàm `checkXxxStatus` hiện có. Cần `low-level-designer` tách phần "chỉ kiểm tra trạng thái" (chỉ cần `Context`) ra khỏi 2 class này.

Đường đảm bảo cho FR-2/3/4: `CMS → NOTIF → SCR → BLR` (native-to-native). Cho FR-8: `TEL → CMS → RSO → CMS → {BLR, TELMGR, CHR}`.

**v1.2 (FR-10)**: `BlocklistExportService`/`BlocklistImportService` → `{SHARE, FILEPICKER}` tương tác plugin hệ thống chuẩn (kiểu `share_plus`/file-picker), KHÔNG qua MethodChannel/EventChannel, KHÔNG chạm native layer — export/import không liên quan call-screening OS integration. Toàn bộ nằm trong Flutter layer.

## Trách nhiệm từng thành phần

| Thành phần | Layer | Làm | Không làm / biên giới |
|---|---|---|---|
| OnboardingScreen + PermissionScreen | Dart `lib/` | Giải thích quyền trước khi xin, bỏ qua nếu đủ quyền, hệ quả từ chối, phát hiện thu hồi. Mở rộng FR-8: giải thích `ANSWER_PHONE_CALLS` | Không tự gọi permission API — qua `PermissionsBridge` |
| BlocklistScreen | Dart `lib/` | Hiển thị/thêm/xóa số, cảnh báo trùng contact. **v1.2 (FR-9)**: chọn loại entry khi thêm (exact/prefix), nhãn phân biệt, cảnh báo khi đầu số khớp contact. **v1.2 (FR-10)**: nút Export (disabled khi rỗng)/Import | Không đọc/viết SQLite trực tiếp — qua `BlocklistService`. Không serialize JSON/gọi Share Sheet — đó là 2 service mới |
| CallHistoryScreen | Dart `lib/` | Hiển thị lịch sử, chỉ đọc | Không ghi |
| ScamConfirmationDialog | Dart `lib/` | Hiển thị dialog khi nhận event (foreground) | Không phải cơ chế đảm bảo; không liên quan FR-8 |
| BlocklistService | Dart `lib/` | Đọc/viết blocklist qua `sqflite`. **v1.2 (FR-9)**: đọc/viết CẢ HAI loại entry qua cột `entry_type`, validate riêng theo loại. **v1.2 (FR-10)**: tầng truy cập dữ liệu DUY NHẤT mà Export/Import service tái sử dụng | Không tham gia chặn real-time; không tự serialize JSON/gọi Share Sheet |
| **BlocklistExportService** (MỚI, FR-10) | Dart `lib/` | Đọc toàn bộ blocklist (exact+prefix) qua `BlocklistService`; serialize JSON kèm `schema_version`; ghi file tạm (cache dir); gọi Share Sheet | Không validate/merge (đó là Import); không ghi đè blocklist — read-only; không qua MethodChannel/native |
| **BlocklistImportService** (MỚI, FR-10) | Dart `lib/` | Gọi file picker; đọc+validate JSON+`schema_version` (từ chối version mới hơn, tương thích xuôi version cũ); merge entry hợp lệ chưa tồn tại qua `BlocklistService`; báo kết quả (N thêm/M bỏ qua hoặc lỗi) | Không tự parse giao thức share của OS; không xóa/ghi đè (merge-only); không tự đảm bảo transaction cụ thể (thuộc LLD); không qua MethodChannel/native |
| CallHistoryService | Dart `lib/` | Đọc lịch sử, chỉ đọc | Không ghi |
| PermissionsBridge | Dart `lib/` | Client MethodChannel cho quyền/role, gồm `ANSWER_PHONE_CALLS` | Không tự quyết định logic quyền |
| CallEventBridge | Dart `lib/` | Lắng nghe EventChannel, forward dialog | Không phải nguồn sự thật; không liên quan FR-8/FR-10 |
| CallScreeningService | Kotlin native | OS gọi khi có cuộc gọi đến. **v1.3**: pipeline quyết định chặn theo đúng **6 bước**: (1) nhánh sớm số ẩn danh (`handlePresentation` RESTRICTED/UNKNOWN/PAYPHONE) — cờ "chặn số ẩn danh" BẬT thì chặn, ngược lại allow, **return ngay, không chạy các bước sau**; (2) có số → normalize → kiểm tra BLOCK-02 (exact/prefix, FR-1); (3) khớp EXACT → chặn ngay, **không xét CONTACTS-PRIORITY**; (4) khớp PREFIX: CONTACTS-PRIORITY BẬT và số là contact → allow (lý do `contacts_priority`); TẮT hoặc không phải contact → chặn; (5) không khớp BLOCK-02: tính `isContact` MỘT LẦN (dùng chung bước 4 và 5) → kiểm tra cờ "chặn số lạ" (not contact) và "chặn số quốc tế" (not +84, not contact) — khớp 1 trong 2 → chặn, ngược lại allow; (6) không bị chặn ở trên → tiếp tục bình thường tới SCAM-DETECT-01/FR-8. Đọc 4 cờ mới VÀ exact/prefix cùng qua `BlocklistRepository`. Gọi `ContactsRepository.isNumberInContacts` trực tiếp in-process (không qua `ContactsLookupModule`/MethodChannel — hot-path ~1500ms không đảm bảo Flutter engine sống). **Ràng buộc hiệu năng (NFR-4, v1.2)**: hot path timeout ngắn của OS — nếu blocklist lớn, KHÔNG so khớp tuyến tính; cấu trúc tra cứu phù hợp — **thuật toán cụ thể thuộc `low-level-designer`** | Không qua MethodChannel/EventChannel, không phụ thuộc Flutter engine. Không tự ghi lý do chặn chi tiết vào `call_history` theo schema cuối cùng (FR-6/AC4, AC5) — ngoài phạm vi lần cập nhật này. Không tự quyết cách tối ưu đọc 4 cờ (cache hay point-read) — `low-level-designer` |
| CallMonitorService (foreground service) | Kotlin native | Như v1.1 (FR-2/3/4/8 — xem chi tiết ở LLD) | Như v1.1 |
| ScamConfirmationReceiver | Kotlin native | Xử lý tap Notification | Không mở rộng FR-8/FR-10 |
| RingingScamOverlay | Kotlin native | Như v1.1, cơ chế hiển thị ĐÃ CHỐT (ADR-007, Window overlay) | Như v1.1 |
| BlocklistRepository (DAO native) | Kotlin native | Đọc cho `CallScreeningService`, viết cho `ScamConfirmationReceiver`/`CallMonitorService`. **v1.2 (FR-9)**: bảng `blocklist` chứa CẢ EXACT và PREFIX (`entry_type`); `isBlocked()` kiểm tra cả hai loại. **v1.3 (FR-11/FR-12)**: thêm method đọc 4 cờ mới (`contacts_priority_enabled`, `block_unknown_enabled`, `block_anonymous_enabled`, `block_international_enabled`) từ `blocklist_meta` (ADR-008) — cùng cơ chế point-read đã dùng cho `prefix_version` | Không expose qua MethodChannel. **v1.2**: KHÔNG tự xử lý import/merge. **v1.3**: KHÔNG ghi 4 cờ mới — ghi là trách nhiệm của `SettingsService` (Dart), qua cùng bảng `blocklist_meta` (Dart ghi, native đọc, giống mô hình `prefix_version`) |
| **ContactsRepository** (ĐÃ CÓ — bổ sung vào sơ đồ HLD v1.3) | Kotlin native | Tra cứu contact thuần (`isNumberInContacts`, `countContactsMatchingPrefix`), không phụ thuộc MethodChannel. Dùng chung bởi `ContactsLookupModule` (FR-5/AC8, FR-9/AC5) và `CallMonitorService` (FR-2/AC3). **v1.3**: thêm người gọi thứ ba — `CallScreeningService` (in-process, FR-11) | Không tự quyết định logic chặn/ưu tiên — chỉ trả Boolean/Int. Không cache giữa các lần gọi |
| CallHistoryRepository (DAO native) | Kotlin native | Ghi lịch sử, outcome ngắt gọi (FR-8). **v1.4**: ghi `wasBlocked`/`decisionReason` tường minh (LLD v1.4 §"Sửa lỗi phát hiện..."; `decision_reason` + CHECK trong ERD v1.3) — đã phản ánh ĐẦY ĐỦ ở component này, backend/schema ĐÃ ĐÓNG | Không đọc cho UI. **Ghi chú (đã đóng ở v1.4)**: phần backend/schema cho lý do chặn mới từ FR-11/FR-12 (contacts_priority, block_unknown/anonymous/international) đã thiết kế xong ở LLD v1.4/ERD v1.3 — KHÔNG còn gap ở `CallHistoryRepository`. Gap còn lại DUY NHẤT là tầng hiển thị UI (`CallHistoryScreen.deriveDisplayStatus`, Dart) — xem Ngoài phạm vi |
| RoleModule | Kotlin native | Handler MethodChannel cho role | — |
| PermissionsModule | Kotlin native | Handler quyền runtime, gồm `ANSWER_PHONE_CALLS` | Không liên quan FR-10 — Share Sheet/file picker dùng quyền/intent riêng của plugin, không qua `PermissionsModule` |
| **SettingsScreen** (MỚI, FR-11/FR-12, v1.3) | Dart `lib/` | Hiển thị 1 toggle CONTACTS-PRIORITY (default BẬT) + 3 switch BLOCK-TOGGLE (default TẮT: "Chặn số lạ", "Chặn số ẩn danh", "Chặn số quốc tế") trên CÙNG một màn hình. Đổi trạng thái → gọi `SettingsService` ghi ngay, áp dụng từ cuộc gọi tiếp theo, không cần khởi động lại app | Không tự đọc/viết `blocklist_meta` trực tiếp — qua `SettingsService`. Không tự quyết vị trí trong cấu trúc điều hướng app (SRS 7.1, câu hỏi mở — `ui-ux-designer`). Không hiển thị cảnh báo khi đổi cờ (SRS 7.1, chưa chốt) |
| **SettingsService** (MỚI, FR-11/FR-12, v1.3) | Dart `lib/` | Đọc/viết 4 cờ qua `sqflite` vào bảng `blocklist_meta` (dùng chung file SQLite với `BlocklistService`/`CallHistoryService`) | Không tham gia hot-path chặn real-time. Không tự định nghĩa schema/migration — ADR-008 + `data-modeler` |
| **SafeCallApplication** (MỚI, FR-13, v1.3) | Kotlin native | `onCreate()`: enqueue một `androidx.work.PeriodicWorkRequest` (24h) cho `PermissionCheckWorker`, độc lập hoàn toàn Flutter engine | Không tự chứa logic kiểm tra quyền — chỉ enqueue. Cách tích hợp với `android:name="${applicationName}"` hiện tại trong Manifest — **chưa xác định**, cần `system-architect`/`low-level-designer` |
| **PermissionCheckWorker** (MỚI, FR-13, v1.3) | Kotlin native | Chạy định kỳ 24h, kiểm tra trạng thái 6 quyền (vai trò chặn gọi, trạng thái cuộc gọi, danh bạ, thông báo + `POST_NOTIFICATIONS`, `ANSWER_PHONE_CALLS`, Overlay `SYSTEM_ALERT_WINDOW`). Quyền thiếu → notification với ID cố định theo loại quyền (tự thay thế/idempotent), kèm `PendingIntent` điều hướng tới đúng luồng cấp quyền | Không tự xin cấp quyền. Không phụ thuộc `Activity`/Flutter engine để chạy — nhưng logic kiểm tra cần `Context` (xem Ghi chú hiệu chỉnh #2 ở sơ đồ). Không quyết định ngưỡng chu kỳ nhỏ hơn 15 phút (không áp dụng — chu kỳ 24h đã lớn hơn ngưỡng tối thiểu của `WorkManager`) |

**Ghi chú NFR-4 (v1.2)**: hiệu năng tra cứu matching exact+prefix khi blocklist lớn (có thể tăng nhanh sau import) là ràng buộc định tính, không có số liệu cụ thể — thuộc `system-architect`/`low-level-designer`.

## Giao tiếp & luồng dữ liệu (các dòng mới v1.2)

| Từ | Đến | Đồng bộ | Kênh | Ý nghĩa |
|---|---|---|---|---|
| BlocklistScreen | BlocklistExportService | Đồng bộ | In-process (Dart) | Yêu cầu export; disable khi rỗng |
| BlocklistExportService | BlocklistService | Đồng bộ | In-process (Dart) | Đọc toàn bộ blocklist để serialize |
| BlocklistExportService | Android Share Sheet (OS) | Đồng bộ (gọi); kết quả theo Activity result | Plugin hệ thống | Mở hộp thoại chia sẻ — KHÔNG qua MethodChannel/EventChannel |
| BlocklistScreen | BlocklistImportService | Đồng bộ | In-process (Dart) | Yêu cầu import |
| BlocklistImportService | File picker (OS) | Đồng bộ (gọi) | Plugin hệ thống | Chọn file JSON — KHÔNG qua MethodChannel/EventChannel |
| BlocklistImportService | BlocklistService | Đồng bộ | In-process (Dart) | Merge entry hợp lệ, bỏ qua trùng/sai định dạng |
| CallScreeningService | ContactsRepository | Đồng bộ | In-process (Kotlin) | **v1.3**: kiểm tra số gọi đến có phải contact — tính 1 lần, dùng chung cho nhánh CONTACTS-PRIORITY (bước 4) và BLOCK-TOGGLE (bước 5) |
| SettingsScreen | SettingsService | Đồng bộ | In-process (Dart) | **v1.3**: đọc trạng thái hiện tại 4 cờ khi mở màn hình; ghi khi người dùng đổi |
| SettingsService | SQLite (`blocklist_meta`) | Đồng bộ | `sqflite` | **v1.3**: ghi 4 cờ — có hiệu lực ngay cho lần `CallScreeningService` đọc tiếp theo (point-read, không cần tín hiệu đẩy, giống mô hình `prefix_version`) |
| SafeCallApplication | PermissionCheckWorker | Bất đồng bộ (enqueue một lần, chạy định kỳ sau đó) | `androidx.work.WorkManager` | **v1.3**: đăng ký tác vụ nền 24h, tồn tại độc lập vòng đời `Application` |
| PermissionCheckWorker | OS Notification system | Đồng bộ (gọi), hiển thị bất đồng bộ | Android Notification API | **v1.3**: phát thông báo nhắc cấp lại quyền, ID cố định theo loại quyền (idempotent) |
| Notification (tap) | MainActivity/OnboardingFlowScreen | Bất đồng bộ (do người dùng khởi tạo) | `PendingIntent` | **v1.3**: điều hướng thẳng tới luồng cấp quyền tương ứng — tái dùng cơ chế resume-denied-step đã có |

(Các dòng FR-1..FR-8 giữ nguyên như v1.1.)

### Luồng chính 1: Chặn real-time (FR-1/FR-9) *(cập nhật v1.2)*
1. OS gọi `CallScreeningService` (đã có role từ onboarding).
2. Chuẩn hóa số qua `PhoneNumberNormalizer`, đọc `BlocklistRepository`, kiểm tra CẢ exact match VÀ prefix match (FR-9).
3. Khớp MỘT TRONG HAI loại → chặn — blocklist ưu tiên hơn contact luôn.
4. **Ràng buộc hiệu năng**: không so khớp tuyến tính khi blocklist lớn — cấu trúc tra cứu cụ thể thuộc `low-level-designer`.

### Luồng chính 3: Quản lý blocklist thủ công (FR-5/FR-9) *(cập nhật v1.2)*
1. `BlocklistScreen` hiển thị CẢ HAI loại entry, nhãn phân biệt rõ.
2. Thêm số cụ thể (FR-5) hoặc đầu số (FR-9) dùng CÙNG cơ chế ghi của `BlocklistService` — chỉ khác `entry_type` và validate theo loại (ngưỡng 4 ký tự cho đầu số). Cả hai qua `PhoneNumberNormalizer`, cùng cảnh báo khớp contact trước khi xác nhận — có hiệu lực ngay cho Luồng 1.
3. Xóa dùng cùng cơ chế, chỉ khác đối tượng theo `entry_type`.

### Luồng chính 6: Export/Import blocklist (FR-10) *(mới — v1.2)*

**Export**: chọn "Export" (chỉ khi không rỗng) → `BlocklistExportService` đọc toàn bộ entry qua `BlocklistService` → serialize JSON kèm `schema_version` → ghi file tạm (cache dir, tách biệt SQLite chung) → gọi Share Sheet. Hủy giữa Share Sheet → blocklist không đổi (toàn bộ luồng tới đó là read-only).

**Import**: chọn "Import" → `BlocklistImportService` gọi file picker → đọc + validate cấu trúc + `schema_version` (không hợp lệ → báo lỗi, không đổi blocklist; version mới hơn → từ chối an toàn; version cũ hơn được hỗ trợ → tương thích xuôi) → merge từng entry qua `BlocklistService` (hợp lệ+chưa có → thêm; đã có → bỏ qua; sai định dạng → bỏ qua, không chặn phần hợp lệ còn lại) → báo kết quả tổng hợp. Tính nhất quán khi gián đoạn giữa chừng (crash) — cơ chế cụ thể thuộc `low-level-designer`.

**Ranh giới**: toàn bộ Luồng 6 trong Dart layer, KHÔNG qua MethodChannel/EventChannel, KHÔNG chạm `android/.../callscreening/` — export/import không liên quan call-screening OS integration.

### Luồng chính 7: Chặn real-time — pipeline đầy đủ 6 bước (FR-1/FR-9/FR-11/FR-12) *(mới — v1.3, thay thế mô tả rút gọn ở Luồng chính 1 cho phần liên quan)*
1. OS gọi `CallScreeningService.onScreenCall`. Nếu `handlePresentation` là RESTRICTED/UNKNOWN/PAYPHONE (số ẩn danh): đọc cờ "chặn số ẩn danh" qua `BlocklistRepository` — BẬT thì chặn, TẮT thì allow; **return ngay, dừng pipeline**.
2. Có số → normalize qua `PhoneNumberNormalizer` → kiểm tra EXACT rồi PREFIX qua `BlocklistRepository`/`BlocklistPrefixIndex`.
3. Khớp EXACT → chặn ngay, không xét CONTACTS-PRIORITY (FR-1/AC9).
4. Khớp PREFIX (không khớp EXACT) → gọi `ContactsRepository.isNumberInContacts` → đọc cờ CONTACTS-PRIORITY: BẬT + là contact → allow (lý do `contacts_priority`); còn lại → chặn.
5. Không khớp BLOCK-02 → dùng LẠI kết quả `isContact` đã tính (không gọi `ContactsRepository` lần 2) → đọc cờ "chặn số lạ"/"chặn số quốc tế" qua `BlocklistRepository` → khớp 1 trong 2 (và không phải contact) → chặn; ngược lại → allow.
6. Không bị chặn ở bước 1-5 → tiếp tục luồng bình thường tới SCAM-DETECT-01/FR-8 (`CallMonitorService`/overlay) — không đổi so với v1.2.

### Luồng chính 8: Cấu hình Settings (FR-11/FR-12) *(mới — v1.3)*
1. `SettingsScreen` tải trạng thái 4 cờ qua `SettingsService` khi mở.
2. Người dùng đổi 1 toggle/switch → `SettingsService` ghi ngay vào `blocklist_meta`.
3. Cuộc gọi tiếp theo, `CallScreeningService` đọc giá trị mới qua `BlocklistRepository` — không cần khởi động lại app, không cần tín hiệu đẩy (point-read mỗi lần `onScreenCall`).

### Luồng chính 9: Kiểm tra quyền định kỳ (FR-13) *(mới — v1.3)*
1. `SafeCallApplication.onCreate()` enqueue `PermissionCheckWorker` (24h, tồn tại cả khi app bị kill/không mở).
2. Mỗi chu kỳ: Worker kiểm tra 6 quyền qua `Context` (không qua `Activity`).
3. Quyền thiếu → notification với ID cố định theo loại quyền (gọi lại `post()` cùng ID tự thay thế, không chồng) → nhấn → `PendingIntent` mở `MainActivity`/`OnboardingFlowScreen` tại đúng bước cấp quyền đó.
4. Quyền đã cấp lại → chu kỳ tiếp theo không còn phát thông báo loại đó (do kiểm tra lại trạng thái GRANTED trước khi gọi `post()`).

## Deployment view (bổ sung v1.2, v1.3)
- **BlocklistExportService/BlocklistImportService**: chạy trong Flutter engine, chỉ hoạt động khi app foreground (hành động thủ công một lần). Không cần service riêng.
- **Temp JSON file**: cache directory, ephemeral (OS có thể xóa khi cần dung lượng), khác file SQLite chung; không rời thiết bị cho tới khi người dùng chọn kênh chia sẻ.
- Bảng `blocklist` sẽ cần cột `entry_type` mới — schema cụ thể thuộc `data-modeler`.
- **SettingsScreen/SettingsService (v1.3)**: chạy trong Flutter engine, chỉ hoạt động khi app foreground.
- **PermissionCheckWorker (v1.3)**: chạy qua `androidx.work.WorkManager` — tiến trình/scheduling do hệ thống Android quản lý (App Standby/Doze ảnh hưởng độ chính xác lịch trình, xem NFR-4 SRS v6.0). Có thể chạy cùng hoặc khác process với `CallScreeningService`/`CallMonitorService` — **chưa xác định**, cần `system-architect` xác nhận cấu hình `android:process` thật (tương tự ghi chú đã có ở LLD cho `CallScreeningService`).
- **SafeCallApplication (v1.3)**: process chính của app — cách tích hợp với `android:name="${applicationName}"` hiện tại trong Manifest **chưa xác định**.
- **WorkManager dependency** (`androidx.work:work-runtime`, v1.3): cần thêm vào `build.gradle` — cấu hình Gradle cụ thể, ngoài phạm vi HLD.
- `blocklist_meta` (v1.3) mở rộng thêm 4 cột cờ — schema cụ thể theo ADR-008 + `data-modeler`.
- (Các mục FR-8/ADR-005/006/007 giữ nguyên như v1.1.)

## Phase breakdown (bổ sung)

### Phase 1.7 — Chặn theo đầu số + Export/Import blocklist (FR-9/FR-10) *(mới — v1.2)*
- Thành phần: mở rộng `BlocklistScreen`, `BlocklistService`, `CallScreeningService`+`BlocklistRepository` (matching exact+prefix, tra cứu hiệu quả); `BlocklistExportService`, `BlocklistImportService` (MỚI).
- Phụ thuộc: **Phase 1.2 phải hoàn thành trước** (UI/data layer blocklist cơ bản). Cũng phụ thuộc **Phase 1.3** (chặn real-time exact đã có — prefix là mở rộng trực tiếp). KHÔNG phụ thuộc Phase 1.4/1.5/1.6 — độc lập về runtime, chỉ dùng chung bảng `blocklist`.
- Ràng buộc còn mở: cột `entry_type`+UNIQUE theo loại (thuộc `data-modeler`); cấu trúc tra cứu hiệu quả (thuộc `low-level-designer`); cơ chế transaction cho import gián đoạn (thuộc `low-level-designer`); JSON schema chi tiết (thuộc `low-level-designer`/`data-modeler`).

### Phase 1.8 — Cơ chế ưu tiên Danh bạ + hạ tầng Settings (FR-11) *(mới — v1.3)*
- Thành phần: `SettingsScreen` (MỚI), `SettingsService` (MỚI), mở rộng `BlocklistRepository` (đọc cờ CONTACTS-PRIORITY), mở rộng `CallScreeningService` (bước 3-4 của pipeline, gọi `ContactsRepository`).
- Phụ thuộc: **Phase 1.7 phải hoàn thành trước** (prefix matching FR-9 đã có — CONTACTS-PRIORITY là ngoại lệ trực tiếp của nó). `ContactsRepository`/`ContactsLookupModule` đã có sẵn từ cascade FR-5/FR-9, chỉ cần thêm người gọi mới.
- Ràng buộc còn mở: schema cột `blocklist_meta` (ADR-008 + `data-modeler`); lý do ghi vào `call_history` (`low-level-designer`/`data-modeler`).

### Phase 1.9 — Bật/Tắt chế độ chặn nâng cao (FR-12) *(mới — v1.3, phụ thuộc Phase 1.8)*
- Thành phần: mở rộng `SettingsScreen`/`SettingsService` (3 switch thêm vào CÙNG màn hình đã dựng ở Phase 1.8), mở rộng `BlocklistRepository` (đọc 3 cờ BLOCK-TOGGLE), mở rộng `CallScreeningService` (bước 1 nhánh anonymous sớm + bước 5 tái dùng `isContact`).
- Phụ thuộc: **Phase 1.8 phải hoàn thành trước** — tái dùng CÙNG màn hình Settings và CÙNG biến `isContact` đã tính ở bước 4. Không phụ thuộc Phase 1.10.
- Ràng buộc còn mở: độ tin cậy phát hiện số ẩn danh/mã quốc gia qua Telecom API (SRS 7.1, `system-architect`/`low-level-designer` xác nhận khả thi trước khi code AC3/AC4).

### Phase 1.10 — Kiểm tra trạng thái Quyền định kỳ (FR-13) *(mới — v1.3, độc lập, có thể chạy song song với Phase 1.8/1.9)*
- Thành phần: `SafeCallApplication` (MỚI), `PermissionCheckWorker` (MỚI).
- Phụ thuộc: **KHÔNG phụ thuộc Phase 1.8/1.9** — hoàn toàn độc lập về runtime. Phụ thuộc gián tiếp vào `PermissionsModule`/`RoleModule`/`PermissionKeys` đã có (Phase 0) cho DANH SÁCH quyền cần kiểm tra, nhưng cần làm rõ ranh giới Context-vs-Activity trước khi code.
- Ràng buộc còn mở (chuyển `low-level-designer`/`system-architect`): cách tách logic "chỉ kiểm tra" ra khỏi `PermissionsModule`/`RoleModule` (Activity-bound); cấu hình `android:name` Application thật; độ tin cậy lịch trình 24h dưới Doze/App Standby (NFR-4).

**Lưu ý đánh số phase (v1.3)**: `docs/planning/WBS-SafeCall-Phase1.md`/`estimate.md` hiện ĐÃ có một "Phase 1.8" khác (mục bổ sung retroactive cho việc đã làm thật 2026-10-25, không liên quan FR-11/12/13). Việc đặt Phase 1.8/1.9/1.10 ở đây là theo đúng trình tự nối tiếp HLD/LLD (đã ở v1.3, Phase 1.7 là FR-9/FR-10) — khi cascade xuống WBS/estimate cần đổi số phase cũ hoặc chèn đúng vị trí, tương tự cách Phase 1.7 đã từng được xử lý.

## Ngoài phạm vi tài liệu này
- Chế độ WAL/locking — `data-modeler`/`low-level-designer`.
- NFR-4 cụ thể (background detection, overlay, VÀ hiệu năng tra cứu exact+prefix — v1.2); NFR-5/6/7 — "chưa xác định".
- Schema cụ thể: outcome FR-8, VÀ cột `entry_type`/UNIQUE theo loại cho FR-9 (v1.2) — `data-modeler`. ERD hiện có CHƯA cập nhật cho thay đổi này.
- Thuật toán tra cứu matching hiệu quả (v1.2) — `low-level-designer`.
- Cơ chế transaction cho import gián đoạn (FR-10/AC10, v1.2) — `low-level-designer`.
- JSON schema chi tiết export/import, lựa chọn plugin Share Sheet/file picker cụ thể — `low-level-designer`/`data-modeler`.
- **FR-6/CALLHISTORY-01 (v1.2, ĐÃ ĐÓNG phần backend/schema ở v1.4)**: SRS đã cập nhật để phân biệt nguồn chặn exact/prefix VÀ các lý do mới từ FR-11/FR-12 (contacts_priority, block_unknown/anonymous/international) trong lịch sử. Phần `CallHistoryRepository`/schema ĐÃ được thiết kế đầy đủ và đóng ở LLD v1.4 (`CallRecordDraft.wasBlocked`/`decisionReason`, enum `ScreeningDecisionReason`) và ERD v1.3 (cột `decision_reason` + 5 CHECK constraint liên quan). Phần CÒN MỞ, hẹp lại CHỈ CÒN tầng hiển thị UI: `CallHistoryScreen`/`CallHistoryService.deriveDisplayStatus` (Dart) chưa được cập nhật để hiển thị các `decisionReason` mới — tracked ở LLD v1.4 (dòng ~770), WBS (dòng ~970), Test Plan v1.3 (dòng ~8). Ngoài phạm vi lần cập nhật này; cần một lượt riêng cho `ui-ux-designer`/`low-level-designer` chốt phần UI này.
- Cập nhật ERD/LLD/UIUX/Test Plan/estimate/WBS theo cascade FR-9/FR-10/FR-11/FR-12/FR-13 — tiếp tục ở giai đoạn sau.
- `endCall()` call-waiting, test thiết bị thật OEM — như v1.1 (ADR-005).
- **(v1.3)** Schema cột thật cho `blocklist_meta` (4 cờ mới) — ADR-008 + `data-modeler`.
- **(v1.3)** Tách logic kiểm tra quyền (Context-only) ra khỏi `PermissionsModule`/`RoleModule` (Activity-bound) để `PermissionCheckWorker` dùng được — `low-level-designer`.
- **(v1.3)** Cách tích hợp `SafeCallApplication` với `android:name="${applicationName}"` hiện tại trong Manifest — `system-architect`/`low-level-designer`.
- **(v1.3)** Độ tin cậy phát hiện số ẩn danh/mã quốc gia qua `Call.Details.getHandlePresentation()`/Telecom API (SRS 7.1) — `system-architect`/`low-level-designer`.
- **(v1.3)** Vị trí/cấu trúc điều hướng cụ thể của `SettingsScreen`, nội dung cảnh báo (nếu có) khi đổi cờ — `ui-ux-designer`.
- **(v1.3)** Đổi số phase trùng "Phase 1.8" giữa HLD và WBS/estimate hiện có — cần xử lý khi cascade WBS/estimate.
