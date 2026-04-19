package com.hydration.app.models;

/**
 * MODEL: Đại diện cho một lần uống nước / đồ uống.
 *
 * Thay đổi so với bản gốc:
 *   + drinkType : loại đồ uống (DrinkType enum)
 *   + effectiveAmount : ml thực sự tính vào goal (= amount × hydrationFactor)
 */
public class WaterEntry {

    private int       id;
    private int       amount;          // ml người dùng thực sự uống
    private int       effectiveAmount; // ml tính vào goal (sau hệ số)
    private String    timestamp;       // "2024-01-15 14:30:00"
    private String    note;
    private DrinkType drinkType;       // loại đồ uống

    // Constructor mới khi INSERT (có DrinkType)
    public WaterEntry(int amount, String timestamp, DrinkType drinkType) {
        this.amount          = amount;
        this.timestamp       = timestamp;
        this.drinkType       = drinkType;
        this.effectiveAmount = drinkType.calculateEffectiveMl(amount);
        this.note            = "";
    }

    // Constructor tương thích ngược (WATER mặc định)
    public WaterEntry(int amount, String timestamp) {
        this(amount, timestamp, DrinkType.WATER);
    }

    // Constructor khi QUERY từ DB (đầy đủ)
    public WaterEntry(int id, int amount, int effectiveAmount,
                      String timestamp, String note, DrinkType drinkType) {
        this.id              = id;
        this.amount          = amount;
        this.effectiveAmount = effectiveAmount;
        this.timestamp       = timestamp;
        this.note            = note;
        this.drinkType       = drinkType;
    }

    // Constructor tương thích ngược với DB cũ (chưa có drinkType)
    public WaterEntry(int id, int amount, String timestamp, String note) {
        this(id, amount, amount, timestamp, note, DrinkType.WATER);
    }

    // Getters
    public int       getId()              { return id; }
    public int       getAmount()          { return amount; }
    public int       getEffectiveAmount() { return effectiveAmount; }
    public String    getTimestamp()       { return timestamp; }
    public String    getNote()            { return note; }
    public DrinkType getDrinkType()       { return drinkType; }

    // Setters
    public void setId(int id)         { this.id = id; }
    public void setNote(String note)  { this.note = note; }

    /** Lấy giờ:phút từ timestamp — "14:30" */
    public String getFormattedTime() {
        if (timestamp != null && timestamp.length() >= 16) return timestamp.substring(11, 16);
        return "";
    }

    /** Lấy ngày — "15/01/2024" */
    public String getFormattedDate() {
        if (timestamp != null && timestamp.length() >= 10) {
            String[] parts = timestamp.substring(0, 10).split("-");
            if (parts.length == 3) return parts[2] + "/" + parts[1] + "/" + parts[0];
        }
        return timestamp;
    }

    /** Hiển thị trong RecyclerView: "☕ 200ml (100ml hiệu quả)" */
    public String getDisplayLabel() {
        if (drinkType == DrinkType.WATER) return amount + " ml";
        return drinkType.getIcon() + " " + amount + "ml → " + effectiveAmount + "ml";
    }

    @Override
    public String toString() {
        return "WaterEntry{id=" + id + ", type=" + drinkType.name()
                + ", amount=" + amount + "ml, effective=" + effectiveAmount + "ml}";
    }
}