package dev.worldcore.market;

import dev.worldcore.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Periodically snapshots the current market multipliers for all configured materials.
 */
public final class MarketHistoryTracker {

    private final Plugin         plugin;
    private final Logger         log;
    private final MarketConfig   config;
    private final MarketManager  marketManager;
    private final StorageBackend storage;

    public MarketHistoryTracker(@NotNull Plugin plugin, @NotNull Logger log,
                                @NotNull MarketConfig config, @NotNull MarketManager marketManager,
                                @NotNull StorageBackend storage) {
        this.plugin        = plugin;
        this.log           = log;
        this.config        = config;
        this.marketManager = marketManager;
        this.storage       = storage;

        long interval = config.getHistoryIntervalTicks();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::snapshot, interval, interval);
    }

    private void snapshot() {
        long now = System.currentTimeMillis();
        List<StorageBackend.MarketHistoryRow> batch = new ArrayList<>();

        String trendId  = marketManager.getTrendManager().getActiveTrend() != null 
                ? marketManager.getTrendManager().getActiveTrend().id() : null;
        String seasonId = dev.worldcore.api.WorldCoreAPI.getInstance().getStateManager().getActiveSeasonId();
        String eventId  = dev.worldcore.api.WorldCoreAPI.getInstance().getStateManager().getActiveEventId();

        // We only snapshot materials that have a category defined (the tracked ones)
        for (Material mat : Material.values()) {
            if (config.getCategory(mat) != null) {
                double mult = marketManager.getMultiplier(mat);
                batch.add(new StorageBackend.MarketHistoryRow(now, mat.name(), mult, trendId, seasonId, eventId));
            }
        }

        if (!batch.isEmpty()) {
            storage.saveMarketHistory(batch);
            log.fine("[Market] Saved history snapshot for " + batch.size() + " items.");
        }
    }
}
