# 💧 Smart Hydration Assistant
### Ứng dụng nhắc uống nước thông minh dựa trên ngữ cảnh
**Môn: Lập trình Web & Ứng dụng di động | Java Android Native**

---

## 📁 Cấu trúc dự án

```
HydrationApp/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/hydration/app/
│   │   ├── activities/
│   │   │   ├── MainActivity.java        ← Màn hình chính
│   │   │   ├── HistoryActivity.java     ← Lịch sử uống nước
│   │   │   ├── ProfileActivity.java     ← Hồ sơ & cài đặt
│   │   │   └── StatsActivity.java       ← Biểu đồ thống kê
│   │   ├── adapters/
│   │   │   └── WaterEntryAdapter.java   ← RecyclerView adapter
│   │   ├── database/
│   │   │   └── DatabaseHelper.java      ← SQLiteOpenHelper
│   │   ├── models/
│   │   │   ├── WaterEntry.java          ← Model dữ liệu uống nước
│   │   │   └── WeatherData.java         ← Model dữ liệu thời tiết
│   │   ├── receivers/
│   │   │   ├── ReminderReceiver.java    ← BroadcastReceiver nhận alarm
│   │   │   └── BootReceiver.java        ← Khôi phục alarm sau reboot
│   │   └── utils/
│   │       ├── GoalCalculator.java      ← Tính mục tiêu & interval nhắc
│   │       ├── WeatherFetcher.java      ← AsyncTask gọi API thời tiết
│   │       ├── NotificationHelper.java  ← Quản lý notification & alarm
│   │       └── PreferenceManager.java   ← Wrapper SharedPreferences
│   └── res/
│       ├── layout/                      ← Tất cả XML layout
│       ├── drawable/                    ← Icons & shapes
│       ├── values/                      ← strings, colors, themes, dimens
│       └── menu/                        ← Menu overflow
```

---

## 🚀 Hướng dẫn chạy

### Bước 1 — Lấy API Key miễn phí
1. Truy cập [openweathermap.org](https://openweathermap.org/api)
2. Đăng ký tài khoản miễn phí
3. Vào **API Keys** → copy key (kích hoạt sau ~2 giờ)
4. Mở hoặc tạo file `local.properties` theo file mẫu `local.properties.example`, viết:
   ```
   WEATHER_API_KEY="<api_key>"
   ```

### Bước 2 — Mở trong Android Studio
1. **File → Open** → chọn thư mục `HydrationApp`
2. Chờ Gradle sync (lần đầu tải ~3 phút)
3. Nếu hỏi về JDK → chọn JDK 17

### Bước 3 — Thêm repository cho MPAndroidChart
Thư viện MPAndroidChart không có trên Maven Central mặc định.
Mở file `settings.gradle`, thêm `jitpack.io`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }  // ← THÊM DÒNG NÀY
    }
}
```

### Bước 4 — Chạy app
1. Kết nối điện thoại Android (bật Developer Mode + USB Debugging)
   **hoặc** dùng Android Emulator (API 24+)
2. Nhấn **Run ▶** hoặc `Shift+F10`

### Bước 5 — Lần đầu dùng
1. App mở ProfileActivity → nhập **cân nặng** và **thành phố**
2. Ví dụ: `65` kg, `Hanoi` (hoặc `Ho Chi Minh City`)
3. Nhấn **Lưu cài đặt**
4. App tự tính mục tiêu nước và bắt đầu nhắc nhở

---

## 📱 Demo 

### Kịch bản 

| Bước | Thao tác | Giải thích cho GV |
|------|----------|-------------------|
| 1 | Mở ProfileActivity | "SharedPreferences lưu cân nặng và thành phố" |
| 2 | Xem weather card | "AsyncTask gọi OpenWeather API, parse JSON thủ công" |
| 3 | Nhấn +250ml vài lần | "INSERT vào SQLite, ProgressBar cập nhật real-time" |
| 4 | Mở History | "RecyclerView hiển thị data từ SQLite query" |
| 5 | Mở Stats | "GROUP BY date trong SQLite, vẽ BarChart" |
| 6 | Bấm Test Notification | "AlarmManager trigger BroadcastReceiver" |
| 7 | Giải thích smart logic | "Interval tính theo lượng còn thiếu + giờ còn lại" |

---

## ⚠️ Lỗi thường gặp & cách fix

### Lỗi 1: "NetworkOnMainThreadException"
```
Nguyên nhân: Gọi API trực tiếp trong Activity (không dùng AsyncTask)
Fix: Đảm bảo WeatherFetcher.fetch() được gọi, không gọi HttpURLConnection trực tiếp
```

### Lỗi 2: Chart không hiển thị (MPAndroidChart)
```
Nguyên nhân: Thiếu JitPack repository trong settings.gradle
Fix: Thêm maven { url 'https://jitpack.io' } vào dependencyResolutionManagement
```

### Lỗi 3: Notification không hiện
```
Nguyên nhân 1: Chưa xin quyền POST_NOTIFICATIONS (Android 13+)
Nguyên nhân 2: Chưa tạo NotificationChannel
Fix: Kiểm tra NotificationHelper.createNotificationChannel() được gọi trong onCreate()
```

### Lỗi 4: API Key lỗi (cod: 401)
```
Nguyên nhân: Key chưa kích hoạt (cần chờ ~2 giờ sau khi đăng ký)
Fix: Dùng key demo hoặc chờ key kích hoạt
```

### Lỗi 5: Alarm không trigger sau khi tắt máy
```
Nguyên nhân: AlarmManager bị xóa khi reboot
Fix: BootReceiver đã xử lý → kiểm tra permission RECEIVE_BOOT_COMPLETED trong Manifest
```

---

## 🔑 Điểm nổi bật 

1. **Không hard-code giờ nhắc** — interval tính động dựa trên lượng nước còn thiếu và thời tiết
2. **Offline-first** — cache thời tiết vào SharedPreferences, hoạt động tốt khi mất mạng
3. **Singleton DatabaseHelper** — tránh tạo nhiều connection tới DB
4. **ViewHolder pattern** — tối ưu RecyclerView, tránh `findViewById()` lặp lại
5. **Proper lifecycle** — `onResume()` refresh data, không dùng static state

