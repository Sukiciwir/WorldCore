package dev.worldcore.achievement.listeners;

import dev.worldcore.achievement.AchievementCategory;
import dev.worldcore.achievement.AchievementManager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

public final class CombatListener implements Listener {

    private final AchievementManager achievements;

    public CombatListener(@NotNull AchievementManager achievements) {
        this.achievements = achievements;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Add 1 progress to Combat category
        achievements.addProgress(killer.getUniqueId(), AchievementCategory.COMBAT, 1);
        
        // We could filter by EntityType here if we had specific achievements 
        // mapped to specific mobs, but for the MVP we just increment COMBAT.
    }
}
