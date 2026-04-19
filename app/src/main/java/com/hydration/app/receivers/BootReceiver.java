package com.hydration.app.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hydration.app.database.DatabaseHelper;
import com.hydration.app.utils.GoalCalculator;
import com.hydration.app.utils.NotificationHelper;
import com.hydration.app.utils.PreferenceManager;

/**
 * BOOT RECEIVER
 *
 * AlarmManager bị xóa khi thiết bị tắt nguồn.
 * BootReceiver lắng nghe sự kiện BOOT_COMPLETED (máy khởi động xong)
 * và thiết lập lại alarm.
 *
 * Cần permission: RECEIVE_BOOT_COMPLETED trong Manifest
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        Log.d(TAG, "Boot completed - restoring reminder");

        PreferenceManager prefs = new PreferenceManager(context);
        if (!prefs.isReminderEnabled()) return;

        // Lấy trạng thái hiện tại
        DatabaseHelper db = DatabaseHelper.getInstance(context);
        int consumed      = db.getTodayTotal();
        int goal          = prefs.getDailyGoal();

        // Tính interval dựa trên dữ liệu hiện có
        int currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int interval    = GoalCalculator.calculateReminderInterval(consumed, goal, currentHour);

        if (interval > 0) {
            NotificationHelper.scheduleNextReminder(context, interval);
            Log.d(TAG, "Reminder restored: next in " + interval + " min");
        }
    }
}
