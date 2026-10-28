# MeoMeo Launcher (Google TV / Android TV)

Launcher cho Google TV và Android TV, viết bằng **Kotlin + Jetpack Compose**, giao diện kiểu **macOS**
(menu bar, dock, Launchpad, cửa sổ kính mờ) nhưng được làm nhẹ để vẫn mượt trên TV cấu hình yếu.

## ✨ Tính năng

### Màn hình chính
- **Desktop kiểu macOS**: hình nền toàn màn hình, **menu bar** mỏng phía trên (tên app đang chọn, Wi-Fi, hẹn giờ, Cài đặt, ngày giờ) và **dock** kính phía dưới.
- **Dock 7 ô yêu thích + Launchpad**: icon phóng to dần khi lướt qua, có tooltip tên app. Ô cuối mở **Launchpad** (lưới toàn bộ ứng dụng trên nền mờ).
- **Hộp thoại là cửa sổ macOS**: nút đỏ-vàng-xanh, Cài đặt dạng nhóm hàng có icon màu, menu chuột phải cho ứng dụng, thẻ PIN và màn hình khóa có giờ lớn kèm avatar.
- Hình nền gồm **album Google Photos** (tự đồng bộ, đổi ảnh mỗi 45 giây) và 3 hình nền dựng sẵn. Ảnh dọc được hiển thị nguyên vẹn ở giữa trên nền mờ, không bị cắt mặt.

### Điều khiển bằng remote
| Phím | Tác dụng |
|---|---|
| D-Pad | Di chuyển giữa dock, menu bar và các cửa sổ |
| OK | Mở ứng dụng / chọn |
| Giữ OK hoặc phím Menu | Mở menu tùy chọn của ứng dụng (đổi ô dock, Yêu thích, thông tin, gỡ cài đặt) |
| Back | Đóng cửa sổ hiện tại (ở màn hình chính thì không thoát launcher) |

### Tùy chỉnh
- **Dock**: `Cài đặt > Dock` hoặc giữ OK trên một ô để đổi ứng dụng ở từng vị trí.
- **Ứng dụng di động (sideload)**: công tắc trong Cài đặt để hiện cả các app Android điện thoại không có banner Leanback.
- **Tự làm mới danh sách app** khi có app được cài, cập nhật hoặc gỡ (`BroadcastReceiver`).

### Đổi album Google Photos bằng điện thoại
Nhập link album dài bằng remote rất khó, nên TV cho phép dùng điện thoại:
1. Vào `Cài đặt > Hình nền > Đổi album bằng điện thoại`. TV hiện **mã QR** và địa chỉ dạng chữ.
2. Quét bằng điện thoại **cùng Wi-Fi** với TV, dán link album đã chia sẻ (hoặc nguyên câu chia sẻ của Google Photos) rồi nhấn **Gửi tới TV**.
3. TV tự đồng bộ và báo kết quả ngay trong hộp thoại. Nếu album không có ảnh thì giữ nguyên album cũ. Nút "Dùng lại album mặc định" đưa về album trong mã nguồn.

Cách hoạt động: TV chạy một web server nhỏ trong mạng LAN, **chỉ khi hộp thoại đang mở**. Mọi địa chỉ chứa token ngẫu nhiên, và server chỉ nhận link Google Photos (`photos.app.goo.gl`, `photos.google.com`, `goo.gl`), nên không thể bắt TV tải một địa chỉ khác trong mạng. Mã QR tạo bằng ZXing core.

Không mở được trang trên điện thoại? Kiểm tra điện thoại dùng Wi-Fi (không phải 4G/5G hay Wi-Fi khách), và router không bật "cách ly thiết bị" (AP isolation). Hộp thoại sẽ báo "Điện thoại đã kết nối" ngay khi điện thoại mở được trang.

### Hẹn giờ xem (khóa của phụ huynh)
- Vào `Cài đặt > Hẹn giờ xem`. Lần đầu bắt buộc tạo mã PIN 4 số; các lần sau phải nhập PIN mới mở được.
- Chọn thời gian từ 1 đến 60 phút rồi bấm bắt đầu. Hết giờ, màn hình khóa phủ kín và chỉ còn ô nhập PIN (dùng được cả phím số trên remote).
- Nhập đúng PIN để đặt lại thời gian hoặc hủy hẹn giờ. Nhập sai 5 lần sẽ bị khóa 30 giây.
- Để màn hình khóa tự bật lên khi đang xem app khác, cần cấp quyền **Hiển thị trên ứng dụng khác**. Nếu TV không có trang cấp quyền:
  ```bash
  adb shell appops set com.meomeo.tvlauncher SYSTEM_ALERT_WINDOW allow
  ```

### Tự cập nhật từ GitHub Releases
- Launcher kiểm tra bản mới khi mở (tối đa 6 giờ một lần, im lặng nếu lỗi). Có bản mới thì nút Cài đặt trên menu bar hiện **chấm xanh**.
- Vào `Cài đặt > Phần mềm > Cập nhật` rồi bấm **Tải và cập nhật**: app tải APK (có thanh tiến trình), kiểm tra SHA-256 do GitHub công bố, rồi gọi trình cài đặt của Android. Bạn chỉ cần bấm Update một lần.
- Điều kiện:
  - Kho mã nguồn phải **public** (TV gọi API GitHub không kèm token) và APK phải được **ký bằng khóa cố định** (xem [Ký APK](#ký-apk)); nếu không Android từ chối cài đè.
  - Lần đầu cần bật `Cài đặt TV > Ứng dụng > Quyền truy cập đặc biệt > Cài ứng dụng không rõ nguồn > MeoMeo Launcher`. Launcher **tự mở trang này** và hướng dẫn trong hộp thoại. TV không có trang đó thì dùng ADB:
    ```bash
    adb shell appops set com.meomeo.tvlauncher REQUEST_INSTALL_PACKAGES allow
    ```
  - Android không cho app cài hoàn toàn im lặng, nên luôn có một hộp thoại xác nhận.

### Nhẹ cho TV yếu
Không dùng blur theo khung hình, shader hay thư viện kính (Kyant0, Liquid-Glass-Android...):
- "Kính" là hình nền được decode một lần ở cỡ rất nhỏ (tự mờ khi phóng lên). Mỗi mặt kính chỉ vẽ lại đúng vùng nền phía sau nó rồi phủ một lớp tint, nên hiệu ứng như nhau trên mọi TV.
- Ảnh nền kế tiếp được giải mã sẵn ở luồng nền, ảnh cũ ở nguyên đến khi ảnh mới sẵn sàng, rồi mới chuyển mờ. TV ít RAM giải mã ở 2/3 kích thước.
- Font hệ thống (Roboto) thay cho SF Pro, vì SF không được phép dùng ngoài thiết bị Apple.

---

## 📋 Yêu cầu
- Google TV / Android TV, **Android 8.0 (API 26) trở lên**.
- Đồng bộ Google Photos và cập nhật cần mạng Internet.

---

## 🚀 Cài đặt lên TV

### 1. Lấy file APK
Tải `MeoMeoLauncher-v1.0.N.apk` mới nhất ở tab **Releases** của repo. Hoặc tự build bản debug (xem [Phát triển](#-phát-triển)).

### 2. Cài qua Wi-Fi ADB
1. Bật **Developer Options** và **Network Debugging** trên TV:
   - Vào `Cài đặt > Hệ thống > Giới thiệu > Bản dựng hệ điều hành Android TV` và bấm 7 lần phím OK.
   - Quay lại `Tùy chọn cho nhà phát triển > Bật "Gỡ lỗi qua mạng"`.
2. Trên máy tính, kết nối tới địa chỉ IP của TV:
   ```bash
   adb connect <IP_CỦA_TV>:5555
   ```
3. Cài ứng dụng:
   ```bash
   adb install -r MeoMeoLauncher-v1.0.N.apk
   ```

> Nếu trên TV đang có bản ký bằng **khóa khác** (ví dụ bản debug tự build), phải gỡ rồi cài lại một lần: `adb uninstall com.meomeo.tvlauncher`. Từ bản đó trở đi mới cập nhật được trong app.

### 3. Đặt làm launcher mặc định
- **Cách 1**: bấm nút **Home** trên remote, hệ thống sẽ hỏi chọn launcher (chọn *MeoMeo Launcher* và bấm *Luôn luôn*).
- **Cách 2** (một số mẫu Google TV khóa phím Home): tắt launcher mặc định bằng ADB:
  ```bash
  adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
  # Bật lại khi cần:
  adb shell pm enable com.google.android.apps.tv.launcherx
  ```
  Hoặc dùng các app như **Button Mapper** / **Launch on Boot** để gán phím Home mở MeoMeo Launcher.

### Quyền ứng dụng
| Quyền | Dùng để |
|---|---|
| `QUERY_ALL_PACKAGES` | Liệt kê ứng dụng đã cài |
| `INTERNET`, `ACCESS_NETWORK_STATE` | Đồng bộ Google Photos, kiểm tra cập nhật, nhận link album từ điện thoại |
| `REQUEST_INSTALL_PACKAGES` | Cài bản cập nhật của chính launcher |
| `SYSTEM_ALERT_WINDOW` | Hiện màn hình khóa lên trên app khác khi hết giờ |
| `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM` | Đặt báo thức để màn hình khóa bật đúng lúc hết giờ xem |
| `RECEIVE_BOOT_COMPLETED` | Đã khai báo; hiện chưa có receiver riêng. Thời điểm hết giờ được lưu và kiểm tra lại mỗi khi launcher mở, nên hẹn giờ vẫn còn sau khi TV khởi động lại |

---

## 🛠️ Phát triển

### Build
Cần JDK 21 và Android SDK 35.
```bash
./gradlew :app:assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```
`versionCode` / `versionName` lấy từ biến môi trường `VERSION_CODE` / `VERSION_NAME` (CI đặt theo số lần chạy; build local mặc định là 1 và `1.0.0`).

### Cấu trúc dự án
```
app/src/main/java/com/meomeo/tvlauncher/
├── MainActivity.kt              # Activity toàn màn hình, phím Back, nhận sự kiện cài/gỡ app
├── data/
│   ├── AppRepository.kt         # Quét app, banner, icon, yêu thích, các ô dock
│   ├── GooglePhotosWallpaperManager.kt  # Tải album Google Photos về máy
│   ├── AlbumLinks.kt            # Trích và kiểm tra link album từ văn bản dán vào
│   └── AlbumLinkServer.kt       # Web server nhỏ nhận link từ điện thoại
├── update/
│   ├── UpdateChecker.kt         # Hỏi GitHub Releases có bản mới không
│   └── ApkInstaller.kt          # Tải APK, kiểm tra SHA-256, cài qua PackageInstaller
├── timer/                       # Hẹn giờ xem + PIN phụ huynh
├── model/AppInfo.kt
└── ui/
    ├── MainViewModel.kt         # State và logic của toàn bộ màn hình
    ├── MainScreen.kt            # Desktop + menu bar + dock, điều phối các hộp thoại
    ├── TimerHost.kt
    ├── components/
    │   ├── Desktop.kt           # Hình nền, menu bar, dock có phóng to
    │   ├── Glass.kt             # Mặt kính mờ không blur theo khung hình
    │   ├── Ui.kt                # FocusSurface, hàng cài đặt, khung cửa sổ macOS, Launchpad
    │   ├── AppTiles.kt          # Icon ứng dụng, ô Launchpad
    │   ├── AllAppsDialog.kt, AppPickerDialog.kt, DockCustomizerDialog.kt
    │   ├── SettingsDialog.kt, WallpaperPickerDialog.kt, AppContextMenuDialog.kt
    │   ├── AlbumLinkDialog.kt, QrCode.kt, UpdateDialog.kt
    │   └── TimerUi.kt           # Bàn phím PIN, hẹn giờ, màn hình khóa
    └── theme/                   # Màu, hình dạng, font, trích màu icon
app/src/debug/                   # Chỉ bản debug: cho phép HTTP tới 10.0.2.2 để thử cập nhật offline
```

### Thử luồng cập nhật mà không cần phát hành
Bản debug cho phép trỏ bộ kiểm tra cập nhật về một server cục bộ (`api_override` trong SharedPreferences `update_debug`) và cho phép HTTP tới `10.0.2.2`. Dựng một APK có `versionCode` cao hơn, dùng một server giả trả JSON giống `releases/latest` (có `tag_name`, `assets[].browser_download_url`, `size`, `digest`), rồi chạy trên emulator. Bản release bỏ qua cơ chế này.

---

## 🤖 CI/CD (GitHub Actions)

Workflow: [`.github/workflows/build-release.yml`](.github/workflows/build-release.yml)

- **Mở hoặc cập nhật PR vào `main`**: *không* build.
- **Merge / push vào `main`**: build APK release và tự tạo **GitHub Release** `v1.0.<số lần chạy>` kèm file `MeoMeoLauncher-v1.0.N.apk`. `versionCode` bằng số lần chạy nên bản sau luôn cập nhật đè được bản trước (và launcher so số này để biết có bản mới).
- Có thể chạy tay ở tab *Actions > Build & Release > Run workflow*; chỉ `main` mới tạo release.
- Nếu `main` bật "Branch protection" yêu cầu check build cho PR, hãy bỏ yêu cầu đó vì PR không chạy build.

### Ký APK
Nếu chưa cấu hình, APK được ký bằng khóa debug do runner tự sinh và **khác nhau ở mỗi lần build**, nên không cài đè được lên bản trước và **tính năng tự cập nhật không dùng được**. Để các bản release cùng chữ ký:

```bash
# 1. Tạo keystore (một lần; giữ file và mật khẩu ở nơi an toàn, NGOÀI thư mục dự án)
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

Lưu ý:
- **Sao lưu `release.jks`**: mất khóa này thì mọi TV đang cài phải gỡ rồi cài lại.
- `.gitignore` đã chặn `*.jks`, `*.keystore`, `encoded.txt`, `*.base64`. Xóa file base64 sau khi đã dán vào secret.
- Nếu workflow cần quyền tạo release, vào *Settings > Actions > General > Workflow permissions* và chọn **Read and write permissions**.

---

## 🔒 Lưu ý khi để kho mã nguồn công khai
- Tự cập nhật cần repo **public**. Đừng commit keystore, mật khẩu hay token.
- Album Google Photos mặc định nằm trong mã nguồn (`GooglePhotosWallpaperManager.DEFAULT_ALBUM_URL`) nên **ai cũng xem được album đó** khi repo public, và link cũ vẫn còn trong lịch sử git. Nếu album có ảnh riêng tư, hãy tắt chia sẻ album cũ, dùng một link khác và nhập qua QR trên TV thay vì để trong mã.
