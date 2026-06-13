package dev.worldcore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * PlayerPoints hook using reflection to avoid direct compilation dependency.
 */
public final class PlayerPointsHook {

    private final Logger log;
    private Object ppAPI;
    private Method lookMethod;
    private Method takeMethod;
    private Method giveMethod;
    private boolean ready = false;

    public PlayerPointsHook(@NotNull Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin != null && plugin.isEnabled()) {
            try {
                Method getAPI = plugin.getClass().getMethod("getAPI");
                ppAPI = getAPI.invoke(plugin);
                Class<?> apiClass = ppAPI.getClass();
                
                lookMethod = apiClass.getMethod("look", java.util.UUID.class);
                takeMethod = apiClass.getMethod("take", java.util.UUID.class, int.class);
                giveMethod = apiClass.getMethod("give", java.util.UUID.class, int.class);
                
                ready = true;
                log.info("[Hooks/PlayerPoints] Hook active (reflection mode).");
            } catch (Throwable e) {
                log.warning("[Hooks/PlayerPoints] Failed to init reflection: " + e.getMessage());
            }
        }
    }

    public boolean isReady() { return ready; }

    public int getPoints(@NotNull Player player) {
        if (!ready) return 0;
        try {
            return (int) lookMethod.invoke(ppAPI, player.getUniqueId());
        } catch (Throwable e) { return 0; }
    }

    public boolean take(@NotNull Player player, int amount) {
        if (!ready) return false;
        try {
            return (boolean) takeMethod.invoke(ppAPI, player.getUniqueId(), amount);
        } catch (Throwable e) { return false; }
    }

    public boolean give(@NotNull Player player, int amount) {
        if (!ready) return false;
        try {
            return (boolean) giveMethod.invoke(ppAPI, player.getUniqueId(), amount);
        } catch (Throwable e) { return false; }
    }
}
