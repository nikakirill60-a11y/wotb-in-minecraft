package com.wotbminecraft.entity;

/**
 * Типы танковых снарядов в стиле World of Tanks Blitz.
 */
public enum ShellType {
    /**
     * Бронебойный (ББ / AP) — базовый снаряд с высокой скоростью полёта,
     * хорошим пробитием, но возможным рикошетом при острых углах. Не разрушает блоки.
     */
    AP(0, "ББ", "AP", 165, 45, 3.6F, 70.0F, false, 0xE0E0E0),

    /**
     * Кумулятивный (КС / HEAT) — «голдовый» снаряд с повышенным бронепробитием,
     * не теряет пробитие с расстоянием, рикошетит только при экстремальных углах. Не разрушает блоки.
     */
    HEAT(1, "КС", "HEAT", 235, 38, 3.1F, 82.0F, false, 0xFFD700),

    /**
     * Осколочно-фугасный (ОФ / HE) — снаряд с низким пробитием, но высоким уроном при пробитии,
     * осколочным уроном по площади даже без пробития брони и разрушением блоков мира.
     */
    HE(2, "ОФ", "HE", 45, 60, 2.7F, 90.0F, true, 0xFF6B2B);

    private final int id;
    private final String shortNameRu;
    private final String shortNameEn;
    private final int penetrationMm;
    private final int averageDamage;
    private final float speed;
    private final float ricochetAngleDeg;
    private final boolean breaksBlocks;
    private final int hudColor;

    ShellType(int id, String shortNameRu, String shortNameEn, int penetrationMm,
              int averageDamage, float speed, float ricochetAngleDeg,
              boolean breaksBlocks, int hudColor) {
        this.id = id;
        this.shortNameRu = shortNameRu;
        this.shortNameEn = shortNameEn;
        this.penetrationMm = penetrationMm;
        this.averageDamage = averageDamage;
        this.speed = speed;
        this.ricochetAngleDeg = ricochetAngleDeg;
        this.breaksBlocks = breaksBlocks;
        this.hudColor = hudColor;
    }

    public int getId() {
        return id;
    }

    public String getShortNameRu() {
        return shortNameRu;
    }

    public String getShortNameEn() {
        return shortNameEn;
    }

    public int getPenetrationMm() {
        return penetrationMm;
    }

    public int getAverageDamage() {
        return averageDamage;
    }

    public float getSpeed() {
        return speed;
    }

    public float getRicochetAngleDeg() {
        return ricochetAngleDeg;
    }

    public boolean isBreaksBlocks() {
        return breaksBlocks;
    }

    public int getHudColor() {
        return hudColor;
    }

    public static ShellType byId(int id) {
        for (ShellType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return AP;
    }

    public ShellType next() {
        ShellType[] all = values();
        return all[(this.ordinal() + 1) % all.length];
    }
}
