package dev.worldcore.hooks;

import dev.discoverycore.DiscoveryCorePlugin;
import dev.worldcore.achievement.AchievementCategory;
import dev.worldcore.achievement.AchievementManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Hook into DiscoveryCore (which contains HallOfFame via HofManager).
 */
public final class DiscoveryCoreHook {

    private final Logger             log;
    private final AchievementManager achievements;
    private boolean                  ready = false;
    private DiscoveryCorePlugin      discoveryCore;

    public DiscoveryCoreHook(@NotNull Logger log, @NotNull AchievementManager achievements) {
        this.log          = log;
        this.achievements = achievements;
        init();
    }

    private void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("DiscoveryCore");
        if (plugin instanceof DiscoveryCorePlugin dc) {
            discoveryCore = dc;
            ready = true;
            log.info("[Hooks/DiscoveryCore] Hook active (HallOfFame access).");
        }
    }

    public boolean isReady() { return ready; }

    /**
     * Checks if a player has reached a specific top rank in HallOfFame.
     * This could be called periodically or on specific events.
     */
    public void checkHallOfFameRanks(@NotNull Player player) {
        if (!ready || discoveryCore.getHofManager() == null) return;
        
        // In a real implementation, we would query discoveryCore.getHofManager()
        // for the player's best rank across categories and update the achievement manager.
        // For now, we simulate the hook being present.
        
        // Example: If player is top 10
        // achievements.triggerOneShot(player.getUniqueId(), AchievementCategory.HALL_OF_FAME);
    }
}
