package dev.worldcore.achievement;

import org.jetbrains.annotations.NotNull;

/** Category grouping for achievements. */
public enum AchievementCategory {
    COMBAT, ECONOMY, CRATES, AUCTION, FISHING, QUEST, PLAYTIME,
    SHOPS, EXPEDITION, HALL_OF_FAME, DISCOVERY, TOWNY, SEASONAL, EVENT, CUSTOM;

    @NotNull
    public static AchievementCategory fromString(@NotNull String s) {
        try { return valueOf(s.toUpperCase()); }
        catch (Exception e) { return CUSTOM; }
    }
}
