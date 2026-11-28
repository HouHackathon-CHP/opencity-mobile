<div align="center">

# OpenCity Mobile — Android Citizen App

**Ứng dụng Android Công dân Thông minh (Kotlin &amp; Jetpack Compose) trên nền DX-OS**

<p>
  <a href="#gioi-thieu">Giới thiệu</a> &bull;
  <a href="#tinh-nang">Chức năng Thực tế</a> &bull;
  <a href="#cong-nghe">Công nghệ</a> &bull;
  <a href="#cai-dat">Cài đặt &amp; Chạy</a> &bull;
  <a href="#cau-hinh">Cấu hình</a> &bull;
  <a href="#thanh-vien">Đội ngũ</a>
</p>

<p>
  <img src="https://img.shields.io/badge/License-Apache_2.0-0f172a?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4?style=flat-square&logo=android&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/MapLibre-Native-396CB2?style=flat-square" alt="MapLibre Native">
  <img src="https://img.shields.io/badge/Gradle-8.11.1-02303A?style=flat-square&logo=gradle&logoColor=white" alt="Gradle">
</p>

</div>

---

## Giới thiệu <a id="gioi-thieu"></a>

**OpenCity Mobile** là ứng dụng Android bản địa (**Kotlin + Jetpack Compose**) dành cho người dân và điều phối viên di động, xác thực trực tiếp qua Keycloak của [DX-OS Core](https://github.com/HouHackathon-CHP/dxos-core) và gọi API của [OpenCity Backend](https://github.com/HouHackathon-CHP/opencity-backend) qua Gateway.

Dự án là một nguyên mẫu mã nguồn mở sử dụng nhận diện riêng, dữ liệu mở và dữ liệu thử nghiệm `[DEMO]`, không kết nối trực tiếp vào hệ thống nội bộ của cơ quan nhà nước.

---

## Chức năng Thực tế <a id="tinh-nang"></a>

* **Xác thực OIDC + PKCE An toàn**: Hỗ trợ cả **Đăng nhập** và **Đăng ký công dân** qua `AppAuth` (`opencity://auth`); phiên đăng nhập được mã hóa bằng `Android Keystore` và tự động làm mới token.
* **Phản ánh Hiện trường & Bản đồ GIS Đa tầng**: Tạo phản ánh kèm ảnh chụp và vị trí GPS, lưu bản nháp cục bộ khi chưa gửi, theo dõi chi tiết và tiến trình xử lý; xem bản đồ phản ánh/cảnh báo và bản đồ đô thị số đa tầng trên `MapLibre Native`.
* **Trung tâm Thao tác Cán bộ Điều phối**: Tiếp nhận, phân công phản ánh hiện trường, phê duyệt lịch hẹn và kiểm soát quyền ngay trên thiết bị di động.
* **Quan trắc Không khí & Giao thông**: Hiển thị toàn bộ các trạm đo WAQI tại Hà Nội trên bản đồ chất lượng không khí; tra cứu tuyến/lộ trình xe buýt thông minh.
* **Danh mục Dịch vụ Thống nhất & Bộ nhớ đệm Ngoại tuyến**: Tra cứu danh mục dịch vụ công gắn trạng thái tích hợp thực tế (các mục chưa có API không mở màn hình giả).
* **Tương tác Hành chính & Cộng đồng**: Đọc tin tức/cảnh báo, trả lời khảo sát một lần, đặt lịch tiếp công dân, gửi phản ánh thủ tục hành chính, xem truyền thông, nhóm cộng đồng, góp ý xã hội, thông tin xã/phường số và tra cứu đường dây nóng (chạm để mở trình quay số của điện thoại).

---

## Công nghệ Sử dụng <a id="cong-nghe"></a>

| Thành phần | Công nghệ | Vai trò |
| --- | --- | --- |
| **Ngôn ngữ & UI** | Kotlin + Jetpack Compose | Giao diện Android hiện đại, phản hồi mượt mà. |
| **Xác thực SSO** | AppAuth + Android Keystore | OAuth 2.0 Authorization Code + PKCE S256 với Keycloak (`dxos-mobile`). |
| **Bản đồ** | MapLibre Native SDK | Hiển thị bản đồ GIS đa tầng, phản ánh và các trạm WAQI. |
| **Build System** | Gradle `8.11.1` (JDK 21) | Quản lý phụ thuộc và build trực tiếp trên Android Studio hoặc CLI. |

---

## Mở và Chạy trong Android Studio <a id="cai-dat"></a>

Project dùng **Gradle 8.11.1**; trong **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle**, chọn **Gradle JDK 21** (ví dụ Temurin 21 — không dùng JDK 25). `.idea` là cấu hình riêng của máy, không commit.

1. Mở thư mục gốc repo `opencity-mobile` bằng **Android Studio**, chọn Gradle project `OpenCity`, cấu hình chạy `app` và Android emulator.
2. Khởi chạy `dxos-core` + `opencity-backend` ở thư mục anh em `opencity-backend`:
   ```powershell
   docker compose --env-file ..\dxos-core\.env -f ..\dxos-core\compose.yaml -f compose.stack.yaml up --build -d
   python ..\dxos-core\scripts\smoke.py
   python scripts\smoke_app.py
   ```
3. Kết nối các cổng `localhost` của máy với emulator (mỗi lần emulator khởi động lại):
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb reverse tcp:8081 tcp:8081
   & $adb reverse tcp:8000 tcp:8000
   ```
4. Bấm **Run** trong Android Studio (hoặc chạy `./gradlew.bat :app:installDebug`). Đăng nhập bằng tài khoản thử `dxos-citizen` (mật khẩu nằm trong `DEMO_CITIZEN_PASSWORD` ở `..\dxos-core\.env`).

---

## Cấu hình Môi trường <a id="cau-hinh"></a>

Mặc định bản `debug` gọi `http://localhost:8081` (Keycloak) và `http://127.0.0.1:8000` (Gateway) qua `adb reverse`. Có thể ghi đè trong `local.properties` (không commit):

```properties
IDENTITY_URL=https://identity.example.org
GATEWAY_URL=https://api.example.org
```

Bản `release` chặn HTTP cleartext; trước khi phát hành cần cấu hình HTTPS công khai và redirect OIDC tương ứng. Bản demo dùng raster tiles OpenStreetMap; khi triển khai thực tế cần tile server phù hợp với lưu lượng.

---

## Thành viên Dự án & Đóng góp <a id="thanh-vien"></a>

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/hieutnago">
        <img src="https://github.com/hieutnago.png" width="96px" alt="Trương Ngọc Hiểu"/><br>
        <sub><b>Trương Ngọc Hiểu</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/blueMonkey23">
        <img src="https://avatars.githubusercontent.com/u/168514215?v=4" width="96px" alt="Trần Trọng Chiến"/><br>
        <sub><b>Trần Trọng Chiến</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/ourstorycomic">
        <img src="https://avatars.githubusercontent.com/u/100331812?v=4" width="96px" alt="Nguyễn Hà Phương"/><br>
        <sub><b>Nguyễn Hà Phương</b></sub>
      </a>
    </td>
  </tr>
</table>

Xem hướng dẫn đóng góp tại [CONTRIBUTING.md](CONTRIBUTING.md), quy tắc ứng xử tại [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), chính sách bảo mật tại [SECURITY.md](SECURITY.md) và lịch sử thay đổi tại [CHANGELOG.md](CHANGELOG.md). Phát hành theo giấy phép **[Apache License 2.0](LICENSE)**.
