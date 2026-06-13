package dev.worldcore.achievement.listeners;

import dev.worldcore.achievement.AchievementCategory;
import dev.worldcore.achievement.AchievementManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Periodically increments playtime progress for online players.
 */
public final class PlaytimeListener {

    private final AchievementManager achievements;

    public PlaytimeListener(@NotNull Plugin plugin, @NotNull AchievementManager achievements) {
        this.achievements = achievements;

        // Run every minute (1200 ticks). Adds 60,000 milliseconds to playtime progress.
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::incrementPlaytime, 1200L, 1200L);
    }

    private void incrementPlaytime() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            achievements.addProgress(player.getUniqueId(), AchievementCategory.PLAYTIME, 60000L);
        }
    }
}
