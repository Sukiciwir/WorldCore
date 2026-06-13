package dev.worldcore.statistics;

import dev.worldcore.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Tracks player statistics (expeditions, achievements, events, titles).
 */
public final class StatisticsManager {

    private final Plugin         plugin;
    private final Logger         log;
    private final StorageBackend storage;

    private final Map<UUID, StorageBackend.StatRow> cache = new ConcurrentHashMap<>();

    public StatisticsManager(@NotNull Plugin plugin, @NotNull Logger log, @NotNull StorageBackend storage) {
        this.plugin  = plugin;
        this.log     = log;
        this.storage = storage;
    }

    public void loadPlayer(@NotNull UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            StorageBackend.StatRow stats = storage.loadStats(uuid);
            cache.put(uuid, stats);
        });
    }

    public void unloadPlayer(@NotNull UUID uuid) {
        StorageBackend.StatRow stats = cache.remove(uuid);
        if (stats != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storage.saveStats(uuid, stats));
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, StorageBackend.StatRow> entry : cache.entrySet()) {
            storage.saveStats(entry.getKey(), entry.getValue());
        }
    }

    @NotNull
    public StorageBackend.StatRow getStats(@NotNull UUID uuid) {
        return cache.getOrDefault(uuid, StorageBackend.StatRow.empty());
    }

    public void incrementExpeditionsStarted(@NotNull UUID uuid) {
        cache.computeIfPresent(uuid, (k, v) -> new StorageBackend.StatRow(
                v.expeditionsStarted() + 1, v.expeditionsCompleted(),
                v.achievementsUnlocked(), v.eventParticipations(), v.titlesUnlocked(),
                v.moneyEarned(), v.itemsSold(), v.itemsBought()));
    }

    public void incrementExpeditionsCompleted(@NotNull UUID uuid) {
        cache.computeIfPresent(uuid, (k, v) -> new StorageBackend.StatRow(
                v.expeditionsStarted(), v.expeditionsCompleted() + 1,
                v.achievementsUnlocked(), v.eventParticipations(), v.titlesUnlocked(),
                v.moneyEarned(), v.itemsSold(), v.itemsBought()));
    }

    public void incrementAchievementsUnlocked(@NotNull UUID uuid) {
        cache.computeIfPresent(uuid, (k, v) -> new StorageBackend.StatRow(
                v.expeditionsStarted(), v.expeditionsCompleted(),
                v.achievementsUnlocked() + 1, v.eventParticipations(), v.titlesUnlocked(),
                v.moneyEarned(), v.itemsSold(), v.itemsBought()));
    }

    public void incrementEventParticipations(@NotNull UUID uuid) {
        cache.computeIfPresent(uuid, (k, v) -> new StorageBackend.StatRow(
                v.expeditionsStarted(), v.expeditionsCompleted(),
                v.achievementsUnlocked(), v.eventParticipations() + 1, v.titlesUnlocked(),
                v.moneyEarned(), v.itemsSold(), v.itemsBought()));
    }

    public void incrementTitlesUnlocked(@NotNull UUID uuid) {
        cache.computeIfPresent(uuid, (k, v) -> new StorageBackend.StatRow(
                v.expeditionsStarted(), v.expeditionsCompleted(),
                v.achievementsUnlocked(), v.eventParticipations(), v.titlesUnlocked() + 1,
                v.moneyEarned(), v.itemsSold(), v.itemsBought()));
    }

    public void addTransactionStats(@NotNull UUID uuid, long moneyDelta, long itemsSoldDelta, long itemsBoughtDelta) {
        cache.computeIfPresent(uuid, (k, v) -> new StorageBackend.StatRow(
                v.expeditionsStarted(), v.expeditionsCompleted(),
                v.achievementsUnlocked(), v.eventParticipations(), v.titlesUnlocked(),
                v.moneyEarned() + moneyDelta, v.itemsSold() + itemsSoldDelta, v.itemsBought() + itemsBoughtDelta));
    }
}
