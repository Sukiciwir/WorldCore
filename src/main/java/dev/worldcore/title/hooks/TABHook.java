package dev.worldcore.title.hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * TAB plugin hook — sets player tab prefix via reflection.
 * Supports TAB Reborn (me.neznamy.tab.api.TabAPI).
 */
public final class TABHook {

    private final Logger log;
    private boolean ready = false;
    private Object tabAPI;
    private Method getPlayer;
    private Method getPrefixManager;
    private Method setCustomPrefix;

    public TABHook(@NotNull Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        Plugin tab = Bukkit.getPluginManager().getPlugin("TAB");
        if (tab == null || !tab.isEnabled()) return;
        try {
            Class<?> apiClass   = Class.forName("me.neznamy.tab.api.TabAPI");
            Method   getInstance = apiClass.getMethod("getInstance");
            tabAPI = getInstance.invoke(null);

            getPlayer      = apiClass.getMethod("getPlayer", java.util.UUID.class);
            // Get prefix manager
            Method pm = apiClass.getMethod("getTeamManager");
            getPrefixManager = pm;

            // Try to get setCustomPrefix
            Class<?> teamMgrClass = pm.getReturnType();
            try {
                setCustomPrefix = teamMgrClass.getMethod("setCustomPrefix",
                        Class.forName("me.neznamy.tab.api.TabPlayer"), String.class);
            } catch (NoSuchMethodException e2) {
                // older API — try setPrefix
                setCustomPrefix = teamMgrClass.getMethod("setPrefix",
                        Class.forName("me.neznamy.tab.api.TabPlayer"), String.class);
            }
            ready = true;
            log.info("[Titles/TAB] Hook active.");
        } catch (Throwable e) {
            log.fine("[Titles/TAB] Not available: " + e.getMessage());
        }
    }

    public boolean isReady() { return ready; }

    public void setPrefix(@NotNull Player player, @NotNull String prefix) {
        if (!ready) return;
        try {
            Object tabPlayer  = getPlayer.invoke(tabAPI, player.getUniqueId());
            if (tabPlayer == null) return;
            Object teamMgr    = getPrefixManager.invoke(tabAPI);
            if (teamMgr == null) return;
            setCustomPrefix.invoke(teamMgr, tabPlayer, prefix);
        } catch (Throwable e) {
            log.fine("[Titles/TAB] setPrefix failed: " + e.getMessage());
        }
    }
}
