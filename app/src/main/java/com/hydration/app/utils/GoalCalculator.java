package com.hydration.app.utils;

import java.util.Calendar;

/**
 * GOAL CALCULATOR
 *
 * Tính toán mục tiêu nước, interval nhắc thông minh,
 * Smart Suggestion theo ngữ cảnh, và Hydration Score.
 */
public class GoalCalculator {

    // ===== GOAL FORMULA =====
    private static final int    ML_PER_KG         = 35;
    private static final int    HEAT_BONUS_PER_DEG = 50;
    private static final double HEAT_THRESHOLD     = 25.0;
    private static final int    HUMIDITY_BONUS     = 200;
    private static final int    HUMIDITY_THRESHOLD = 70;
    private static final int    MIN_GOAL           = 1500;
    private static final int    MAX_GOAL           = 5000;

    public static int calculateDailyGoal(float weightKg, double tempCelsius, int humidity) {
        int baseGoal  = (int) (weightKg * ML_PER_KG);
        int heatBonus = tempCelsius > HEAT_THRESHOLD
                ? (int) ((tempCelsius - HEAT_THRESHOLD) * HEAT_BONUS_PER_DEG) : 0;
        int humidBonus = humidity > HUMIDITY_THRESHOLD ? HUMIDITY_BONUS : 0;
        return Math.max(MIN_GOAL, Math.min(baseGoal + heatBonus + humidBonus, MAX_GOAL));
    }

    public static int calculateDailyGoal(float weightKg) {
        return calculateDailyGoal(weightKg, 25.0, 50);
    }

    // ===== TÍNH NĂNG 1: ADAPTIVE REMINDER =====
    /**
     * Interval nhắc thông minh — thích nghi theo tiến độ.
     *
     *   < 25% goal  → 30 phút  (chậm lắm, nhắc dày)
     *   25–50%      → 45 phút
     *   50–75%      → 60 phút  (đang ổn)
     *   75–100%     → 90 phút  (gần đủ, thưa hơn)
     *   ≥ 100%      → -1       (không nhắc)
     *
     * Catch-up pressure: qua 14h mà chưa đến 50% → giảm interval thêm 30%.
     */
    public static int calculateReminderInterval(int consumed, int goal, int currentHour) {
        if (currentHour >= 22 || currentHour < 6) return -1;
        if (consumed >= goal) return -1;

        double percent = goal > 0 ? (double) consumed / goal * 100.0 : 0;

        int baseInterval;
        if      (percent < 25) baseInterval = 30;
        else if (percent < 50) baseInterval = 45;
        else if (percent < 75) baseInterval = 60;
        else                   baseInterval = 90;

        // Catch-up pressure: đã chiều mà vẫn tụt hậu
        if (currentHour >= 14 && percent < 50) {
            baseInterval = (int) (baseInterval * 0.7);
        }

        // Đảm bảo kịp uống đủ trước 22h
        int remaining   = goal - consumed;
        int hoursLeft   = Math.max(1, 22 - currentHour);
        int timesNeeded = (int) Math.ceil(remaining / 250.0);
        if (timesNeeded > 0) {
            int urgentInterval = (hoursLeft * 60) / timesNeeded;
            baseInterval = Math.min(baseInterval, urgentInterval);
        }

        return Math.max(15, Math.min(baseInterval, 90));
    }

    // ===== TÍNH NĂNG 3: HYDRATION SCORE =====
    /**
     * Tính Hydration Score (0–100).
     *
     *   baseScore (80đ) = tỷ lệ consumed/goal
     *   timeBonus (20đ) = số lần uống / 8 lần kỳ vọng (uống đều = thưởng điểm)
     *
     * @param consumed   Tổng ml đã uống hôm nay
     * @param goal       Mục tiêu ml
     * @param entryCount Số lần uống trong ngày
     */
    public static int calculateHydrationScore(int consumed, int goal, int entryCount) {
        if (goal <= 0) return 0;
        double ratio   = Math.min(1.0, (double) consumed / goal);
        int baseScore  = (int) (ratio * 80);
        int timeBonus  = 0;
        if (consumed > 0) {
            double evenness = Math.min(1.0, (double) entryCount / 8.0);
            timeBonus = (int) (evenness * 20);
        }
        return Math.min(100, baseScore + timeBonus);
    }

    public static String getScoreLabel(int score) {
        if (score >= 90) return "Xuất sắc 🏆";
        if (score >= 70) return "Tốt 👍";
        if (score >= 50) return "Trung bình ⚠️";
        if (score >= 30) return "Kém 😟";
        return "Rất kém 🚨";
    }

    // ===== TÍNH NĂNG 4: SMART SUGGESTION =====
    /**
     * Gợi ý ngữ cảnh thông minh — if/else theo giờ + tiến độ + thời tiết.
     * Không cần AI thật.
     */
    public static String getSmartSuggestion(int consumed, int goal, double temperature) {
        int hour      = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int remaining = Math.max(0, goal - consumed);
        double percent = goal > 0 ? (double) consumed / goal * 100.0 : 0;

        // Sáng sớm 6–9h
        if (hour >= 6 && hour < 9) {
            if (consumed == 0)
                return "☀️ Chào buổi sáng! Uống 1 ly nước ngay để khởi động cơ thể nhé.";
            return "☀️ Khởi đầu tốt! Đã uống " + consumed + "ml. Tiếp tục nhé!";
        }
        // Giữa sáng 9–12h
        if (hour < 12) {
            if (percent < 25)
                return "🌿 Đã " + hour + "h mà mới uống " + consumed + "ml. Uống thêm ngay!";
            return "👍 Đang đúng hướng. Còn " + remaining + "ml nữa để đạt mục tiêu.";
        }
        // Bữa trưa 12–14h
        if (hour < 14) {
            if (temperature >= 32)
                return "🌞 Trời nóng " + (int)temperature + "°C! Uống nhiều hơn trong bữa trưa nhé.";
            if (percent < 40)
                return "🍱 Trưa rồi! Mới uống " + (int)percent + "% mục tiêu. Uống 1 ly trước khi ăn nhé.";
            return "😊 Nhớ uống nước trong và sau bữa trưa nhé.";
        }
        // Chiều 14–18h
        if (hour < 18) {
            if (percent < 50)
                return "⚡ Đã chiều mà mới " + (int)percent + "% mục tiêu! Cần uống " + remaining + "ml nữa.";
            if (temperature >= 30)
                return "☀️ Trời vẫn nóng (" + (int)temperature + "°C). Uống đều mỗi 45 phút nhé.";
            return "🕑 Buổi chiều — còn " + remaining + "ml là xong mục tiêu hôm nay!";
        }
        // Chiều tối 18–20h
        if (hour < 20) {
            if (remaining > 500)
                return "⚠️ Sắp tối! Vẫn thiếu " + remaining + "ml. Hãy uống thêm trước 21h nhé.";
            if (remaining > 0)
                return "🎯 Gần xong! Chỉ còn " + remaining + "ml nữa thôi. Cố lên!";
            return "🎉 Đạt mục tiêu trước 20h. Thành tích xuất sắc!";
        }
        // Tối 20–22h
        if (hour < 22) {
            if (remaining > 0)
                return "🌙 Còn " + remaining + "ml. Uống 1 ly nhỏ trước khi ngủ nhé.";
            return "✨ Hoàn thành mục tiêu hôm nay! Ngủ ngon.";
        }
        // Đêm
        if (percent >= 100) return "🎉 Bạn đã đủ nước hôm nay! Tuyệt vời!";
        return "💧 Còn " + remaining + "ml nữa để đạt mục tiêu hôm nay.";
    }

    // ===== CÁC HÀM GIỮ NGUYÊN =====

    public static String getMotivationMessage(int consumed, int goal) {
        if (goal <= 0) return "Hãy thiết lập mục tiêu của bạn!";
        double percent = (double) consumed / goal * 100;
        if (percent <= 0)  return "Hãy bắt đầu uống nước nào! 💧";
        if (percent < 25)  return "Mới bắt đầu, cố lên! 🌱";
        if (percent < 50)  return "Đang tiến bộ tốt! 👍";
        if (percent < 75)  return "Hơn nửa đường rồi! 💪";
        if (percent < 100) return "Gần đến đích rồi! 🎯";
        return "Tuyệt vời! Đã đủ nước hôm nay! 🎉";
    }

    public static String getProgressColorHex(int consumed, int goal) {
        if (goal <= 0) return "#E0E0E0";
        double percent = (double) consumed / goal * 100;
        if (percent < 30) return "#FF5252";
        if (percent < 70) return "#FFD740";
        return "#4CAF50";
    }
}