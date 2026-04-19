package com.hydration.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hydration.app.R;
import com.hydration.app.database.DatabaseHelper;
import com.hydration.app.models.DrinkType;
import com.hydration.app.models.WeatherData;
import com.hydration.app.utils.ChallengeManager;
import com.hydration.app.utils.GoalCalculator;
import com.hydration.app.utils.NotificationHelper;
import com.hydration.app.utils.PreferenceManager;
import com.hydration.app.utils.WeatherFetcher;

import java.util.Calendar;
import java.util.List;

/**
 * MAIN ACTIVITY — Màn hình chính
 *
 * Tích hợp các tính năng mới:
 *   1. Adaptive Reminder — interval tự điều chỉnh theo tiến độ
 *   2. Hydration Score   — hiển thị điểm sức khỏe hôm nay
 *   3. Smart Suggestion  — gợi ý ngữ cảnh theo giờ + thời tiết
 *   4. Custom Drink Type — chọn loại đồ uống khi nhập
 *   5. Challenge badge   — hiển thị challenge gần nhất
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    // ===== Views =====
    private ProgressBar progressBar;
    private TextView tvProgress;
    private TextView tvPercent;
    private TextView tvMotivation;
    private TextView tvWeatherInfo;
    private TextView tvGoalInfo;
    private LinearLayout layoutWeatherLoading;
    private LinearLayout layoutWeatherContent;

    // Views mới cho các tính năng
    private TextView tvHydrationScore;    // "Hydration Score: 78/100 — Tốt 👍"
    private TextView tvSmartSuggestion;   // gợi ý ngữ cảnh
    private TextView tvChallengeBadge;    // "🔥 3 ngày liên tiếp! Tiếp tục nhé"

    // ===== Helpers =====
    private DatabaseHelper   db;
    private PreferenceManager prefs;

    // ===== State =====
    private int    currentConsumed = 0;
    private int    currentGoal     = 2100;
    private double currentTemp     = 28.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db    = DatabaseHelper.getInstance(this);
        prefs = new PreferenceManager(this);

        NotificationHelper.createNotificationChannel(this);
        initViews();
        requestNotificationPermission();

        if (!prefs.isSetupDone()) {
            startActivity(new Intent(this, ProfileActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
        fetchWeather();
    }

    private void initViews() {
        progressBar          = findViewById(R.id.progress_water);
        tvProgress           = findViewById(R.id.tv_progress_text);
        tvPercent            = findViewById(R.id.tv_percent);
        tvMotivation         = findViewById(R.id.tv_motivation);
        tvWeatherInfo        = findViewById(R.id.tv_weather_info);
        tvGoalInfo           = findViewById(R.id.tv_goal_info);
        layoutWeatherLoading = findViewById(R.id.layout_weather_loading);
        layoutWeatherContent = findViewById(R.id.layout_weather_content);

        // Views tính năng mới (thêm vào layout activity_main.xml)
        tvHydrationScore  = findViewById(R.id.tv_hydration_score);
        tvSmartSuggestion = findViewById(R.id.tv_smart_suggestion);
        tvChallengeBadge  = findViewById(R.id.tv_challenge_badge);

        // Nút uống nhanh
        findViewById(R.id.btn_drink_150).setOnClickListener(v -> showDrinkTypeDialog(150));
        findViewById(R.id.btn_drink_250).setOnClickListener(v -> showDrinkTypeDialog(250));
        findViewById(R.id.btn_drink_500).setOnClickListener(v -> showDrinkTypeDialog(500));
        findViewById(R.id.btn_drink_custom).setOnClickListener(v -> showCustomInputDialog());

        // Bottom nav
        findViewById(R.id.btn_nav_history).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btn_nav_stats).setOnClickListener(v ->
                startActivity(new Intent(this, StatsActivity.class)));
        findViewById(R.id.btn_nav_profile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        // Bấm vào challenge badge → mở stats
        if (tvChallengeBadge != null) {
            tvChallengeBadge.setOnClickListener(v ->
                    startActivity(new Intent(this, StatsActivity.class)));
        }
    }

    // ============================================================
    // REFRESH UI — cập nhật toàn bộ màn hình
    // ============================================================
    private void refreshUI() {
        currentConsumed = db.getTodayTotal();
        currentGoal     = prefs.getDailyGoal();
        int entryCount  = db.getTodayEntryCount();

        // Progress bar
        progressBar.setMax(currentGoal);
        progressBar.setProgress(Math.min(currentConsumed, currentGoal));

        int percent = currentGoal > 0 ? (currentConsumed * 100 / currentGoal) : 0;
        tvProgress.setText(currentConsumed + " / " + currentGoal + " ml");
        tvPercent.setText(percent + "%");
        tvGoalInfo.setText("Mục tiêu: " + currentGoal + " ml/ngày");
        tvMotivation.setText(GoalCalculator.getMotivationMessage(currentConsumed, currentGoal));
        updateProgressColor(percent);

        // ── Tính năng 3: Hydration Score ──
        if (tvHydrationScore != null) {
            int score = GoalCalculator.calculateHydrationScore(currentConsumed, currentGoal, entryCount);
            String label = GoalCalculator.getScoreLabel(score);
            tvHydrationScore.setText("Score hôm nay: " + score + "/100  " + label);
            // Màu score
            int scoreColor;
            if (score >= 70)       scoreColor = Color.parseColor("#2E7D32"); // xanh đậm
            else if (score >= 40)  scoreColor = Color.parseColor("#F57F17"); // cam
            else                   scoreColor = Color.parseColor("#C62828"); // đỏ
            tvHydrationScore.setTextColor(scoreColor);
        }

        // ── Tính năng 4: Smart Suggestion ──
        if (tvSmartSuggestion != null) {
            String suggestion = GoalCalculator.getSmartSuggestion(
                    currentConsumed, currentGoal, currentTemp);
            tvSmartSuggestion.setText(suggestion);
        }

        // ── Tính năng 6: Challenge Badge ──
        if (tvChallengeBadge != null) {
            List<ChallengeManager.Challenge> challenges =
                    ChallengeManager.getAllChallenges(db, currentGoal);
            int done  = ChallengeManager.countCompleted(challenges);
            ChallengeManager.Challenge next = ChallengeManager.getNextChallenge(challenges);

            if (done == challenges.size()) {
                tvChallengeBadge.setText("🏆 Tất cả " + done + " thử thách hoàn thành!");
                tvChallengeBadge.setBackgroundColor(Color.parseColor("#E8F5E9"));
            } else if (next != null) {
                tvChallengeBadge.setText(next.icon + " Mục tiêu: " + next.title
                        + "  (" + done + "/" + challenges.size() + " xong)");
                tvChallengeBadge.setBackgroundColor(Color.parseColor("#FFF8E1"));
            }
        }
    }

    // ============================================================
    // TÍNH NĂNG 7: Dialog chọn loại đồ uống
    // ============================================================
    /**
     * Khi bấm nút uống nhanh → hỏi loại đồ uống trước khi ghi.
     * Nếu là nước lọc → ghi thẳng (phổ biến nhất).
     * Nếu là loại khác → hiển thị dialog chọn.
     */
    private void showDrinkTypeDialog(int amount) {
        String[] options = DrinkType.getDisplayNames();

        new AlertDialog.Builder(this)
                .setTitle("Bạn vừa uống gì? (" + amount + "ml)")
                .setItems(options, (dialog, which) -> {
                    DrinkType chosen = DrinkType.values()[which];
                    addWater(amount, chosen);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Dialog nhập lượng tùy chỉnh + chọn loại đồ uống.
     */
    private void showCustomInputDialog() {
        // Bước 1: nhập lượng
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập lượng đồ uống (ml)");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Ví dụ: 350");
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        builder.setView(input);

        builder.setPositiveButton("Tiếp theo →", (d, w) -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            try {
                int amount = Integer.parseInt(text);
                if (amount <= 0 || amount > 2000) {
                    Toast.makeText(this, "Nhập từ 1 – 2000ml", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Bước 2: chọn loại
                showDrinkTypeDialog(amount);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số không hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    // ============================================================
    // GHI NƯỚC — cốt lõi
    // ============================================================
    /**
     * Thêm lượng uống vào DB, cập nhật UI, reschedule reminder.
     * effective = amount × hydrationFactor (tự tính trong DrinkType)
     */
    private void addWater(int amount, DrinkType drinkType) {
        db.insertWaterEntry(amount, drinkType);

        int effective = drinkType.calculateEffectiveMl(amount);
        currentConsumed += effective;

        refreshUI();

        // Toast thông minh
        String msg = "+" + amount + "ml " + drinkType.getIcon();
        if (drinkType != DrinkType.WATER) {
            msg += "  (tính " + effective + "ml vào mục tiêu)";
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        rescheduleReminder();

        // Kiểm tra có achievement mới không
        checkNewAchievement();
    }

    /** Tương thích ngược — mặc định WATER */
    private void addWater(int amount) {
        addWater(amount, DrinkType.WATER);
    }

    /**
     * Kiểm tra achievement sau khi uống.
     * Nếu vừa đạt mục tiêu → thông báo chúc mừng.
     */
    private void checkNewAchievement() {
        if (currentConsumed >= currentGoal) {
            int score = GoalCalculator.calculateHydrationScore(
                    currentConsumed, currentGoal, db.getTodayEntryCount());
            if (score >= 90) {
                showAchievementToast("🏆 Xuất sắc! Score " + score + "/100 hôm nay!");
            } else if (currentConsumed >= currentGoal) {
                showAchievementToast("🎉 Bạn đã đạt mục tiêu hôm nay!");
            }
        }
    }

    private void showAchievementToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ============================================================
    // WEATHER & REMINDER (giữ nguyên logic, thêm temp cho suggestion)
    // ============================================================
    private void fetchWeather() {
        String city = prefs.getCity();
        layoutWeatherLoading.setVisibility(View.VISIBLE);
        layoutWeatherContent.setVisibility(View.GONE);

        WeatherFetcher.fetch(city, new WeatherFetcher.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData data) {
                currentTemp = data.getTemperature();
                prefs.cacheWeather(data.getTemperature(), data.getHumidity());

                tvWeatherInfo.setText(data.getWeatherIcon() + " "
                        + data.getCityName() + "  "
                        + (int) data.getTemperature() + "°C  "
                        + data.getTemperatureLabel());

                layoutWeatherLoading.setVisibility(View.GONE);
                layoutWeatherContent.setVisibility(View.VISIBLE);

                // Refresh lại suggestion với nhiệt độ mới
                refreshUI();
            }

            @Override
            public void onError(String message) {
                currentTemp = prefs.getCachedTemperature();
                tvWeatherInfo.setText("⚠️ Dùng dữ liệu thời tiết đã lưu");
                layoutWeatherLoading.setVisibility(View.GONE);
                layoutWeatherContent.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Lên lịch nhắc với Adaptive interval (Tính năng 1).
     * GoalCalculator.calculateReminderInterval() đã có logic phân tầng.
     */
    private void rescheduleReminder() {
        if (!prefs.isReminderEnabled()) return;
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int interval    = GoalCalculator.calculateReminderInterval(
                currentConsumed, currentGoal, currentHour);

        if (interval > 0) {
            NotificationHelper.scheduleNextReminder(this, interval);
        } else {
            NotificationHelper.cancelReminder(this);
        }
    }

    private void updateProgressColor(int percent) {
        int color;
        if (percent < 30)      color = Color.parseColor("#FF5252");
        else if (percent < 70) color = Color.parseColor("#FFD740");
        else                   color = Color.parseColor("#4CAF50");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            progressBar.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(color));
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.POST_NOTIFICATIONS },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Đã bật thông báo nhắc nhở", Toast.LENGTH_SHORT).show();
            rescheduleReminder();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh_weather) {
            fetchWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}