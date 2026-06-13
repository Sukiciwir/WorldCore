package dev.worldcore.achievement;

/** Achievement type: INCREMENTAL needs a progress count; ONE_SHOT triggers once. */
public enum AchievementType {
    INCREMENTAL, ONE_SHOT;

    public static AchievementType fromString(String s) {
        try { return valueOf(s.toUpperCase()); }
        catch (Exception e) { return INCREMENTAL; }
    }
}
