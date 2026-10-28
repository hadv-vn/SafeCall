# Hướng dẫn cấu hình Release Signing — SAFECALL

> Runbook cho Task WBS 8.2.1/Phase 8 (cross-cutting, release-readiness). Đây là việc **Human-do bắt buộc**
> (xem `docs/governance/RISK-SAFECALL.md` mục R14 và `Operating-Model.md` luật cứng #2: AI không được
> tự tạo/biết/chạm keystore hay mật khẩu). AI chỉ chuẩn bị cơ chế gradle đọc cấu hình — không tự điền secret.

## Trạng thái hiện tại

`android/app/build.gradle` đã được sửa để tự đọc `android/key.properties` (chưa tồn tại — bạn tự tạo) và
dùng nó cho `signingConfigs.release`. Nếu file đó chưa có, `buildTypes.release` tự fallback về debug-signing
(build vẫn chạy được để dev, nhưng **AAB ký bằng debug key sẽ bị Play Store từ chối khi publish**).

## Các bước thực hiện (tự làm, không nhờ AI)

### Bước 1 — Tạo keystore

Chạy trong terminal của bạn, tự nhập mật khẩu khi được hỏi (không dán mật khẩu vào chat với AI):

```bash
keytool -genkey -v -keystore android/upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

- Giữ file `.jks` này an toàn, backup ở nơi ngoài máy hiện tại (ví dụ password manager + ổ cứng ngoài/cloud
  storage riêng tư).
- **Mất file hoặc quên mật khẩu = mất khả năng cập nhật app đã publish trên Play Store, không khôi phục được.**

### Bước 2 — Tạo `android/key.properties` từ mẫu

```bash
cp android/key.properties.example android/key.properties
```

Mở `android/key.properties` bằng editor của bạn, tự điền:

```
storeFile=upload-keystore.jks
storePassword=<mật khẩu keystore thật>
keyAlias=upload
keyPassword=<mật khẩu key thật>
```

File này đã được thêm vào `.gitignore` (cùng `*.jks`, `*.keystore`) — không commit, không chia sẻ.

### Bước 3 — Build thử để xác nhận ký đúng

```bash
flutter build appbundle --release
```

Nếu `key.properties` đúng format, Gradle tự dùng `signingConfigs.release` thật. Có thể xác minh keystore
nào đã ký bằng:

```bash
keytool -list -printcert -jarfile build/app/outputs/bundle/release/app-release.aab
```

(SHA-256 cert phải khớp với cert của `upload-keystore.jks`, không phải debug cert mặc định của Android SDK.)

## Việc KHÔNG nằm trong phạm vi runbook này

- Play Store Console listing/release process (ngoài phạm vi WBS, xem `WBS-SafeCall-Phase1.md` mục "Ngoài
  phạm vi").
- `8.2.1` Security review chính thức (sanity SQL injection, xoá dữ liệu khi gỡ app) — việc khác, vẫn Human-do
  riêng, chưa làm.
- Nếu dùng Play App Signing (Google quản lý key ký cuối cùng, bạn chỉ giữ "upload key") — quy trình trên vẫn
  đúng cho upload key; đăng ký Play App Signing là bước làm trên Play Console, ngoài phạm vi file này.
