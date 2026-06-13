package dev.worldcore.achievement.listeners;

import dev.worldcore.achievement.AchievementCategory;
import dev.worldcore.achievement.AchievementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;

public final class ExternalEventListener {

    public ExternalEventListener(@NotNull Plugin plugin, @NotNull AchievementManager achievements) {
        tryRegister(plugin, achievements, "com.badbones69.crazycrates.api.events.PlayerPrizeEvent",
                AchievementCategory.CRATES);
        tryRegister(plugin, achievements, "me.realized.odailyquests.api.events.PlayerQuestCompleteEvent",
                AchievementCategory.QUEST);
        tryRegister(plugin, achievements, "com.oheers.fish.api.EMFCatchEvent",
                AchievementCategory.FISHING);
        tryRegister(plugin, achievements, "dev.nexustools.auctionhouse.api.events.AuctionListEvent",
                AchievementCategory.AUCTION);
        tryRegisterTowny(plugin, achievements);
    }

    private void tryRegister(@NotNull Plugin plugin, @NotNull AchievementManager achievements,
                              String className, AchievementCategory category) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(className);

            EventExecutor executor = (listener, event) -> {
                if (!eventClass.isInstance(event)) return;
                Player player = getPlayer(event);
                if (player != null) achievements.addProgress(player.getUniqueId(), category, 1);
            };

            plugin.getServer().getPluginManager().registerEvent(
                    eventClass, new org.bukkit.event.Listener() {}, EventPriority.MONITOR, executor, plugin, true
            );
        } catch (ClassNotFoundException ignored) {
            // Plugin tidak terpasang, skip
        } catch (Exception e) {
            plugin.getLogger().warning("[WorldCore] Failed to register listener for " + className + ": " + e.getMessage());
        }
    }

    private void tryRegisterTowny(@NotNull Plugin plugin, @NotNull AchievementManager achievements) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>)
                    Class.forName("com.palmergames.bukkit.towny.event.NewTownEvent");

            EventExecutor executor = (listener, event) -> {
                if (!eventClass.isInstance(event)) return;
                Player player = getPlayer(event);
                if (player != null) achievements.triggerOneShot(player.getUniqueId(), AchievementCategory.TOWNY);
            };

            plugin.getServer().getPluginManager().registerEvent(
                    eventClass, new org.bukkit.event.Listener() {}, EventPriority.MONITOR, executor, plugin, true
            );
        } catch (ClassNotFoundException ignored) {}
        catch (Exception e) {
            plugin.getLogger().warning("[WorldCore] Failed to register Towny listener: " + e.getMessage());
        }
    }

    private Player getPlayer(Event event) {
        for (String methodName : new String[]{"getPlayer", "getCreator", "getOwner"}) {
            try {
                Method m = event.getClass().getMethod(methodName);
                Object result = m.invoke(event);
                if (result instanceof Player p) return p;
                if (result instanceof org.bukkit.OfflinePlayer op && op.isOnline()) return op.getPlayer();
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {}
        }
        return null;
    }
}