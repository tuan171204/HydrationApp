package com.hydration.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hydration.app.database.DatabaseHelper;
import com.hydration.app.utils.GoalCalculator;
import com.hydration.app.utils.NotificationHelper;
import com.hydration.app.utils.PreferenceManager;
import com.hydration.app.utils.WeatherFetcher;

import java.util.Calendar;

/**
 * REMINDER RECEIVER
 *
 * BroadcastReceiver được kích hoạt bởi AlarmManager.
 * Đây là core của smart notification system:
 *
 * Khi nhận được alarm:
 *   1. Đọc lượng nước đã uống hôm nay từ SQLite
 *   2. Đọc mục tiêu từ SharedPreferences
 *   3. Quyết định có nên nhắc không (đã đủ? ban đêm?)
 *   4. Gửi notification với nội dung thông minh
 *   5. Lên lịch alarm tiếp theo (tự điều chỉnh interval)
 *
 * Lưu ý: onReceive() chạy trên Main Thread nhưng rất ngắn.
 * Không được làm gì nặng ở đây. Network call → dùng AsyncTask riêng.
 */
public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        // Đọc dữ liệu cần thiết
        DatabaseHelper db       = DatabaseHelper.getInstance(context);
        PreferenceManager prefs = new PreferenceManager(context);

        // Kiểm tra user có bật reminder không
        if (!prefs.isReminderEnabled()) {
            Log.d(TAG, "Reminder disabled by user");
            return;
        }

        // Kiểm tra giờ - không nhắc ban đêm (22h - 6h)
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (currentHour >= 22 || currentHour < 6) {
            Log.d(TAG, "Night time, skip reminder");
            return;
        }

        // Lấy dữ liệu từ DB và Preferences
        int consumed = db.getTodayTotal();
        int goal     = prefs.getDailyGoal();
        double temp  = prefs.getCachedTemperature();

        // Tính còn cần uống bao nhiêu
        int remaining = Math.max(0, goal - consumed);

        // Đã đủ nước → không nhắc, hủy alarm
        if (remaining <= 0) {
            Log.d(TAG, "Goal reached! No notification needed.");
            NotificationHelper.cancelReminder(context);
            return;
        }

        // Tạo nội dung notification thông minh
        String message = NotificationHelper.buildSmartMessage(temp, remaining);

        // Gửi notification
        NotificationHelper.showReminder(context, message, consumed, goal);

        // Tính interval tiếp theo và lên lịch lại
        int interval = GoalCalculator.calculateReminderInterval(consumed, goal, currentHour);

        if (interval > 0) {
            NotificationHelper.scheduleNextReminder(context, interval);
            Log.d(TAG, "Next reminder in " + interval + " minutes");
        } else {
            Log.d(TAG, "No more reminders today");
        }

        // Cập nhật thời tiết ngầm (không chặn main thread)
        // Dùng Executor thay vì AsyncTask (cách hiện đại hơn)
        refreshWeatherInBackground(context, prefs);
    }

    /**
     * Cập nhật dữ liệu thời tiết trong background
     * Dùng Executor - cách async hiện đại (Android khuyến nghị thay AsyncTask)
     * Kết quả sẽ được cache để dùng cho lần nhắc tiếp theo
     */
    private void refreshWeatherInBackground(Context context, PreferenceManager prefs) {
        String city = prefs.getCity();

        // Executor.newSingleThreadExecutor() = tạo 1 background thread
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            WeatherFetcher.fetch(city, new WeatherFetcher.WeatherCallback() {
                @Override
                public void onSuccess(com.hydration.app.models.WeatherData data) {
                    // Cache thời tiết mới
                    prefs.cacheWeather(data.getTemperature(), data.getHumidity());
                    Log.d(TAG, "Weather refreshed: " + data.getTemperature() + "°C");
                }

                @Override
                public void onError(String message) {
                    // Không cần làm gì, tiếp tục dùng cache cũ
                    Log.w(TAG, "Weather refresh failed: " + message);
                }
            });
        });
    }
}
