package com.hydration.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * PREFERENCE MANAGER
 *
 * Wrapper cho SharedPreferences - lưu cài đặt người dùng:
 *   - Cân nặng
 *   - Thành phố
 *   - Mục tiêu nước hàng ngày (có thể override)
 *   - Nhiệt độ cached (để dùng khi offline)
 *   - Trạng thái reminder
 *
 * SharedPreferences phù hợp cho dữ liệu đơn giản kiểu key-value.
 * Dữ liệu phức tạp (lịch sử uống nước) → dùng SQLite.
 */
public class PreferenceManager {

    private static final String PREF_NAME       = "hydration_prefs";

    // Keys
    private static final String KEY_WEIGHT      = "weight_kg";
    private static final String KEY_CITY        = "city";
    private static final String KEY_DAILY_GOAL  = "daily_goal_ml";
    private static final String KEY_TEMP_CACHED = "cached_temperature";
    private static final String KEY_HUM_CACHED  = "cached_humidity";
    private static final String KEY_SETUP_DONE  = "setup_completed";
    private static final String KEY_REMINDER_ON = "reminder_enabled";

    // Defaults
    private static final float  DEFAULT_WEIGHT  = 60f;
    private static final String DEFAULT_CITY    = "Hanoi";
    private static final int    DEFAULT_GOAL    = 2100; // 60kg × 35ml
    private static final double DEFAULT_TEMP    = 28.0;

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ===== WEIGHT =====
    public float getWeight() {
        return prefs.getFloat(KEY_WEIGHT, DEFAULT_WEIGHT);
    }
    public void setWeight(float weight) {
        prefs.edit().putFloat(KEY_WEIGHT, weight).apply();
        // Tự động tính lại goal khi thay đổi cân nặng
        int newGoal = GoalCalculator.calculateDailyGoal(weight, getCachedTemperature(), getCachedHumidity());
        setDailyGoal(newGoal);
    }

    // ===== CITY =====
    public String getCity() {
        return prefs.getString(KEY_CITY, DEFAULT_CITY);
    }
    public void setCity(String city) {
        prefs.edit().putString(KEY_CITY, city).apply();
    }

    // ===== DAILY GOAL =====
    public int getDailyGoal() {
        return prefs.getInt(KEY_DAILY_GOAL, DEFAULT_GOAL);
    }
    public void setDailyGoal(int goalMl) {
        prefs.edit().putInt(KEY_DAILY_GOAL, goalMl).apply();
    }

    // ===== CACHED WEATHER =====
    public double getCachedTemperature() {
        // Float được lưu là bits của double → convert lại
        return Double.longBitsToDouble(prefs.getLong(KEY_TEMP_CACHED,
                Double.doubleToLongBits(DEFAULT_TEMP)));
    }
    public int getCachedHumidity() {
        return prefs.getInt(KEY_HUM_CACHED, 60);
    }
    public void cacheWeather(double temp, int humidity) {
        prefs.edit()
                .putLong(KEY_TEMP_CACHED, Double.doubleToLongBits(temp))
                .putInt(KEY_HUM_CACHED, humidity)
                .apply();
        // Cập nhật goal ngay khi có thời tiết mới
        int newGoal = GoalCalculator.calculateDailyGoal(getWeight(), temp, humidity);
        setDailyGoal(newGoal);
    }

    // ===== SETUP =====
    public boolean isSetupDone() {
        return prefs.getBoolean(KEY_SETUP_DONE, false);
    }
    public void setSetupDone(boolean done) {
        prefs.edit().putBoolean(KEY_SETUP_DONE, done).apply();
    }

    // ===== REMINDER =====
    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ON, true);
    }
    public void setReminderEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_REMINDER_ON, enabled).apply();
    }
}
