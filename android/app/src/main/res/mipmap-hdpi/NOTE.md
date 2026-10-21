# Thiếu launcher icon thật

Chưa có file `ic_launcher.png` (hoặc adaptive icon) trong các thư mục `mipmap-*`. Đây là asset đồ
họa, không được tạo tay trong vòng implement này (không có Flutter/Android SDK để verify build).
Cần thêm icon thật trước khi build APK lần đầu — nếu không, build sẽ lỗi thiếu resource
`@mipmap/ic_launcher` tham chiếu trong `AndroidManifest.xml`.
