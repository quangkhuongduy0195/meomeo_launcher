# MeoMeo Launcher (Google TV / Android TV)

Một launcher tối giản, mượt mà và hiện đại được viết bằng **Kotlin + Jetpack Compose** cho Google TV và Android TV.

## ✨ Tính năng nổi bật
- **Giao diện tối giản (Minimalist & Smooth)**: Tông màu tối điện ảnh (Cinematic Dark) tối ưu cho màn hình TV OLED/LED.
- **D-Pad Focus mượt mà**: Hiệu ứng phóng to thẻ (scale), viền sáng (glow/sheen) và đổ bóng nổi mượt mà khi dùng Remote TV.
- **Top Header thông minh**:
  - Đồng hồ to sắc nét theo thời gian thực + ngày tháng tiếng Việt.
  - Phím tắt nhanh mở Cài đặt Wi-Fi và Cài đặt TV.
- **Hàng Yêu thích (Favorites)**: Ghim các ứng dụng hay dùng nhất lên đầu.
- **Context Menu trên Remote**:
  - Nhấn giữ phím **OK / D-Pad Center** hoặc bấm nút **Menu** trên remote để:
    - Thêm / Xóa khỏi danh sách Yêu thích.
    - Mở Cài đặt chi tiết của ứng dụng.
    - Gỡ cài đặt ứng dụng.
- **Tùy chọn App di động (Sideload)**:
  - Tích hợp sẵn switch bật/tắt trong phần Cài đặt Launcher để quét và hiển thị cả các ứng dụng Android điện thoại không có Leanback banner chuẩn.
- **Giao diện kiểu macOS**: hình nền toàn màn hình, menu bar mỏng phía trên (tên app đang chọn, Wi-Fi, hẹn giờ, cài đặt, giờ), dock kính phía dưới với icon phóng to dần khi lướt qua, ô cuối là Launchpad (lưới toàn bộ ứng dụng trên nền mờ). Hộp thoại là cửa sổ macOS có nút đỏ-vàng-xanh, Cài đặt dạng nhóm hàng có icon màu, menu chuột phải cho ứng dụng, màn hình khóa có giờ lớn và avatar. "Kính" không dùng blur theo khung hình: hình nền được decode một lần ở cỡ rất nhỏ (tự mờ khi phóng lên) và mỗi mặt kính vẽ lại đúng vùng nền phía sau nó rồi phủ một lớp tint, nên hiệu ứng như nhau trên mọi TV, kể cả máy yếu. Không dùng thư viện kính nào. Font hệ thống (Roboto) thay cho SF Pro vì SF không được phép dùng ngoài thiết bị Apple.
- **Đổi album Google Photos bằng điện thoại**: vào `Cài đặt > Hình nền > Đổi album bằng điện thoại`. TV hiện mã QR; quét bằng điện thoại cùng Wi-Fi, dán link album đã chia sẻ (hoặc nguyên câu chia sẻ của Google Photos) rồi nhấn "Gửi tới TV". TV tự đồng bộ album và báo kết quả; nếu album không có ảnh thì giữ nguyên album cũ. Bên dưới là một web server nhỏ chỉ chạy khi hộp thoại đang mở, mọi địa chỉ chứa token ngẫu nhiên, và chỉ nhận link Google Photos (không thể dùng để bắt TV tải địa chỉ khác trong mạng). Mã QR tạo bằng ZXing core.
- **Tự cập nhật từ GitHub Releases**: launcher kiểm tra bản mới khi mở (tối đa 6 giờ một lần, im lặng nếu lỗi); có bản mới thì nút Cài đặt trên menu bar hiện chấm xanh. Vào `Cài đặt > Phần mềm > Cập nhật` để xem, bấm "Tải và cập nhật": app tải APK (có thanh tiến trình, kiểm tra SHA-256 do GitHub công bố) rồi gọi trình cài đặt của Android, bạn chỉ cần bấm Update một lần. Điều kiện:
  - Kho mã nguồn phải **public** (TV gọi API GitHub không kèm token) và APK phải được **ký bằng khóa cố định** (xem phần "Ký APK"), nếu không Android từ chối cài đè.
  - Lần đầu cần bật `Cài đặt TV > Ứng dụng > Quyền truy cập đặc biệt > Cài ứng dụng không rõ nguồn > MeoMeo Launcher` (launcher tự mở trang này). TV không có trang đó thì dùng ADB: `adb shell appops set com.meomeo.tvlauncher REQUEST_INSTALL_PACKAGES allow`.
  - Android không cho app cài hoàn toàn im lặng, nên luôn có một hộp thoại xác nhận.
- **Hẹn giờ xem (khóa của phụ huynh)**:
  - Vào `Cài đặt > Hẹn giờ xem`. Lần đầu bắt buộc tạo mã PIN 4 số; các lần sau phải nhập PIN mới mở được.
  - Chọn thời gian từ 1 đến 60 phút rồi bấm bắt đầu. Hết giờ, một màn hình đen phủ kín và chỉ còn ô nhập PIN cùng bàn phím số (dùng được cả phím số trên remote).
  - Nhập đúng PIN để đặt lại thời gian hoặc hủy hẹn giờ. Nhập sai 5 lần sẽ bị khóa 30 giây.
  - Để màn hình khóa tự bật lên khi đang xem app khác, cần cấp quyền **Hiển thị trên ứng dụng khác**. Nếu TV không có trang cấp quyền, dùng ADB:
    ```bash
    adb shell appops set com.meomeo.tvlauncher SYSTEM_ALERT_WINDOW allow
    ```
- **Tự động làm mới**: Tự cập nhật danh sách ứng dụng khi có app mới được cài đặt, cập nhật hoặc gỡ bỏ (`BroadcastReceiver`).

---

## 🛠️ Cấu trúc dự án
```
launcher_tv_meomeo/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Cấu hình HOME & LEANBACK_LAUNCHER
│   │   ├── java/com/meomeo/tvlauncher/
│   │   │   ├── MainActivity.kt          # Fullscreen TV Activity & phím Back
│   │   │   ├── data/
│   │   │   │   └── AppRepository.kt     # Quét app, banner, icon & favorites
│   │   │   ├── model/
│   │   │   │   └── AppInfo.kt           # Entity chứa thông tin ứng dụng
│   │   │   └── ui/
│   │   │       ├── MainViewModel.kt     # State flow & xử lý logic
│   │   │       ├── MainScreen.kt        # Màn hình chính
│   │   │       ├── components/
│   │   │       │   ├── AppCard.kt       # Card ứng dụng hỗ trợ focus D-Pad
│   │   │       │   ├── TopHeader.kt     # Header đồng hồ & lối tắt
│   │   │       │   ├── AppContextMenuDialog.kt # Menu tùy chọn app
│   │   │       │   └── SettingsDialog.kt       # Cài đặt launcher & toggle sideload
│   │   │       └── theme/
│   │   └── res/                         # Icon, Banner TV, Color, Styles
```

---

## 🚀 Cách cài đặt lên Google TV / Android TV

### 1. File APK đã build sẵn
File APK debug đã được tạo tại:
`app/build/outputs/apk/debug/app-debug.apk`

### 2. Cài đặt qua Wi-Fi ADB
1. Bật **Developer Options** và **USB/Network Debugging** trên TV:
   - Vào `Cài đặt > Hệ thống > Giới thiệu > Bản dựng hệ điều hành Android TV` và bấm 7 lần phím OK.
   - Quay lại `Tùy chọn cho nhà phát triển > Bật "Gỡ lỗi qua mạng" (Network debugging)`.
2. Trên máy tính Mac, kết nối tới địa chỉ IP của TV:
   ```bash
   adb connect <IP_CỦA_TV>:5555
   ```
3. Cài đặt ứng dụng:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### 3. Đặt làm Launcher mặc định trên Google TV
- **Cách 1**: Bấm nút **Home** trên remote TV, hệ thống sẽ hỏi chọn launcher (chọn *MeoMeo Launcher* và bấm *Luôn luôn*).
- **Cách 2 (Trên một số mẫu Google TV khoá Home)**: Dùng lệnh ADB:
  ```bash
  # Tắt launcher mặc định của Google TV
  adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx

  # Nếu muốn bật lại:
  adb shell pm enable com.google.android.apps.tv.launcherx
  ```
  Hoặc sử dụng các ứng dụng như **Button Mapper** / **Launch on Boot** để gán phím Home mở MeoMeo Launcher.

---

## 🤖 CI/CD (GitHub Actions)

Workflow: [`.github/workflows/build-release.yml`](.github/workflows/build-release.yml)

- **Pull request vào `main`**: chỉ build kiểm tra và đính kèm APK vào workflow run (tab *Actions*).
- **Merge / push vào `main`**: build APK release và tự tạo **GitHub Release** `v1.0.<số lần chạy>`, đính kèm file `MeoMeoLauncher-v1.0.N.apk` (tab *Releases*). `versionCode` bằng số lần chạy nên bản sau luôn cập nhật đè được bản trước.
- Cũng có thể chạy tay ở tab *Actions > Build & Release > Run workflow*.

### Ký APK (nên làm trước khi phát hành thật)

Nếu chưa cấu hình, APK được ký bằng khóa debug do runner tự sinh và **khác nhau ở mỗi lần build**, nên không cài đè lên bản trước được (phải gỡ rồi cài lại). Để các bản release cùng chữ ký:

```bash
# 1. Tạo keystore (chỉ làm một lần, hãy giữ file này và mật khẩu ở nơi an toàn)
keytool -genkeypair -v -keystore release.jks -alias meomeo -keyalg RSA -keysize 2048 -validity 10000

# 2. Mã hóa base64 để dán vào secret
base64 -i release.jks | pbcopy        # macOS
```

Vào *Settings > Secrets and variables > Actions* của repo và thêm 4 secret:

| Secret | Giá trị |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | nội dung base64 ở bước 2 |
| `ANDROID_KEYSTORE_PASSWORD` | mật khẩu keystore |
| `ANDROID_KEY_ALIAS` | `meomeo` (alias đã dùng ở bước 1) |
| `ANDROID_KEY_PASSWORD` | mật khẩu của khóa |

Lưu ý: nếu workflow cần quyền tạo release, vào *Settings > Actions > General > Workflow permissions* và chọn **Read and write permissions**.
