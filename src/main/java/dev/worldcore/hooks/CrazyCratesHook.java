package dev.worldcore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * CrazyCrates hook.
 *
 * <p>Uses reflection to avoid compile-time dependencies, as CrazyCrates API
 * changes frequently. Dispatches "give physical key" commands or uses API to
 * deliver keys to the player.
 */
public final class CrazyCratesHook {

    private final Logger log;
    private boolean ready = false;

    public CrazyCratesHook(@NotNull Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("CrazyCrates");
        if (plugin == null || !plugin.isEnabled()) return;
        ready = true;
        log.info("[Hooks/CrazyCrates] Hook active (using console command dispatch).");
    }

    public boolean isReady() { return ready; }

    /**
     * Gives a physical crate key to a player.
     *
     * <p>We use command dispatch instead of internal API for maximum compatibility
     * across different CrazyCrates v1 and v2 versions.
     * The typical command is: /cc give physical <crate> <amount> <player>
     */
    public void giveKey(@NotNull Player player, @NotNull String crateId, int amount) {
        if (!ready) return;
        String cmd = String.format("cc give physical %s %d %s", crateId, amount, player.getName());
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("WorldCore"), () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        });
    }
}
