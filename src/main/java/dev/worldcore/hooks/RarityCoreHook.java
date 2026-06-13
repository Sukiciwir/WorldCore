package dev.worldcore.hooks;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * RarityCore hook.
 *
 * <p>Included for completeness as WorldCore sits at the top of the stack.
 * Could be used to boost rarity chances based on WorldState.
 */
public final class RarityCoreHook {

    private final Logger log;
    private boolean ready = false;

    public RarityCoreHook(@NotNull Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("RarityCore");
        if (plugin != null && plugin.isEnabled()) {
            ready = true;
            log.info("[Hooks/RarityCore] Hook active.");
        }
    }

    public boolean isReady() { return ready; }
}
