package dev.worldcore.achievement.listeners;

import dev.worldcore.achievement.AchievementCategory;
import dev.worldcore.achievement.AchievementManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically increments playtime progress for online players using real time.
 */
public final class PlaytimeListener implements Listener {

    private final AchievementManager achievements;
    private final Map<UUID, Long> joinTimes = new ConcurrentHashMap<>();

    public PlaytimeListener(@NotNull Plugin plugin, @NotNull AchievementManager achievements) {
        this.achievements = achievements;
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Run every minute to update progress
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::incrementPlaytime, 1200L, 1200L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        joinTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        joinTimes.remove(event.getPlayer().getUniqueId());
    }

    private void incrementPlaytime() {
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            Long last = joinTimes.get(uuid);
            if (last != null) {
                long delta = now - last;
                achievements.addProgress(uuid, AchievementCategory.PLAYTIME, delta);
            }
            joinTimes.put(uuid, now);
        }
    }
}
