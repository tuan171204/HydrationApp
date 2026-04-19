package com.hydration.app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.hydration.app.R;
import com.hydration.app.database.DatabaseHelper;
import com.hydration.app.database.DatabaseHelper.DailyStat;
import com.hydration.app.database.DatabaseHelper.HourStat;
import com.hydration.app.database.DatabaseHelper.DrinkTypeStat;
import com.hydration.app.models.DrinkType;
import com.hydration.app.utils.ChallengeManager;
import com.hydration.app.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * STATS ACTIVITY — mở rộng với:
 *   - Tính năng 2: Drinking Pattern Analysis (giờ uống nhiều nhất)
 *   - Tính năng 6: Challenge list (hiển thị achievement)
 *   - Tính năng 7: Drink type breakdown
 */
public class StatsActivity extends AppCompatActivity {

    private BarChart barChart;
    private TextView tvAverage, tvBestDay, tvTotalWeek, tvStreak;

    // Views mới
    private TextView  tvPatternTitle;
    private LinearLayout layoutPatternHours;   // chứa các dòng giờ uống
    private LinearLayout layoutDrinkTypes;     // breakdown loại đồ uống
    private LinearLayout layoutChallenges;     // danh sách challenge

    private DatabaseHelper   db;
    private PreferenceManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thống kê & Thành tích");
        }

        db    = DatabaseHelper.getInstance(this);
        prefs = new PreferenceManager(this);

        initViews();
        loadWeeklyChart();
        loadPattern();       // Tính năng 2
        loadDrinkTypes();    // Tính năng 7
        loadChallenges();    // Tính năng 6
    }

    private void initViews() {
        barChart          = findViewById(R.id.bar_chart);
        tvAverage         = findViewById(R.id.tv_stat_average);
        tvBestDay         = findViewById(R.id.tv_stat_best);
        tvTotalWeek       = findViewById(R.id.tv_stat_total);
        tvStreak          = findViewById(R.id.tv_stat_streak);
        tvPatternTitle    = findViewById(R.id.tv_pattern_title);
        layoutPatternHours= findViewById(R.id.layout_pattern_hours);
        layoutDrinkTypes  = findViewById(R.id.layout_drink_types);
        layoutChallenges  = findViewById(R.id.layout_challenges);
        setupChart();
    }

    private void setupChart() {
        barChart.setDescription(null);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBorders(false);
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setExtraBottomOffset(8f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#666666"));
        xAxis.setTextSize(11f);

        YAxis left = barChart.getAxisLeft();
        left.setAxisMinimum(0f);
        left.setGridColor(Color.parseColor("#EEEEEE"));
        left.setTextColor(Color.parseColor("#666666"));
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return (int)v + "ml"; }
        });
        barChart.getAxisRight().setEnabled(false);
    }

    // ============================================================
    // WEEKLY CHART (giữ nguyên + thêm streak)
    // ============================================================
    private void loadWeeklyChart() {
        List<DailyStat> stats = db.getWeeklyStats();
        int goal  = prefs.getDailyGoal();

        if (stats.isEmpty()) {
            tvAverage.setText("Chưa có dữ liệu");
            tvBestDay.setText("-");
            tvTotalWeek.setText("0 ml");
            tvStreak.setText("0 ngày");
            return;
        }

        List<BarEntry> entries  = new ArrayList<>();
        String[]       labels   = new String[stats.size()];
        int totalWeek = 0, bestDay = 0;

        for (int i = 0; i < stats.size(); i++) {
            DailyStat s = stats.get(i);
            entries.add(new BarEntry(i, s.total));
            labels[i] = s.getShortLabel();
            totalWeek += s.total;
            if (s.total > bestDay) bestDay = s.total;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float v) { return v <= 0 ? "" : (int)v+""; }
        });

        List<Integer> colors = new ArrayList<>();
        for (DailyStat s : stats) {
            colors.add(s.total >= goal ? Color.parseColor("#42A5F5") : Color.parseColor("#FFCA28"));
        }
        dataSet.setColors(colors);

        LimitLine goalLine = new LimitLine((float)goal, "Mục tiêu");
        goalLine.setLineColor(Color.parseColor("#FF5252"));
        goalLine.setLineWidth(1.5f);
        goalLine.setTextColor(Color.parseColor("#FF5252"));
        goalLine.setTextSize(10f);
        goalLine.enableDashedLine(10f, 5f, 0f);
        barChart.getAxisLeft().removeAllLimitLines();
        barChart.getAxisLeft().addLimitLine(goalLine);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.length);
        barChart.animateY(800);
        barChart.invalidate();

        int average = stats.size() > 0 ? totalWeek / stats.size() : 0;
        tvAverage.setText(average + " ml");
        tvBestDay.setText(bestDay + " ml");
        tvTotalWeek.setText(totalWeek + " ml");

        // Streak từ DB
        int streak = db.getCurrentStreak(goal);
        tvStreak.setText(streak + " ngày");
    }

    // ============================================================
    // TÍNH NĂNG 2: Drinking Pattern Analysis
    // ============================================================
    private void loadPattern() {
        if (layoutPatternHours == null) return;
        layoutPatternHours.removeAllViews();

        List<HourStat> pattern = db.getDrinkingPatternByHour();

        if (pattern.isEmpty()) {
            TextView empty = makeTextView("Chưa đủ dữ liệu để phân tích thói quen.", 14, "#9E9E9E");
            layoutPatternHours.addView(empty);
            return;
        }

        // Tiêu đề
        if (tvPatternTitle != null) {
            tvPatternTitle.setText("Bạn thường uống nước vào:");
        }

        // Tìm max count để vẽ thanh tỷ lệ
        int maxCount = pattern.get(0).count;

        for (HourStat stat : pattern) {
            // Dòng: "9h sáng  ████░░  (12 lần)"
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 6, 0, 6);

            // Label giờ
            TextView tvHour = makeTextView(stat.getLabel(), 14, "#424242");
            tvHour.setMinWidth(dp(90));
            row.addView(tvHour);

            // Progress bar mini (dùng View)
            int barWidth = maxCount > 0 ? (int)(dp(120) * stat.count / (float)maxCount) : dp(10);
            View bar = new View(this);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(barWidth, dp(14));
            barParams.setMargins(8, 0, 8, 0);
            bar.setLayoutParams(barParams);
            bar.setBackgroundColor(Color.parseColor("#42A5F5"));
            // Set corner radius programmatically (API 21+)
            bar.setClipToOutline(true);
            row.addView(bar);

            // Count
            TextView tvCount = makeTextView(stat.count + " lần", 13, "#757575");
            row.addView(tvCount);

            layoutPatternHours.addView(row);
        }
    }

    // ============================================================
    // TÍNH NĂNG 7: Drink Type Breakdown
    // ============================================================
    private void loadDrinkTypes() {
        if (layoutDrinkTypes == null) return;
        layoutDrinkTypes.removeAllViews();

        List<DrinkTypeStat> breakdown = db.getDrinkTypeBreakdown();

        if (breakdown.isEmpty()) {
            layoutDrinkTypes.addView(makeTextView("Chưa có dữ liệu.", 14, "#9E9E9E"));
            return;
        }

        for (DrinkTypeStat stat : breakdown) {
            // "☕ Cà phê  —  800ml uống  →  400ml tính  (3 lần)"
            String text = stat.type.getIcon() + "  " + stat.type.getDisplayName()
                    + "   " + stat.totalAmount + "ml uống"
                    + " → " + stat.totalEffective + "ml tính"
                    + "   (" + stat.count + " lần)";

            TextView tv = makeTextView(text, 14, "#424242");
            tv.setPadding(0, 8, 0, 8);

            // Nếu hệ số < 100% → màu cam để user nhận thấy
            if (stat.type.getHydrationFactor() < 1.0f) {
                tv.setTextColor(Color.parseColor("#E65100"));
            }
            layoutDrinkTypes.addView(tv);

            // Divider
            View div = new View(this);
            div.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            div.setBackgroundColor(Color.parseColor("#EEEEEE"));
            layoutDrinkTypes.addView(div);
        }
    }

    // ============================================================
    // TÍNH NĂNG 6: Challenge List
    // ============================================================
    private void loadChallenges() {
        if (layoutChallenges == null) return;
        layoutChallenges.removeAllViews();

        int goal = prefs.getDailyGoal();
        List<ChallengeManager.Challenge> challenges =
                ChallengeManager.getAllChallenges(db, goal);

        int done = ChallengeManager.countCompleted(challenges);

        // Header
        TextView header = makeTextView(done + " / " + challenges.size() + " thử thách hoàn thành", 14, "#757575");
        header.setPadding(0, 0, 0, dp(8));
        layoutChallenges.addView(header);

        for (ChallengeManager.Challenge ch : challenges) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));
            row.setBackgroundColor(ch.completed
                    ? Color.parseColor("#E8F5E9")  // xanh nhạt khi done
                    : Color.parseColor("#FAFAFA")); // xám nhạt khi chưa

            // Icon
            TextView tvIcon = makeTextView(ch.icon, 22, "#000000");
            tvIcon.setMinWidth(dp(40));
            row.addView(tvIcon);

            // Nội dung
            LinearLayout colText = new LinearLayout(this);
            colText.setOrientation(LinearLayout.VERTICAL);
            colText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvTitle = makeTextView(ch.title, 15, ch.completed ? "#2E7D32" : "#424242");
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            colText.addView(tvTitle);

            String subtitle = ch.completed ? "✓ " + ch.rewardText : ch.description;
            TextView tvDesc = makeTextView(subtitle, 12, ch.completed ? "#43A047" : "#9E9E9E");
            colText.addView(tvDesc);

            row.addView(colText);

            // Check mark
            if (ch.completed) {
                TextView tvCheck = makeTextView("✓", 18, "#43A047");
                row.addView(tvCheck);
            }

            // Margin giữa các row
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, dp(6));
            row.setLayoutParams(rowParams);

            layoutChallenges.addView(row);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================
    private TextView makeTextView(String text, int sizeSp, String colorHex) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(Color.parseColor(colorHex));
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return tv;
    }

    private int dp(int value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}