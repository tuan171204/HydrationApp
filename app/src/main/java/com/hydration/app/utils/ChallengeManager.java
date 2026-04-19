package com.hydration.app.utils;

import com.hydration.app.database.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * CHALLENGE MANAGER — Tính năng 6: Mini Challenge System
 *
 * Định nghĩa danh sách Challenge và kiểm tra xem user đã đạt chưa.
 * Không cần backend — toàn bộ logic dựa vào SQLite local.
 *
 * Mỗi Challenge có:
 *   - Tiêu đề
 *   - Mô tả điều kiện
 *   - Icon
 *   - isCompleted() → truy vấn DB để kiểm tra
 */
public class ChallengeManager {

    /**
     * Data class đại diện cho 1 challenge.
     */
    public static class Challenge {
        public final String title;
        public final String description;
        public final String icon;
        public final boolean completed;
        public final String rewardText; // hiển thị khi đạt

        public Challenge(String title, String description, String icon,
                         boolean completed, String rewardText) {
            this.title       = title;
            this.description = description;
            this.icon        = icon;
            this.completed   = completed;
            this.rewardText  = rewardText;
        }
    }

    /**
     * Tạo và kiểm tra toàn bộ danh sách challenge.
     * Gọi hàm này để lấy danh sách cập nhật mới nhất.
     *
     * @param db   DatabaseHelper instance
     * @param goal Mục tiêu ml/ngày hiện tại
     * @return Danh sách challenges (đã check completed hay chưa)
     */
    public static List<Challenge> getAllChallenges(DatabaseHelper db, int goal) {
        List<Challenge> list = new ArrayList<>();

        int streak  = db.getCurrentStreak(goal);
        int today   = db.getTodayTotal();
        int count   = db.getTodayEntryCount();

        // === Challenge 1: Ngày đầu tiên ===
        list.add(new Challenge(
                "Khởi động!",
                "Uống đủ nước ngày đầu tiên",
                "🌟",
                streak >= 1,
                "Bạn đã bắt đầu hành trình! +10 điểm"
        ));

        // === Challenge 2: 3 ngày liên tiếp ===
        list.add(new Challenge(
                "Bền bỉ 3 ngày",
                "Uống đủ nước 3 ngày liên tiếp",
                "🔥",
                streak >= 3,
                "Thói quen đang hình thành! +30 điểm"
        ));

        // === Challenge 3: 7 ngày liên tiếp ===
        list.add(new Challenge(
                "Tuần hoàn hảo",
                "Uống đủ nước 7 ngày liên tiếp",
                "💎",
                streak >= 7,
                "Bạn là người hùng hydration! +100 điểm"
        ));

        // === Challenge 4: Đủ nước trước 18h ===
        boolean before6pm = db.isGoalReachedBefore(18, goal);
        list.add(new Challenge(
                "Sáng suốt sớm",
                "Uống đủ mục tiêu trước 18 giờ",
                "⚡",
                before6pm,
                "Kỷ luật tuyệt vời! +20 điểm"
        ));

        // === Challenge 5: Uống ≥ 8 lần trong ngày ===
        list.add(new Challenge(
                "Siêu đều đặn",
                "Uống nước ít nhất 8 lần trong 1 ngày",
                "🎯",
                count >= 8,
                "Cơ thể bạn rất cảm ơn! +25 điểm"
        ));

        // === Challenge 6: Uống 150% mục tiêu (ngày nóng) ===
        list.add(new Challenge(
                "Ngày nóng bỏng",
                "Uống ≥ 150% mục tiêu trong 1 ngày",
                "🌡️",
                today >= (int)(goal * 1.5),
                "Đối phó thời tiết xuất sắc! +15 điểm"
        ));

        return list;
    }

    /**
     * Đếm số challenge đã hoàn thành.
     */
    public static int countCompleted(List<Challenge> challenges) {
        int count = 0;
        for (Challenge c : challenges) if (c.completed) count++;
        return count;
    }

    /**
     * Lấy challenge gần hoàn thành nhất (để hiển thị "gợi ý tiếp theo").
     * Logic: chọn challenge chưa done mà điều kiện gần thỏa nhất.
     * Đơn giản: lấy challenge chưa done đầu tiên.
     */
    public static Challenge getNextChallenge(List<Challenge> challenges) {
        for (Challenge c : challenges) {
            if (!c.completed) return c;
        }
        return null; // tất cả đã done
    }
}