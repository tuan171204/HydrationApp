package com.hydration.app.utils;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.hydration.app.R;
import com.hydration.app.activities.MainActivity;
import com.hydration.app.receivers.ReminderReceiver;

/**
 * NOTIFICATION HELPER
 *
 * Quản lý toàn bộ logic notification:
 *   1. Tạo Notification Channel (bắt buộc từ Android 8.0+)
 *   2. Gửi notification ngay lập tức
 *   3. Lên lịch nhắc nhở bằng AlarmManager (không hard-code giờ)
 *
 * Flow:
 *   AlarmManager → (trigger sau X phút) → ReminderReceiver → NotificationHelper.showReminder()
 */
public class NotificationHelper {

    private static final String TAG              = "NotificationHelper";
    public static final String CHANNEL_ID        = "hydration_reminder";
    public static final String CHANNEL_NAME      = "Nhắc uống nước";
    public static final int    NOTIFICATION_ID   = 1001;
    public static final int    ALARM_REQUEST_CODE = 2001;

    /**
     * Khởi tạo Notification Channel
     * Bắt buộc gọi 1 lần khi app khởi động (Android 8.0+)
     *
     * Nếu không có channel → notification sẽ không hiển thị
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT  // hiển thị + có âm thanh
            );
            channel.setDescription("Nhắc bạn uống đủ nước mỗi ngày");

            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    /**
     * Hiển thị notification nhắc uống nước ngay lập tức
     *
     * @param context   Context
     * @param message   Nội dung thông báo (có thể chứa thông tin thời tiết)
     * @param consumed  Đã uống (ml)
     * @param goal      Mục tiêu (ml)
     */
    public static void showReminder(Context context, String message, int consumed, int goal) {
        // Tạo Intent để khi bấm notification → mở MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo nội dung chi tiết
        String title = "💧 Đến giờ uống nước rồi!";
        String subText = String.format("Đã uống: %dml / %dml", consumed, goal);

        // Build notification với NotificationCompat (tương thích nhiều Android version)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_water_drop)
                .setContentTitle(title)
                .setContentText(message)
                .setSubText(subText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message + "\n" + subText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)  // tự đóng khi bấm vào
                .setDefaults(NotificationCompat.DEFAULT_SOUND);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "Notification shown: " + message);
        }
    }

    /**
     * Lên lịch nhắc nhở tiếp theo bằng AlarmManager
     *
     * AlarmManager là cách Android hỗ trợ chạy code sau X milliseconds,
     * kể cả khi app đóng. Khi alarm trigger → gửi broadcast đến ReminderReceiver
     *
     * @param context          Context
     * @param delayMinutes     Số phút sau khi nhắc (tính từ GoalCalculator)
     */
    public static void scheduleNextReminder(Context context, int delayMinutes) {
        if (delayMinutes <= 0) {
            Log.d(TAG, "No reminder scheduled (goal reached or night time)");
            return;
        }

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Intent để AlarmManager biết cần broadcast đến đâu
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction("com.hydration.app.REMINDER");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tính thời điểm trigger
        long triggerTime = System.currentTimeMillis() + (delayMinutes * 60 * 1000L);

        // Set alarm
        // setExactAndAllowWhileIdle: chính xác, hoạt động cả khi Doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }

        Log.d(TAG, "Next reminder scheduled in " + delayMinutes + " minutes");
    }

    /**
     * Hủy alarm đã lên lịch
     * Dùng khi user đã đạt mục tiêu hoặc vào ban đêm
     */
    public static void cancelReminder(Context context) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Reminder cancelled");
    }

    /**
     * Tạo nội dung notification thông minh dựa trên context
     */
    public static String buildSmartMessage(double temperature, int remaining) {
        String tempNote;
        if (temperature >= 35)      tempNote = "Trời rất nóng (" + (int)temperature + "°C)";
        else if (temperature >= 30) tempNote = "Trời nóng (" + (int)temperature + "°C)";
        else if (temperature >= 25) tempNote = "Trời ấm (" + (int)temperature + "°C)";
        else                        tempNote = "Hôm nay";

        if (remaining > 1000) {
            return tempNote + ", bạn cần uống thêm " + remaining + "ml nữa. Hãy uống ngay!";
        } else if (remaining > 0) {
            return "Sắp đạt mục tiêu rồi! Còn " + remaining + "ml nữa thôi.";
        } else {
            return "Bạn đã đạt mục tiêu hôm nay! Tuyệt vời! 🎉";
        }
    }
}
