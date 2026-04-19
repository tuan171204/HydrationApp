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
4. Mở file `WeatherFetcher.java`, thay dòng:
   ```java
   public static final String API_KEY = "YOUR_API_KEY_HERE";
   // →
   public static final String API_KEY = "abc123xyz..."; // key của bạn
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

## 📱 Demo cho giảng viên

### Kịch bản demo (5-7 phút)

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

## 🎓 Giải thích theo kiến thức môn học

### 1. Lập trình mạng (Networking)

**File:** `WeatherFetcher.java`

```java
// Dùng HttpURLConnection - lớp thuần Java
URL url = new URL(urlString);
HttpURLConnection connection = (HttpURLConnection) url.openConnection();
connection.setRequestMethod("GET");
connection.setConnectTimeout(10000);
connection.connect();

// Đọc response
BufferedReader reader = new BufferedReader(
    new InputStreamReader(connection.getInputStream())
);
```

**Điểm quan trọng:** Network call KHÔNG được chạy trên Main Thread → sẽ bị lỗi `NetworkOnMainThreadException`. Phải dùng AsyncTask hoặc Executor.

---

### 2. RESTful API

**Endpoint đang dùng:**
```
GET https://api.openweathermap.org/data/2.5/weather?q=Hanoi&appid=KEY&units=metric
```

| Thành phần | Giá trị | Ý nghĩa |
|------------|---------|---------|
| **Method** | `GET` | Chỉ đọc dữ liệu → đúng nguyên tắc REST |
| **Resource** | `/weather` | Tài nguyên muốn lấy |
| **Params** | `q=Hanoi` | Bộ lọc theo thành phố |
| **Auth** | `appid=KEY` | API key xác thực |
| **Format** | `units=metric` | Celsius thay vì Kelvin |

**Parse JSON thủ công** (không dùng Gson/Jackson):
```java
JSONObject root = new JSONObject(jsonString);
JSONObject main = root.getJSONObject("main");
double temp     = main.getDouble("temp");       // 33.5
int humidity    = main.getInt("humidity");       // 80
String city     = root.getString("name");       // "Hanoi"
```

**Tại sao là RESTful?**
- **Stateless:** Mỗi request có đủ thông tin, server không lưu session
- **Resource-based URL:** `/weather` thay vì `/getWeatherData`
- **HTTP Method đúng nghĩa:** GET để đọc, không dùng POST

---

### 3. SQLite Database

**File:** `DatabaseHelper.java`

```java
// Kế thừa SQLiteOpenHelper - chuẩn Android
public class DatabaseHelper extends SQLiteOpenHelper {

    // onCreate: chạy 1 lần khi tạo DB lần đầu
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE water_entries (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "amount INTEGER NOT NULL, " +
            "timestamp TEXT NOT NULL, " +
            "note TEXT DEFAULT ''" +
        ")");
    }

    // INSERT
    public long insertWaterEntry(int amount) {
        ContentValues values = new ContentValues();
        values.put("amount", amount);
        values.put("timestamp", getCurrentTimestamp());
        return db.insert("water_entries", null, values);
    }

    // QUERY - tổng hôm nay
    public int getTodayTotal() {
        Cursor cursor = db.rawQuery(
            "SELECT SUM(amount) FROM water_entries WHERE date(timestamp) = ?",
            new String[]{ getTodayDate() }
        );
        cursor.moveToFirst();
        return cursor.getInt(0);
    }
}
```

**Khi nào dùng SQLite vs SharedPreferences?**
- **SQLite:** Dữ liệu nhiều record, cần query/filter/sort (lịch sử uống nước)
- **SharedPreferences:** Dữ liệu đơn giản key-value (cân nặng, thành phố, cài đặt)

---

### 4. Async Processing

**Cách 1 — AsyncTask** (trong `WeatherFetcher.java`):
```java
public class WeatherFetcher extends AsyncTask<String, Void, WeatherData> {

    @Override
    protected WeatherData doInBackground(String... params) {
        // ← Chạy trên BACKGROUND THREAD
        // Thực hiện network call ở đây
        return callWeatherAPI(params[0]);
    }

    @Override
    protected void onPostExecute(WeatherData result) {
        // ← Chạy trên MAIN THREAD (UI Thread)
        // Cập nhật UI ở đây
        callback.onSuccess(result);
    }
}
```

**Cách 2 — Executor** (trong `ReminderReceiver.java`):
```java
// Hiện đại hơn AsyncTask, Android khuyến dùng từ API 30+
Executors.newSingleThreadExecutor().execute(() -> {
    // Background work ở đây
    WeatherFetcher.fetch(city, callback);
});
```

**Tại sao cần Async?**
- Main Thread = UI Thread: chịu trách nhiệm render UI mỗi 16ms
- Network call có thể mất vài giây → block Main Thread → app bị đơ → ANR crash

---

### 5. UI Android (XML Layout)

**Các component đã dùng:**

| Component | File | Mục đích |
|-----------|------|---------|
| `ProgressBar` | activity_main.xml | Hiển thị % lượng nước hôm nay |
| `EditText` | activity_profile.xml | Nhập cân nặng, thành phố |
| `Button` | activity_main.xml | Nút +150ml, +250ml, +500ml |
| `RecyclerView` | activity_history.xml | Danh sách lịch sử uống nước |
| `Switch` | activity_profile.xml | Bật/tắt nhắc nhở |
| `CardView` | tất cả layouts | Container với bo góc và shadow |

**RecyclerView cần 3 thành phần:**
```java
// 1. LayoutManager - quyết định cách sắp xếp items
recyclerView.setLayoutManager(new LinearLayoutManager(this));

// 2. Adapter - kết nối data với View
adapter = new WaterEntryAdapter(entries);
recyclerView.setAdapter(adapter);

// 3. ViewHolder (trong Adapter) - cache reference tới Views
// → tránh gọi findViewById() lặp lại khi scroll
```

---

### 6. Smart Notification Logic

**Công thức tính mục tiêu ngày:**
```
dailyGoal = (weight × 35) + max(0, temp - 25) × 50 + (humidity > 70% ? 200 : 0)
```

**Công thức tính interval nhắc (phút):**
```
remaining   = dailyGoal - consumed
hoursLeft   = 22 - currentHour
timesNeeded = ceil(remaining / 250)
interval    = (hoursLeft × 60) / timesNeeded
interval    = clamp(interval, 20, 180)
```

**Flow notification:**
```
MainActivity.addWater()
    → db.insertWaterEntry()
    → GoalCalculator.calculateReminderInterval()
    → NotificationHelper.scheduleNextReminder()
        → AlarmManager.setExactAndAllowWhileIdle()
            → (sau X phút) ReminderReceiver.onReceive()
                → NotificationHelper.showReminder()
                → scheduleNextReminder() [lên lịch lần tiếp]
```

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

## 🔑 Điểm nổi bật để trình bày với giảng viên

1. **Không hard-code giờ nhắc** — interval tính động dựa trên lượng nước còn thiếu và thời tiết
2. **Offline-first** — cache thời tiết vào SharedPreferences, hoạt động tốt khi mất mạng
3. **Singleton DatabaseHelper** — tránh tạo nhiều connection tới DB
4. **ViewHolder pattern** — tối ưu RecyclerView, tránh `findViewById()` lặp lại
5. **Proper lifecycle** — `onResume()` refresh data, không dùng static state

---

## 📊 Checklist yêu cầu môn học

| Yêu cầu | File | Trạng thái |
|---------|------|-----------|
| HttpURLConnection / OkHttp | WeatherFetcher.java | ✅ HttpURLConnection |
| Parse JSON thủ công | WeatherFetcher.java | ✅ org.json.JSONObject |
| RESTful API + giải thích | README + WeatherFetcher | ✅ |
| SQLiteOpenHelper | DatabaseHelper.java | ✅ |
| INSERT | DatabaseHelper.insertWaterEntry() | ✅ |
| QUERY | DatabaseHelper.getTodayTotal(), getAllEntries() | ✅ |
| AsyncTask / Executor | WeatherFetcher + ReminderReceiver | ✅ Cả hai |
| EditText | activity_profile.xml | ✅ |
| Button | activity_main.xml | ✅ |
| RecyclerView | HistoryActivity + Adapter | ✅ |
| ProgressBar | activity_main.xml | ✅ |
| Local notification | NotificationHelper + ReminderReceiver | ✅ |
| Chart | StatsActivity + MPAndroidChart | ✅ BarChart |
| Smart goal (weight + weather) | GoalCalculator.java | ✅ |
| Không dùng Firebase | — | ✅ |
| Không over-engineering | — | ✅ |
