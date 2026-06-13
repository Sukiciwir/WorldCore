package dev.worldcore.market;

import dev.worldcore.market.modifiers.ModifierProvider;
import dev.worldcore.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks items sold/bought and provides supply/demand price modifier.
 */
public final class SupplyDemandTracker implements ModifierProvider {

    private final Plugin         plugin;
    private final Logger         log;
    private final MarketConfig   config;
    private final StorageBackend storage;

    // material name -> data
    private final Map<String, SupplyData> cache = new ConcurrentHashMap<>();

    public SupplyDemandTracker(@NotNull Plugin plugin, @NotNull Logger log,
                               @NotNull MarketConfig config, @NotNull StorageBackend storage) {
        this.plugin  = plugin;
        this.log     = log;
        this.config  = config;
        this.storage = storage;

        // Load existing data from SQLite
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, StorageBackend.MarketSupplyRow> rows = storage.loadMarketSupply();
            for (StorageBackend.MarketSupplyRow row : rows.values()) {
                cache.put(row.material(), new SupplyData(row.totalSold(), row.totalBought()));
            }
        });

        // Periodic flush to DB every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushToDb, 6000L, 6000L);
    }

    /**
     * Record a transaction (called by TransactionProviders).
     *
     * @param material The material traded.
     * @param amount   Amount of items.
     * @param isSell   True if player sold to server (supply goes up), false if bought from server.
     */
    public void recordTransaction(@NotNull Material material, int amount, boolean isSell) {
        cache.compute(material.name(), (k, v) -> {
            if (v == null) v = new SupplyData(0, 0);
            if (isSell) v.sold += amount;
            else        v.bought += amount;
            v.dirty = true;
            return v;
        });
    }

    /**
     * Flushes dirty records to the database.
     */
    public void flushToDb() {
        Map<String, StorageBackend.MarketSupplyRow> toSave = new HashMap<>();
        for (Map.Entry<String, SupplyData> entry : cache.entrySet()) {
            SupplyData sd = entry.getValue();
            if (sd.dirty) {
                toSave.put(entry.getKey(), new StorageBackend.MarketSupplyRow(entry.getKey(), sd.sold, sd.bought));
                sd.dirty = false;
            }
        }
        if (!toSave.isEmpty()) {
            storage.saveMarketSupply(toSave);
        }
    }

    @Override
    public double getMultiplier(@NotNull Material material, @Nullable String category) {
        SupplyData sd = cache.get(material.name());
        if (sd == null) return 1.0;

        // If player sells to server, supply goes up -> price should drop.
        // If player buys from server, demand goes up -> price should rise.
        // Ratio = bought / max(1, sold).
        // If bought > sold, ratio > 1 -> price goes up.
        // If sold > bought, ratio < 1 -> price goes down.
        double baseRatio = (double) sd.bought / Math.max(1.0, (double) sd.sold);

        // Smoothing formula to prevent extreme spikes initially.
        // Use logarithmic or capped interpolation.
        // For simplicity, we just clamp the ratio.
        
        double mult = baseRatio;
        if (mult > config.getMaxPositiveModifier()) mult = config.getMaxPositiveModifier();
        if (mult < config.getMaxNegativeModifier()) mult = config.getMaxNegativeModifier();

        return mult;
    }

    public long getTotalSold(@NotNull Material material) {
        SupplyData sd = cache.get(material.name());
        return sd != null ? sd.sold : 0;
    }

    public long getTotalBought(@NotNull Material material) {
        SupplyData sd = cache.get(material.name());
        return sd != null ? sd.bought : 0;
    }

    private static class SupplyData {
        long sold;
        long bought;
        volatile boolean dirty = false;

        SupplyData(long sold, long bought) {
            this.sold   = sold;
            this.bought = bought;
        }
    }
}
