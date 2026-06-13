package dev.worldcore.market;

import dev.worldcore.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages the active global market trend.
 */
public final class MarketTrendManager {

    private final Plugin         plugin;
    private final Logger         log;
    private final MarketConfig   config;
    private final StorageBackend storage;

    private @Nullable MarketConfig.MarketTrendDef activeTrend;
    private long endsAtMs;

    public MarketTrendManager(@NotNull Plugin plugin, @NotNull Logger log,
                              @NotNull MarketConfig config, @NotNull StorageBackend storage) {
        this.plugin  = plugin;
        this.log     = log;
        this.config  = config;
        this.storage = storage;

        loadFromDb();

        // Check for trend expiry every minute
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkTrend, 1200L, 1200L);
    }

    private void loadFromDb() {
        StorageBackend.MarketTrendRow row = storage.loadActiveTrend();
        if (row != null) {
            activeTrend = config.getTrend(row.trendId());
            endsAtMs    = row.endsAt();
            if (activeTrend == null || System.currentTimeMillis() >= endsAtMs) {
                rollNewTrend();
            } else {
                log.info("[Market] Restored active trend: " + activeTrend.id());
            }
        } else {
            rollNewTrend();
        }
    }

    private synchronized void checkTrend() {
        if (activeTrend == null || System.currentTimeMillis() >= endsAtMs) {
            rollNewTrend();
        }
    }

    /** Randomly selects a new trend based on weights. */
    public synchronized void rollNewTrend() {
        List<MarketConfig.MarketTrendDef> trends = new ArrayList<>(config.getTrends());
        if (trends.isEmpty()) {
            activeTrend = null;
            return;
        }

        int totalWeight = trends.stream().mapToInt(MarketConfig.MarketTrendDef::chance).sum();
        if (totalWeight <= 0) return;

        int roll = new java.util.Random().nextInt(totalWeight);
        int cursor = 0;
        for (MarketConfig.MarketTrendDef def : trends) {
            cursor += def.chance();
            if (roll < cursor) {
                setTrend(def);
                return;
            }
        }
    }

    /** Forcibly sets the active trend. */
    public synchronized void setTrend(@NotNull MarketConfig.MarketTrendDef trend) {
        this.activeTrend = trend;
        this.endsAtMs    = System.currentTimeMillis() + (trend.durationDays() * 86400000L);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> 
                storage.saveActiveTrend(new StorageBackend.MarketTrendRow(trend.id(), endsAtMs)));
        
        log.info("[Market] New trend active: " + trend.id() + " (lasts " + trend.durationDays() + " days)");
    }

    @Nullable
    public MarketConfig.MarketTrendDef getActiveTrend() {
        return activeTrend;
    }
}
