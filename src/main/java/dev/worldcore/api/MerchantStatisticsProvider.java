package dev.worldcore.api;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.statistics.StatisticsManager;
import dev.worldcore.storage.StorageBackend;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Exposes merchant statistics for external plugins (like HallOfFameCore).
 */
public final class MerchantStatisticsProvider {

    private final WorldCorePlugin plugin;

    public MerchantStatisticsProvider(@NotNull WorldCorePlugin plugin) {
        this.plugin = plugin;
    }

    /** @return Total money earned through transactions. */
    public long getMoneyEarned(@NotNull UUID uuid) {
        StorageBackend.StatRow row = plugin.getStatsManager().getStats(uuid);
        return row.moneyEarned();
    }

    /** @return Total items sold to the server. */
    public long getItemsSold(@NotNull UUID uuid) {
        StorageBackend.StatRow row = plugin.getStatsManager().getStats(uuid);
        return row.itemsSold();
    }

    /** @return Total items bought from the server. */
    public long getItemsBought(@NotNull UUID uuid) {
        StorageBackend.StatRow row = plugin.getStatsManager().getStats(uuid);
        return row.itemsBought();
    }
}
