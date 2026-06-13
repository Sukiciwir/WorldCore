package dev.worldcore.api;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.achievement.AchievementManager;
import dev.worldcore.expedition.ExpeditionManager;
import dev.worldcore.state.WorldState;
import dev.worldcore.state.WorldStateManager;
import dev.worldcore.title.TitleManager;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for external plugins to interact with WorldCore.
 */
public final class WorldCoreAPI {

    private static WorldCoreAPI instance;
    private final WorldCorePlugin plugin;

    public WorldCoreAPI(@NotNull WorldCorePlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /** @return The global WorldCore API instance. */
    @Nullable
    public static WorldCoreAPI getInstance() {
        if (instance == null) {
            WorldCorePlugin p = (WorldCorePlugin) Bukkit.getPluginManager().getPlugin("WorldCore");
            if (p != null && p.isEnabled()) instance = new WorldCoreAPI(p);
        }
        return instance;
    }

    /** @return The current active world state. */
    @NotNull
    public WorldState getActiveState() {
        return plugin.getStateManager().getActiveState();
    }

    /** @return true if an event is currently active. */
    public boolean isEventActive() {
        return plugin.getStateManager().isEventActive();
    }

    /** @return The active state ID (season or event). */
    @NotNull
    public String getActiveStateId() {
        return plugin.getStateManager().getActiveState().getId();
    }

    /** @return The WorldStateManager instance. */
    @NotNull
    public WorldStateManager getStateManager() {
        return plugin.getStateManager();
    }

    /** @return The ExpeditionManager instance. */
    @NotNull
    public ExpeditionManager getExpeditionManager() {
        return plugin.getExpeditionManager();
    }

    /** @return The AchievementManager instance. */
    @NotNull
    public AchievementManager getAchievementManager() {
        return plugin.getAchievementManager();
    }

    /** @return The TitleManager instance. */
    @NotNull
    public TitleManager getTitleManager() {
        return plugin.getTitleManager();
    }

    /** @return The MarketManager instance. */
    @NotNull
    public dev.worldcore.market.MarketManager getMarketManager() {
        return plugin.getMarketManager();
    }

    /** @return The MerchantStatisticsProvider instance. */
    @NotNull
    public MerchantStatisticsProvider getMerchantStatisticsProvider() {
        return new MerchantStatisticsProvider(plugin);
    }
}
