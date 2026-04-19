package com.hydration.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hydration.app.R;
import com.hydration.app.utils.GoalCalculator;
import com.hydration.app.utils.NotificationHelper;
import com.hydration.app.utils.PreferenceManager;
import com.hydration.app.utils.WeatherFetcher;
import com.hydration.app.models.WeatherData;

import java.util.Calendar;

/**
 * PROFILE ACTIVITY
 *
 * Cho phép user cài đặt:
 *   - Cân nặng (kg) → ảnh hưởng đến mục tiêu nước
 *   - Thành phố → ảnh hưởng đến API thời tiết
 *   - Bật/tắt nhắc nhở
 *
 * Sử dụng SharedPreferences để lưu (qua PreferenceManager)
 */
public class ProfileActivity extends AppCompatActivity {

    private EditText etWeight;
    private EditText etCity;
    private Switch switchReminder;
    private TextView tvCurrentGoal;
    private TextView tvGoalBreakdown;
    private Button btnSave;
    private Button btnTestNotification;

    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Hồ sơ & Cài đặt");
        }

        prefs = new PreferenceManager(this);
        initViews();
        loadCurrentSettings();
    }

    private void initViews() {
        etWeight           = findViewById(R.id.et_weight);
        etCity             = findViewById(R.id.et_city);
        switchReminder     = findViewById(R.id.switch_reminder);
        tvCurrentGoal      = findViewById(R.id.tv_current_goal);
        tvGoalBreakdown    = findViewById(R.id.tv_goal_breakdown);
        btnSave            = findViewById(R.id.btn_save_profile);
        btnTestNotification= findViewById(R.id.btn_test_notification);

        btnSave.setOnClickListener(v -> saveSettings());
        btnTestNotification.setOnClickListener(v -> sendTestNotification());

        // Preview goal khi thay đổi input
        etWeight.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateGoalPreview();
            }
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadCurrentSettings() {
        etWeight.setText(String.valueOf((int) prefs.getWeight()));
        etCity.setText(prefs.getCity());
        switchReminder.setChecked(prefs.isReminderEnabled());
        updateGoalPreview();
    }

    private void saveSettings() {
        String weightStr = etWeight.getText().toString().trim();
        String city      = etCity.getText().toString().trim();

        // Validate
        if (weightStr.isEmpty()) {
            etWeight.setError("Nhập cân nặng của bạn");
            return;
        }
        if (city.isEmpty()) {
            etCity.setError("Nhập tên thành phố");
            return;
        }

        float weight;
        try {
            weight = Float.parseFloat(weightStr);
            if (weight < 20 || weight > 300) {
                etWeight.setError("Nhập từ 20 - 300 kg");
                return;
            }
        } catch (NumberFormatException e) {
            etWeight.setError("Số không hợp lệ");
            return;
        }

        // Lưu vào SharedPreferences
        prefs.setWeight(weight);
        prefs.setCity(city);
        prefs.setReminderEnabled(switchReminder.isChecked());
        prefs.setSetupDone(true);

        Toast.makeText(this, "Đã lưu cài đặt ✓", Toast.LENGTH_SHORT).show();

        // Cập nhật alarm
        if (switchReminder.isChecked()) {
            int goal       = prefs.getDailyGoal();
            int consumed   = 0; // Không cần đọc DB ở đây
            int currentHour= Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int interval   = GoalCalculator.calculateReminderInterval(consumed, goal, currentHour);
            NotificationHelper.scheduleNextReminder(this, interval > 0 ? interval : 60);
        } else {
            NotificationHelper.cancelReminder(this);
        }

        // Nếu lần đầu setup → về MainActivity
        if (!getIntent().hasExtra("from_settings")) {
            finish();
        }
    }

    /**
     * Preview mục tiêu dựa trên cân nặng hiện tại nhập
     */
    private void updateGoalPreview() {
        String weightStr = etWeight.getText().toString().trim();
        if (weightStr.isEmpty()) return;

        try {
            float weight   = Float.parseFloat(weightStr);
            double temp    = prefs.getCachedTemperature();
            int humidity   = prefs.getCachedHumidity();
            int goal       = GoalCalculator.calculateDailyGoal(weight, temp, humidity);

            tvCurrentGoal.setText("Mục tiêu ước tính: " + goal + " ml/ngày");

            // Breakdown
            int base = (int)(weight * 35);
            int heatBonus = (int)(Math.max(0, temp - 25) * 50);
            int humBonus  = humidity > 70 ? 200 : 0;
            String breakdown = String.format(
                    "Cơ bản (%dkg × 35): %dml\n" +
                    "Thời tiết (%.0f°C): +%dml\n" +
                    "Độ ẩm (%d%%): +%dml",
                    (int)weight, base, temp, heatBonus, humidity, humBonus
            );
            tvGoalBreakdown.setText(breakdown);

        } catch (NumberFormatException ignored) {}
    }

    /**
     * Gửi test notification để demo cho giảng viên
     */
    private void sendTestNotification() {
        double temp   = prefs.getCachedTemperature();
        int goal      = prefs.getDailyGoal();
        String msg    = NotificationHelper.buildSmartMessage(temp, goal / 2);
        NotificationHelper.showReminder(this, msg, goal / 2, goal);
        Toast.makeText(this, "Đã gửi test notification!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
