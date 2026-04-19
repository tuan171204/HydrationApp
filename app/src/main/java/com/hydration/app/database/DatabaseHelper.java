package com.hydration.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.hydration.app.models.DrinkType;
import com.hydration.app.models.WaterEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * DATABASE HELPER — SQLiteOpenHelper
 *
 * Version 2: thêm cột drink_type, effective_amount
 * để hỗ trợ Custom Drink Types và Pattern Analysis.
 *
 * onUpgrade xử lý migration: thêm cột mới thay vì drop table.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG              = "DatabaseHelper";
    private static final String DATABASE_NAME    = "hydration.db";
    // Tăng version khi thay đổi schema → kích hoạt onUpgrade
    private static final int    DATABASE_VERSION = 2;

    // ===== TABLE water_entries =====
    public static final String TABLE_WATER        = "water_entries";
    public static final String COL_ID             = "id";
    public static final String COL_AMOUNT         = "amount";
    public static final String COL_EFFECTIVE      = "effective_amount"; // ml sau hệ số
    public static final String COL_TIMESTAMP      = "timestamp";
    public static final String COL_NOTE           = "note";
    public static final String COL_DRINK_TYPE     = "drink_type";       // tên enum DrinkType

    // Singleton
    private static DatabaseHelper instance;
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) instance = new DatabaseHelper(context.getApplicationContext());
        return instance;
    }
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_WATER + " ("
                + COL_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_AMOUNT      + " INTEGER NOT NULL, "
                + COL_EFFECTIVE   + " INTEGER NOT NULL DEFAULT 0, "
                + COL_TIMESTAMP   + " TEXT NOT NULL, "
                + COL_NOTE        + " TEXT DEFAULT '', "
                + COL_DRINK_TYPE  + " TEXT DEFAULT 'WATER'"
                + ");"
        );
        Log.d(TAG, "DB v2 created");
    }

    /**
     * Migration từ v1 → v2: thêm 2 cột mới, không drop dữ liệu cũ.
     * ALTER TABLE chỉ hỗ trợ ADD COLUMN trong SQLite.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_WATER
                        + " ADD COLUMN " + COL_EFFECTIVE + " INTEGER NOT NULL DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_WATER
                        + " ADD COLUMN " + COL_DRINK_TYPE + " TEXT DEFAULT 'WATER'");
                // Backfill: những record cũ chưa có effective_amount → copy từ amount
                db.execSQL("UPDATE " + TABLE_WATER
                        + " SET " + COL_EFFECTIVE + " = " + COL_AMOUNT
                        + " WHERE " + COL_EFFECTIVE + " = 0");
                Log.d(TAG, "Migrated DB from v1 to v2");
            } catch (Exception e) {
                // Nếu cột đã tồn tại → bỏ qua
                Log.w(TAG, "Migration note: " + e.getMessage());
            }
        }
    }

    // ============================================================
    // INSERT
    // ============================================================

    /** Thêm entry có DrinkType — đây là hàm chính */
    public long insertWaterEntry(int amount, DrinkType drinkType) {
        SQLiteDatabase db = this.getWritableDatabase();
        int effective = drinkType.calculateEffectiveMl(amount);

        ContentValues v = new ContentValues();
        v.put(COL_AMOUNT,     amount);
        v.put(COL_EFFECTIVE,  effective);
        v.put(COL_TIMESTAMP,  getCurrentTimestamp());
        v.put(COL_NOTE,       "");
        v.put(COL_DRINK_TYPE, drinkType.name());

        long id = db.insert(TABLE_WATER, null, v);
        Log.d(TAG, "Inserted: " + drinkType.name() + " " + amount + "ml → " + effective + "ml eff");
        return id;
    }

    /** Tương thích ngược — mặc định WATER */
    public long insertWaterEntry(int amount) {
        return insertWaterEntry(amount, DrinkType.WATER);
    }

    /** Với ghi chú */
    public long insertWaterEntry(int amount, DrinkType drinkType, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        int effective = drinkType.calculateEffectiveMl(amount);

        ContentValues v = new ContentValues();
        v.put(COL_AMOUNT,     amount);
        v.put(COL_EFFECTIVE,  effective);
        v.put(COL_TIMESTAMP,  getCurrentTimestamp());
        v.put(COL_NOTE,       note);
        v.put(COL_DRINK_TYPE, drinkType.name());

        return db.insert(TABLE_WATER, null, v);
    }

    // ============================================================
    // QUERY — cơ bản
    // ============================================================

    /**
     * Tổng EFFECTIVE ml hôm nay (dùng cho progress/goal).
     * Đây là số ml thực sự tính vào mục tiêu (sau hệ số hydrat hóa).
     */
    public int getTodayTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EFFECTIVE + ") FROM " + TABLE_WATER
                        + " WHERE date(" + COL_TIMESTAMP + ") = ?",
                new String[]{ getTodayDate() }
        );
        int total = 0;
        if (c.moveToFirst()) total = c.isNull(0) ? 0 : c.getInt(0);
        c.close();
        return total;
    }

    /** Số lần uống hôm nay (dùng để tính Hydration Score) */
    public int getTodayEntryCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_WATER
                        + " WHERE date(" + COL_TIMESTAMP + ") = ?",
                new String[]{ getTodayDate() }
        );
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public List<WaterEntry> getTodayEntries() {
        return getEntriesByDate(getTodayDate());
    }

    public List<WaterEntry> getEntriesByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<WaterEntry> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_WATER
                        + " WHERE date(" + COL_TIMESTAMP + ") = ?"
                        + " ORDER BY " + COL_TIMESTAMP + " DESC",
                new String[]{ date }
        );
        while (c.moveToNext()) list.add(rowToEntry(c));
        c.close();
        return list;
    }

    public List<WaterEntry> getAllEntries() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<WaterEntry> list = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_WATER
                        + " ORDER BY " + COL_TIMESTAMP + " DESC LIMIT 100",
                null
        );
        while (c.moveToNext()) list.add(rowToEntry(c));
        c.close();
        return list;
    }

    // ============================================================
    // QUERY — Stats & Chart
    // ============================================================

    public List<DailyStat> getWeeklyStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<DailyStat> stats = new ArrayList<>();
        // Dùng effective_amount cho chart để phản ánh đúng hydrat hóa thực tế
        Cursor c = db.rawQuery(
                "SELECT date(" + COL_TIMESTAMP + ") as day, SUM(" + COL_EFFECTIVE + ") as total"
                        + " FROM " + TABLE_WATER
                        + " WHERE " + COL_TIMESTAMP + " >= date('now', '-6 days')"
                        + " GROUP BY day ORDER BY day ASC",
                null
        );
        while (c.moveToNext()) stats.add(new DailyStat(c.getString(0), c.getInt(1)));
        c.close();
        return stats;
    }

    // ============================================================
    // QUERY — Tính năng 2: Drinking Pattern Analysis
    // ============================================================

    /**
     * Phân tích khung giờ uống nước nhiều nhất.
     *
     * SQL nhóm theo giờ, đếm số lần uống trong mỗi giờ.
     * @return List<HourStat> sắp xếp từ giờ nhiều nhất → ít nhất
     */
    public List<HourStat> getDrinkingPatternByHour() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<HourStat> result = new ArrayList<>();

        // strftime('%H', timestamp) lấy giờ dạng "09", "14",...
        Cursor c = db.rawQuery(
                "SELECT CAST(strftime('%H', " + COL_TIMESTAMP + ") AS INTEGER) as hr, "
                        + "COUNT(*) as cnt "
                        + "FROM " + TABLE_WATER
                        + " GROUP BY hr ORDER BY cnt DESC LIMIT 5",
                null
        );
        while (c.moveToNext()) {
            result.add(new HourStat(c.getInt(0), c.getInt(1)));
        }
        c.close();
        return result;
    }

    /**
     * Tổng hợp lượng nước theo từng loại đồ uống (30 ngày).
     * Dùng cho biểu đồ breakdown trong StatsActivity.
     */
    public List<DrinkTypeStat> getDrinkTypeBreakdown() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<DrinkTypeStat> result = new ArrayList<>();
        Cursor c = db.rawQuery(
                "SELECT " + COL_DRINK_TYPE + ", SUM(" + COL_AMOUNT + ") as total_amount, "
                        + "SUM(" + COL_EFFECTIVE + ") as total_effective, COUNT(*) as cnt "
                        + "FROM " + TABLE_WATER
                        + " WHERE " + COL_TIMESTAMP + " >= date('now', '-29 days')"
                        + " GROUP BY " + COL_DRINK_TYPE
                        + " ORDER BY total_effective DESC",
                null
        );
        while (c.moveToNext()) {
            String typeName = c.getString(0);
            int totalAmount  = c.getInt(1);
            int totalEff     = c.getInt(2);
            int count        = c.getInt(3);
            result.add(new DrinkTypeStat(DrinkType.fromName(typeName), totalAmount, totalEff, count));
        }
        c.close();
        return result;
    }

    // ============================================================
    // QUERY — Tính năng 6: Challenge System
    // ============================================================

    /**
     * Số ngày đạt mục tiêu liên tiếp tính đến hôm nay.
     * Dùng cho Challenge "Uống đủ 3 ngày liên tiếp".
     * @param dailyGoal mục tiêu ml/ngày
     */
    public int getCurrentStreak(int dailyGoal) {
        SQLiteDatabase db = this.getReadableDatabase();
        int streak = 0;

        // Lấy tổng effective mỗi ngày, 30 ngày gần nhất, mới nhất trước
        Cursor c = db.rawQuery(
                "SELECT date(" + COL_TIMESTAMP + ") as day, SUM(" + COL_EFFECTIVE + ") as total "
                        + "FROM " + TABLE_WATER
                        + " WHERE " + COL_TIMESTAMP + " >= date('now', '-29 days')"
                        + " GROUP BY day ORDER BY day DESC",
                null
        );

        // Đếm streak: ngừng ngay khi gặp ngày không đạt goal
        while (c.moveToNext()) {
            int dayTotal = c.getInt(1);
            if (dayTotal >= dailyGoal) {
                streak++;
            } else {
                break; // chuỗi bị phá vỡ
            }
        }
        c.close();
        return streak;
    }

    /**
     * Kiểm tra hôm nay đã đạt mục tiêu trước giờ cutoff chưa.
     * Dùng cho Challenge "Uống đủ trước 18h".
     * @param cutoffHour giờ cần đạt trước (ví dụ: 18)
     * @param dailyGoal  mục tiêu ml
     */
    public boolean isGoalReachedBefore(int cutoffHour, int dailyGoal) {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = getTodayDate();
        // Tổng effective từ 0h đến cutoffHour hôm nay
        String cutoffTime = today + " " + String.format(Locale.US, "%02d:00:00", cutoffHour);
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_EFFECTIVE + ") FROM " + TABLE_WATER
                        + " WHERE date(" + COL_TIMESTAMP + ") = ?"
                        + " AND " + COL_TIMESTAMP + " < ?",
                new String[]{ today, cutoffTime }
        );
        int total = 0;
        if (c.moveToFirst()) total = c.isNull(0) ? 0 : c.getInt(0);
        c.close();
        return total >= dailyGoal;
    }

    // ============================================================
    // DELETE
    // ============================================================

    public int deleteEntry(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_WATER, COL_ID + " = ?", new String[]{ String.valueOf(id) });
    }

    public void clearAll() {
        getWritableDatabase().delete(TABLE_WATER, null, null);
    }

    // ============================================================
    // HELPER PRIVATE
    // ============================================================

    private WaterEntry rowToEntry(Cursor c) {
        int id        = c.getInt(c.getColumnIndexOrThrow(COL_ID));
        int amount    = c.getInt(c.getColumnIndexOrThrow(COL_AMOUNT));
        int effective = c.getInt(c.getColumnIndexOrThrow(COL_EFFECTIVE));
        String ts     = c.getString(c.getColumnIndexOrThrow(COL_TIMESTAMP));
        String note   = c.getString(c.getColumnIndexOrThrow(COL_NOTE));
        String type   = c.getString(c.getColumnIndexOrThrow(COL_DRINK_TYPE));
        return new WaterEntry(id, amount, effective, ts, note, DrinkType.fromName(type));
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // ============================================================
    // INNER DATA CLASSES
    // ============================================================

    public static class DailyStat {
        public final String date;
        public final int    total;
        public DailyStat(String date, int total) { this.date = date; this.total = total; }
        public String getShortLabel() {
            if (date != null && date.length() >= 10) {
                String[] p = date.split("-");
                if (p.length == 3) return Integer.parseInt(p[2]) + "/" + Integer.parseInt(p[1]);
            }
            return date;
        }
    }

    /** Tính năng 2: giờ uống nhiều nhất */
    public static class HourStat {
        public final int hour;  // 0-23
        public final int count; // số lần uống trong giờ này
        public HourStat(int hour, int count) { this.hour = hour; this.count = count; }
        /** "9h sáng", "14h chiều", "21h tối" */
        public String getLabel() {
            if (hour < 12) return hour + "h sáng";
            if (hour < 18) return hour + "h chiều";
            return hour + "h tối";
        }
    }

    /** Tính năng 7: breakdown theo loại đồ uống */
    public static class DrinkTypeStat {
        public final DrinkType type;
        public final int totalAmount;    // ml thực sự uống
        public final int totalEffective; // ml tính vào goal
        public final int count;          // số lần
        public DrinkTypeStat(DrinkType type, int totalAmount, int totalEffective, int count) {
            this.type          = type;
            this.totalAmount   = totalAmount;
            this.totalEffective= totalEffective;
            this.count         = count;
        }
    }
}