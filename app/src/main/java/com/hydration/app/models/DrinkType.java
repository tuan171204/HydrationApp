package com.hydration.app.models;

/**
 * DRINK TYPE - Tính năng 7: Custom Drink Types
 *
 * Mỗi loại đồ uống có hệ số hydrat hóa khác nhau.
 * Ví dụ: cà phê chỉ tính 50% vì có caffeine gây lợi tiểu nhẹ.
 *
 * effectiveMl = inputMl × hydrationFactor
 */
public enum DrinkType {

    //         Tên hiển thị        Icon  Hệ số hydrat hóa
    WATER       ("Nước lọc",       "💧",  1.00f),
    TEA         ("Trà",            "🍵",  0.90f),
    COFFEE      ("Cà phê",         "☕",  0.50f),
    JUICE       ("Nước trái cây",  "🥤",  0.85f),
    MILK        ("Sữa",            "🥛",  0.90f),
    SPORTS      ("Nước thể thao",  "🏃",  1.00f),
    SPARKLING   ("Nước có gas",    "🫧",  0.90f);

    private final String displayName;
    private final String icon;
    private final float  hydrationFactor; // 0.0 – 1.0

    DrinkType(String displayName, String icon, float hydrationFactor) {
        this.displayName     = displayName;
        this.icon            = icon;
        this.hydrationFactor = hydrationFactor;
    }

    public String getDisplayName()    { return displayName; }
    public String getIcon()           { return icon; }
    public float  getHydrationFactor(){ return hydrationFactor; }

    /**
     * Tính lượng nước thực sự tính vào mục tiêu (ml hiệu quả).
     * @param inputMl Lượng đồ uống thực sự (ml)
     * @return Lượng nước được tính vào daily goal
     */
    public int calculateEffectiveMl(int inputMl) {
        return Math.round(inputMl * hydrationFactor);
    }

    /**
     * Mô tả ngắn giải thích hệ số cho user.
     */
    public String getFactorDescription() {
        int pct = Math.round(hydrationFactor * 100);
        if (pct == 100) return "Tính 100% — lý tưởng!";
        return "Chỉ tính " + pct + "% lượng bạn uống";
    }

    /**
     * Tìm DrinkType theo tên (dùng khi load từ DB).
     */
    public static DrinkType fromName(String name) {
        for (DrinkType t : values()) {
            if (t.name().equals(name)) return t;
        }
        return WATER; // fallback
    }

    /** Mảng tên hiển thị để dùng trong Spinner / Dialog */
    public static String[] getDisplayNames() {
        DrinkType[] types = values();
        String[] names    = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].icon + " " + types[i].displayName;
        }
        return names;
    }
}