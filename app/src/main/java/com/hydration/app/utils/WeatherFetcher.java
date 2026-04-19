package com.hydration.app.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.hydration.app.BuildConfig;
import com.hydration.app.models.WeatherData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * WEATHER FETCHER
 *
 * Gọi OpenWeather API bằng HttpURLConnection (Lập trình mạng)
 * Chạy trên background thread bằng AsyncTask (Async processing)
 * Parse JSON thủ công bằng org.json (có sẵn trong Android)
 *
 * -------------------------------------------------------
 * GIẢI THÍCH RESTFUL API:
 * -------------------------------------------------------
 * Endpoint: GET https://api.openweathermap.org/data/2.5/weather
 * Method:   GET (chỉ đọc, không thay đổi server → đúng chuẩn REST)
 * Params:   q={city}         - tên thành phố
 *           appid={key}      - API key xác thực
 *           units=metric     - đơn vị: °C thay vì Kelvin
 *
 * Request Header:
 *   Content-Type: application/json (ngầm định)
 *
 * Response JSON:
 * {
 *   "main": {
 *     "temp": 33.5,       // nhiệt độ °C
 *     "humidity": 80      // độ ẩm %
 *   },
 *   "weather": [
 *     { "description": "scattered clouds" }
 *   ],
 *   "name": "Hanoi",
 *   "cod": 200            // HTTP status code trong body
 * }
 * -------------------------------------------------------
 */
public class WeatherFetcher extends AsyncTask<String, Void, WeatherData> {

    private static final String TAG      = "WeatherFetcher";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    // API Key của OpenWeatherMap - sinh viên cần đăng ký miễn phí tại openweathermap.org
    // Thay thế bằng key thực khi chạy
    public static final String API_KEY = BuildConfig.WEATHER_API_KEY;


    private static final int CONNECT_TIMEOUT = 10000; // 10 giây
    private static final int READ_TIMEOUT    = 10000;

    /**
     * Interface callback - pattern phổ biến trong Android
     * Dùng để truyền kết quả từ background thread về Activity
     */
    public interface WeatherCallback {
        void onSuccess(WeatherData data);
        void onError(String message);
    }

    private final WeatherCallback callback;

    public WeatherFetcher(WeatherCallback callback) {
        this.callback = callback;
    }

    /**
     * doInBackground: Chạy trên BACKGROUND THREAD
     * Đây là nơi thực hiện network call
     * KHÔNG được cập nhật UI ở đây (sẽ crash)
     *
     * @param params params[0] = tên thành phố
     */
    @Override
    protected WeatherData doInBackground(String... params) {
        String city = params.length > 0 ? params[0] : "Hanoi";

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // 1. Xây dựng URL với query parameters
            String encodedCity = URLEncoder.encode(city, "UTF-8");
            String urlString = BASE_URL
                    + "?q="     + encodedCity
                    + "&appid=" + API_KEY
                    + "&units=metric";  // Celsius

            Log.d(TAG, "Fetching: " + urlString);

            // 2. Tạo connection (HttpURLConnection - thuần Java)
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.connect();

            // 3. Kiểm tra HTTP status code
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Response code: " + responseCode);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return new WeatherData("Lỗi HTTP " + responseCode);
            }

            // 4. Đọc response body
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String jsonResponse = sb.toString();
            Log.d(TAG, "Response: " + jsonResponse);

            // 5. Parse JSON thủ công (KHÔNG dùng Gson/Jackson)
            return parseWeatherJson(jsonResponse);

        } catch (Exception e) {
            Log.e(TAG, "Error fetching weather", e);
            return new WeatherData("Không thể kết nối: " + e.getMessage());
        } finally {
            // Luôn đóng connection và reader trong finally
            if (connection != null) connection.disconnect();
            try {
                if (reader != null) reader.close();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Parse JSON response từ OpenWeather API
     *
     * JSON structure:
     * {
     *   "main": { "temp": 33.5, "humidity": 80 },
     *   "weather": [{ "description": "scattered clouds" }],
     *   "name": "Hanoi"
     * }
     */
    private WeatherData parseWeatherJson(String json) throws Exception {
        // Tạo JSONObject từ string
        JSONObject root = new JSONObject(json);

        // Lấy object "main" chứa nhiệt độ và độ ẩm
        JSONObject main = root.getJSONObject("main");
        double temp     = main.getDouble("temp");
        int humidity    = main.getInt("humidity");

        // Lấy mảng "weather", lấy phần tử đầu tiên
        JSONArray weatherArray = root.getJSONArray("weather");
        String description = "";
        if (weatherArray.length() > 0) {
            JSONObject weather = weatherArray.getJSONObject(0);
            description = weather.getString("description");
        }

        // Lấy tên thành phố
        String cityName = root.getString("name");

        Log.d(TAG, String.format("Parsed: %s, %.1f°C, humidity %d%%", cityName, temp, humidity));
        return new WeatherData(temp, humidity, description, cityName);
    }

    /**
     * onPostExecute: Chạy trên MAIN THREAD (UI thread)
     * Được gọi tự động sau khi doInBackground hoàn thành
     * Đây là nơi an toàn để cập nhật UI
     */
    @Override
    protected void onPostExecute(WeatherData result) {
        if (callback == null) return;

        if (result != null && result.isSuccess()) {
            callback.onSuccess(result);
        } else {
            String error = result != null ? result.getErrorMessage() : "Lỗi không xác định";
            callback.onError(error);
        }
    }

    /**
     * Static helper: tạo và chạy ngay
     * Cách dùng:
     *   WeatherFetcher.fetch("Hanoi", new WeatherCallback() { ... });
     */
    public static void fetch(String city, WeatherCallback callback) {
        new WeatherFetcher(callback).execute(city);
    }
}
