# Module Map & Phân tầng (retroactive) — SAFECALL

> Theo Build Playbook bước [2]. **Ghi chú trung thực:** file này được viết SAU KHI Phase 1-8 đã build+test
> xong (không đúng thứ tự chuẩn — Module Map phải có TRƯỚC khi chia WBS/Architecture). Đây là lý do
> `KNOWLEDGE-HEALTH-SAFECALL.md` (c4 Orphan Rate) từng liệt "module chưa có doc" là 1 gap — file này đóng gap
> đó, nhưng KHÔNG xoá được sự thật là Module Map không tồn tại lúc code Phase 1 thực sự được viết. Dữ liệu
> dưới đây lấy từ cấu trúc code thật trên đĩa (`find lib/ android/.../kotlin/`, 2026-10-25), không suy diễn.

## Bảng Module — Layer 0 (móng ẩn) vs module bề mặt

| Layer | Module | Trách nhiệm | File chính (Dart / Kotlin) |
|---|---|---|---|
| **0 — Gateway** | Platform Bridge | Cầu MethodChannel/EventChannel giữa Dart UI và Kotlin native — mọi module bề mặt đều đi qua đây để chạm hệ điều hành | `android/.../MainActivity.kt`; `lib/permissions/permissions_bridge.dart`, `lib/contacts/contacts_bridge.dart`, `lib/call_events/call_event_bridge.dart`; `android/.../monitor/CallEventStreamHandler.kt` |
| **0 — Phân quyền** | Permissions & Role | Xin/kiểm 4 runtime permission + vai trò `ROLE_CALL_SCREENING` — **móng của mọi tính năng chặn gọi**, không module nào phía dưới chạy được nếu thiếu | `android/.../PermissionsModule.kt`, `RoleModule.kt`, `PermissionKeys.kt`; `lib/permissions/permission_keys.dart` |
| **0 — Data model** | Database / Schema | SQLite 2 phía (Dart sqflite + Kotlin SQLiteOpenHelper) — bảng `blocklist`, `blocklist_meta`, `call_history`, `pending_prompts`, `schema_migrations` — **nền của mọi dữ liệu**, mọi module bề mặt đọc/viết qua đây | `lib/db/safecall_schema.dart`, `database_provider.dart`; `android/.../db/SafeCallSchema.kt`, `SafeCallDatabaseHelper.kt`, `BlocklistRepository.kt`, `CallHistoryRepository.kt`, `BlocklistReadException.kt`, `BlocklistWriteException.kt` |
| **0 — Tiện ích dùng chung** | Phone Number Normalization | Chuẩn hoá số điện thoại VN — dùng bởi CẢ Blocklist CRUD, Call Screening, Export/Import; sai ở đây hỏng UNIQUE constraint của toàn hệ thống (xem Risk R9) | `lib/blocklist/phone_number_normalizer.dart`; `android/.../PhoneNumberNormalizer.kt` |
| **0 — Tiện ích dùng chung** | Call State Observation | Lắng nghe trạng thái cuộc gọi hệ điều hành (2 implementation API 29 vs 31+) — móng cho MỌI tính năng phát hiện cuộc gọi (chặn, hỏi xác nhận, overlay) | `android/.../monitor/CallState.kt`, `CallStateObserver.kt`, `CallStateObserverFactory.kt`, `PhoneStateListenerObserver.kt`, `TelephonyCallbackObserver.kt` |
| Bề mặt | Onboarding & Permission UI | 7 bước xin quyền + giải thích lý do (FR-7) | `lib/onboarding/onboarding_flow_screen.dart`, `onboarding_status.dart`, `permission_step_config.dart`, `contacts_banner.dart` |
| Bề mặt | Blocklist Management | CRUD thủ công exact + prefix (FR-5, FR-9) | `lib/blocklist/blocklist_screen.dart`, `blocklist_service.dart`, `blocklist_models.dart`; `android/.../EntryType.kt`, `BlocklistPrefixIndex.kt` |
| Bề mặt | Call Screening | Chặn real-time khi có cuộc gọi đến (FR-1) | `android/.../CallScreeningService.kt` |
| Bề mặt | Contacts Integration | Tra cứu + cảnh báo khi số trùng contact (FR-5/AC8, FR-9/AC5) | `lib/contacts/contacts_bridge.dart`; `android/.../ContactsRepository.kt`, `ContactsLookupModule.kt` |
| Bề mặt | Post-call Confirmation | Hỏi xác nhận sau khi cuộc gọi từ số lạ kết thúc (FR-2/FR-3/FR-4) | `lib/call_events/scam_confirmation_dialog.dart`; `android/.../monitor/CallMonitorService.kt`, `PromptEligibility.kt`, `PromptEligibilityEvaluator.kt`, `PendingPromptRegistry.kt`, `ScamConfirmationNotifier.kt`, `ScamConfirmationReceiver.kt` |
| Bề mặt | Ringing Overlay + End Call | Xác nhận lừa đảo lúc đổ chuông + ngắt cuộc gọi — hành động không thể hoàn tác (FR-8) | `android/.../monitor/RingingScamOverlay.kt`, `RingingEligibility.kt`, `RingingCallNumberHolder.kt`, `CallTerminationOutcome.kt` (mở rộng `CallMonitorService.kt`) |
| Bề mặt | Call History | Xem lại cuộc gọi gần đây + trạng thái xử lý (FR-6) | `lib/call_history/call_history_screen.dart`, `call_history_service.dart`, `call_history_models.dart` |
| Bề mặt | Export/Import Blocklist | Export JSON qua Share Sheet, Import merge (FR-10) | `lib/blocklist/blocklist_export_service.dart`, `blocklist_import_service.dart`, `blocklist_export_import_models.dart` |

## Scope MVP

Toàn bộ 5 module Layer 0 + 8 module bề mặt ở trên đều thuộc phạm vi MVP Phase 1 theo `SRS-SafeCall-Phase1.md`
— không có module nào bị đẩy ra ngoài phạm vi khi viết Module Map retroactive này (khác với tình huống lý
tưởng ở Build Playbook, nơi Module Map giúp CẮT scope trước khi code; ở đây code đã tồn tại nên vai trò thực
tế của bảng trên là **phân loại lại**, không phải cắt).

**Thứ tự làm thực tế** (theo `WBS-SafeCall-Phase1.md`, Phase 1→8), đúng tinh thần "móng trước – bề mặt sau":
Permissions & Role + Platform Bridge (Phase 1) → Blocklist Management (Phase 2) → Call Screening (Phase 3) →
Post-call Confirmation + Database mở rộng (Phase 4) → tích hợp cross-stack (Phase 5) → Ringing Overlay + End
Call (Phase 6) → Prefix matching + Export/Import + hiệu năng (Phase 7) → Security + UAT (Phase 8). Call
History và Contacts Integration xen vào Phase 5/2 tương ứng, không có Phase riêng.

## Lát cắt dọc mỏng — ứng viên & thực tế đã build

**Ứng viên lý tưởng nếu Module Map này có TRƯỚC khi code (phân tích lại, không phải lựa chọn mới):** Platform
Bridge → Permissions & Role → 1 màn hình bề mặt tối giản — đây CHÍNH LÀ lát cắt đã được chọn và build thật,
xem `DOR-SAFECALL.md`: **Onboarding/cấp quyền (Phase 1) → thêm số vào Blocklist (Phase 2) → lưu SQLite → hiển
thị lại đúng trên UI**. Lát cắt này đi đúng thứ tự Layer 0 (Permissions & Role, Database) → bề mặt (Blocklist
Management), xác nhận retroactive rằng thứ tự "móng trước" đã được tuân thủ ở chính lát cắt đầu tiên, dù
Module Map chưa tồn tại lúc đó để xác nhận trước.

**Lát cắt dọc mỏng còn thiếu, nên làm tiếp theo:** Call State Observation → Post-call Confirmation →
Blocklist Management (ghi) — đi xuyên từ móng (lắng nghe trạng thái cuộc gọi) tới bề mặt (prompt xác nhận +
ghi blocklist). Đây là FR-2/FR-3/FR-4, hiện đang là nhóm "mồ côi" nặng nhất trong `RTM-SAFECALL.md` (0 lần
verify tay) — ưu tiên hợp lý cho vòng test kế tiếp, không phải ngẫu nhiên mà vì nó đi xuyên đúng 1 module
Layer 0 (Call State Observation) chưa được bất kỳ lát cắt nào chạm tới.

## 🔒 Cổng hiểu — áp dụng cho chính Module Map retroactive này

**Module nào bị xếp sai tầng trong thực tế build (không phải giả định)?** Hai module Layer 0 — **Permissions
& Role** (Task 1.1.2) và **Database/Schema** (Task 1.3.1) — bị chính Delegation Map/Decision Log xử lý THỰC
TẾ như module bề mặt rủi ro thấp (chạy ở Leash A, không có cổng người duyệt riêng), dẫn trực tiếp tới 2 sự cố
thật: crash `SecurityException` (Decision Log #3) và lỗi PRAGMA làm hỏng toàn bộ tầng đọc DB (Decision Log
#4). Đây không phải lỗi của bảng Module Map này — bảng này được viết SAU — mà là **bằng chứng cho thấy
không có Module Map nào tồn tại trước khi build khiến ranh giới Layer 0 không được tôn trọng ở tầng quyết
định ủy quyền**. Bài học áp dụng cho các lát cắt còn lại: bất kỳ task chạm vào 1 trong 5 module Layer 0 ở
bảng trên phải tự động xét A+ (đã phản ánh trong `DELEGATION-MAP-SAFECALL.md`), không chờ sự cố xảy ra rồi
mới gán lại.
