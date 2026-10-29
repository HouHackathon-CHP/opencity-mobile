# OpenCity Mobile

Ứng dụng Android dành cho **người dân**, thuộc dự án OpenCity của HouHackathon-CHP. Ứng dụng dùng [DX-OS Core](https://github.com/HouHackathon-CHP/dxos-core) để đăng nhập và đi qua API gateway tới [OpenCity Backend](https://github.com/HouHackathon-CHP/opencity-backend). Mã nguồn mobile được giữ trong repo riêng.

## Đã hoạt động

| Mục | Chức năng |
| --- | --- |
| Tài khoản | Đăng nhập Keycloak bằng Authorization Code + PKCE, lưu và làm mới phiên trên thiết bị, đăng xuất. |
| Phản ánh | Tạo phản ánh có ảnh/vị trí tùy chọn, lưu bản nháp văn bản cục bộ, xem danh sách và tiến trình xử lý của chính mình. |
| Tin & cảnh báo | Đọc bản tin/cảnh báo trong tenant, xem chi tiết và lọc theo loại. Chỉ operator của backend mới đăng tin. |
| Khảo sát ý kiến | Xem khảo sát trong tenant, chọn một phương án một lần và xem kết quả sau khi trả lời hoặc khảo sát đóng. |
| Bản đồ | Hiển thị phản ánh của chính người dùng và cảnh báo có tọa độ bằng MapLibre. |
| Danh mục | Hiển thị các ý tưởng dịch vụ với trạng thái thật; các mục chưa triển khai không dẫn tới trang giả. |

Các phần như dịch vụ công, ví giấy tờ, y tế, giao thông và thông báo đẩy cần API/dữ liệu hợp lệ trước khi bật. App không tích hợp với dịch vụ chính thức của Hà Nội. Nội dung `[DEMO]` chỉ là dữ liệu thử nghiệm.

## Chạy trên Android emulator

Cần Docker Desktop, Android Studio với emulator đang chạy, Android SDK, JDK 21 và **Node.js 24**. Máy phát triển hiện tại có Node 24 trong runtime của Codex; `scripts/android.ps1` sẽ dùng bản đó nếu `node` mặc định là phiên bản khác. Expo Go không chạy được native MapLibre và luồng OAuth này; dùng development build.

Để mở project Android và bấm **Run** trong Android Studio, cần bản hỗ trợ **Android Gradle Plugin 9.2.1**: tối thiểu **Android Studio Panda 4 (2025.3.4)** theo [bảng tương thích chính thức](https://developer.android.com/build/releases/about-agp#compatibility). Android Studio Panda 3 (2025.3.3) chỉ hỗ trợ đến AGP 9.1 và sẽ báo lỗi Gradle Sync. Sau khi chạy `scripts/android.ps1 -Build` để tạo thư mục `android/`, mở chính thư mục `opencity-mobile/android` trong Android Studio, chờ Gradle Sync, chọn cấu hình `app` và emulator rồi bấm Run. Thư mục `android/` được Expo tạo cục bộ và không được commit. Bản development build vẫn cần Metro chạy qua `scripts/android.ps1` để tải mã JavaScript; backend dữ liệu chạy riêng trong Docker.

Đặt ba repo `dxos-core`, `opencity-backend`, `opencity-mobile` cạnh nhau. Từ `opencity-backend`:

```powershell
docker compose --env-file ..\dxos-core\.env -f ..\dxos-core\compose.yaml -f compose.stack.yaml up --build -d
..\dxos-core\.venv\Scripts\python.exe ..\dxos-core\scripts\smoke.py
.venv\Scripts\python.exe scripts\smoke_app.py
```

`smoke.py` bảo đảm client OIDC `dxos-mobile` có trong realm đang dùng và tạo tài khoản thử nghiệm. `smoke_app.py` kiểm thử backend và tạo một cảnh báo `[DEMO]` trên bản đồ nếu chưa có. Sau đó, trong `opencity-mobile`:

```powershell
npm ci
.\scripts\android.ps1 -Build
```

Lệnh trên tạo một junction đường dẫn ngắn ở gốc ổ đĩa (`D:\oc` trên máy dùng ổ D:) để tránh giới hạn 260 ký tự của Ninja trên Windows. Junction trỏ tới repo thật, không sao chép dữ liệu. Script cũng cấu hình `adb reverse` cho Keycloak `8081`, gateway `8000`, Metro `19000` và build/cài app lên emulator. Các lần chạy tiếp theo chỉ cần `.\scripts\android.ps1`, rồi mở **OpenCity** trên emulator và chọn server `http://127.0.0.1:19000` trong Development Build. Đăng nhập với tên `dxos-citizen` và mật khẩu `DEMO_CITIZEN_PASSWORD` trong `..\dxos-core\.env`. Không đưa mật khẩu vào repo.

Nếu thay đổi native plugin hoặc Android config, chạy lại với `-Build`. Khi chạy trên điện thoại thật hoặc máy chủ khác, cần cấu hình URL công khai, HTTPS và redirect OIDC tương ứng; cấu hình `localhost` ở đây dành riêng cho emulator.

Nút **Lấy vị trí hiện tại** cần quyền vị trí và GPS hoạt động. Trên emulator, hãy đặt vị trí trong **Extended controls → Location**; nếu thiết bị không trả tọa độ trong 10 giây, app sẽ báo lỗi và vẫn cho phép gửi phản ánh bằng địa chỉ nhập tay.

## Cấu hình

Các biến Expo có thể đặt trong `.env.local` (không commit):

```dotenv
EXPO_PUBLIC_IDENTITY_URL=http://localhost:8081
EXPO_PUBLIC_GATEWAY_URL=http://localhost:8000
# Tùy chọn: URL style MapLibre do bạn vận hành.
# EXPO_PUBLIC_MAP_STYLE_URL=https://maps.example.org/style.json
```

Mặc định bản đồ dùng raster tiles OpenStreetMap để thử trên emulator, có ghi nguồn trên bản đồ. Khi triển khai thật, hãy cấu hình tile/style server có đủ năng lực và quyền phục vụ lưu lượng của mình. Android hiện cho phép HTTP để thử cục bộ. Cần chuyển sang HTTPS và bỏ cleartext trước khi phát hành.

## Kiểm tra

```powershell
npm run typecheck
npm run check:licenses
npx expo export --platform android
```

CI kiểm tra ba bước trên bằng Node 24. Native Android build và thao tác đăng nhập trên emulator cần SDK/Keycloak chạy cục bộ.

## Nguồn mở

Repo theo giấy phép MIT. `scripts/check-licenses.mjs` kiểm tra metadata giấy phép của các package npm trực tiếp và bắc cầu; xem [báo cáo rà soát](docs/license-audit.md). Một ngoại lệ được ghi rõ là bộ dữ liệu trình duyệt `caniuse-lite` theo CC-BY-4.0, không phải giấy phép phần mềm OSI. Việc kiểm tra metadata không bao phủ Android SDK, Gradle artifacts, tile server hoặc nội dung bên ngoài. Chạy `npm audit` và xử lý advisory trước phát hành.
