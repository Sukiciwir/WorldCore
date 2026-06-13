package dev.worldcore.achievement;

import org.jetbrains.annotations.NotNull;

/**
 * Mutable runtime progress object for a specific player + achievement.
 */
public final class AchievementProgress {

    private final String achievementId;
    private long   progress;
    private boolean unlocked;
    private long   unlockedAt;

    public AchievementProgress(@NotNull String achievementId, long progress,
                               boolean unlocked, long unlockedAt) {
        this.achievementId = achievementId;
        this.progress      = progress;
        this.unlocked      = unlocked;
        this.unlockedAt    = unlockedAt;
    }

    public static @NotNull AchievementProgress fresh(@NotNull String id) {
        return new AchievementProgress(id, 0, false, 0);
    }

    /** @return New progress value after adding delta. */
    public long addProgress(long delta) {
        progress += delta;
        return progress;
    }

    public void markUnlocked() {
        this.unlocked   = true;
        this.unlockedAt = System.currentTimeMillis();
    }

    @NotNull public String  getId()         { return achievementId; }
    public long             getProgress()   { return progress; }
    public boolean          isUnlocked()    { return unlocked; }
    public long             getUnlockedAt() { return unlockedAt; }
    public void             setProgress(long v) { this.progress = v; }
}
