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

