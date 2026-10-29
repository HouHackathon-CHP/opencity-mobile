# OpenCity Mobile

Ứng dụng **Android Kotlin + Jetpack Compose** cho người dân. Đây là client giao diện: đăng nhập bằng Keycloak của [DX-OS Core](https://github.com/HouHackathon-CHP/dxos-core) và gọi API của [OpenCity Backend](https://github.com/HouHackathon-CHP/opencity-backend) qua gateway. Không có backend hoặc Metro riêng trong repo này.

## Chức năng

- Đăng nhập OAuth 2.0 Authorization Code + PKCE bằng AppAuth; phiên được mã hóa với Android Keystore, làm mới token khi cần.
- Tạo phản ánh có ảnh/vị trí tùy chọn, lưu bản nháp trên máy, xem danh sách, chi tiết và tiến trình.
- Đọc tin/cảnh báo và trả lời khảo sát một lần.
- Xem phản ánh/cảnh báo có tọa độ trên MapLibre Native.
- Xem danh mục dịch vụ với trạng thái tích hợp thực tế. Các mục chưa có API không mở trang giả.

App chưa tích hợp dịch vụ chính thức của Hà Nội. Dữ liệu `[DEMO]` chỉ để thử nghiệm.

## Mở và chạy trong Android Studio

1. Mở **thư mục gốc repo `D:\MNM2026\opencity-mobile`** bằng Android Studio. Chọn Gradle project `OpenCity`, cấu hình chạy `app` và Android emulator.
2. Đảm bảo Docker Desktop đã chạy, rồi khởi động backend từ `D:\MNM2026\opencity-backend`:

   ```powershell
   docker compose --env-file ..\dxos-core\.env -f ..\dxos-core\compose.yaml -f compose.stack.yaml up --build -d
   ..\dxos-core\.venv\Scripts\python.exe ..\dxos-core\scripts\smoke.py
   .venv\Scripts\python.exe scripts\smoke_app.py
   ```

3. Kết nối các cổng localhost của máy với emulator (mỗi lần emulator khởi động lại):

   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb reverse tcp:8081 tcp:8081
   & $adb reverse tcp:8000 tcp:8000
   ```

4. Bấm **Run ▶** trong Android Studio. App được build và cài trực tiếp; không cần `npm`, `npx`, Expo hoặc Metro. Đăng nhập bằng tài khoản thử `dxos-citizen`; mật khẩu nằm trong `DEMO_CITIZEN_PASSWORD` ở `..\dxos-core\.env`.

Chạy bằng PowerShell cũng được: `./gradlew.bat :app:installDebug`. Android Studio tự tạo `local.properties` chứa đường dẫn SDK. Nếu dùng CLI trước khi mở Android Studio, tạo `local.properties` với `sdk.dir=C:/Users/<user>/AppData/Local/Android/Sdk`.

Nếu Run báo `Requested internal only, but not enough space`, emulator đã thiếu bộ nhớ trong. Hãy tạo một AVD mới trong **Device Manager** và chọn AVD đó để chạy; thao tác này không xóa dữ liệu của AVD cũ. AVD `Medium_Phone_API_36.0` trên máy thử nghiệm hiện chỉ còn khoảng 314 MB trống.

## Cấu hình

Mặc định bản `debug` gọi `http://127.0.0.1:8081` (Keycloak) và `http://127.0.0.1:8000` (gateway) qua `adb reverse`. Có thể ghi đè trong `local.properties`:

```properties
IDENTITY_URL=https://identity.example.org
GATEWAY_URL=https://api.example.org
```

`local.properties` không được commit. Bản `release` chặn HTTP cleartext; trước khi phát hành phải cấu hình HTTPS công khai và redirect OIDC cho môi trường đó. Bản demo cần Chrome hoặc trình duyệt hỗ trợ Custom Tabs trên emulator. Nếu lấy GPS không được, đặt vị trí trong **Extended controls → Location** hoặc nhập địa chỉ bằng tay.

Bản đồ demo dùng raster tiles OpenStreetMap và ghi nguồn ngay dưới bản đồ. Khi triển khai thật cần style/tile server có quyền và đủ năng lực phục vụ lưu lượng của mình.

## Công nghệ và giấy phép

Kotlin, Jetpack Compose, Android Gradle Plugin, AppAuth, OkHttp và MapLibre Native Android. Mã nguồn repo phát hành theo [MIT](LICENSE). AppAuth dùng Apache-2.0, MapLibre Native dùng BSD-2-Clause; đây đều là giấy phép được OSI phê duyệt. Tài nguyên tile/dữ liệu bản đồ có điều kiện sử dụng riêng.
