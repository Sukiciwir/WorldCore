package dev.worldcore.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * YAML fallback storage backend.
 *
 * <p>Uses Bukkit's {@link org.bukkit.configuration.file.YamlConfiguration}.
 * Not recommended for servers with many players — prefer SQLite.
 */
public final class YamlBackend implements StorageBackend {

    private final File   dataFolder;
    private final Logger log;

    private final Map<String, Map<String, Object>> data = new HashMap<>();

    public YamlBackend(@NotNull File dataFolder, @NotNull Logger log) {
        this.dataFolder = dataFolder;
        this.log        = log;
    }

    @Override
    public void init() {
        dataFolder.mkdirs();
        log.info("[Storage] YAML backend initialised at: " + dataFolder.getAbsolutePath());
    }

    @Override public void close() {}

    // ─── Expeditions ──────────────────────────────────────────────────────────

    @Override public void saveExpedition(@NotNull UUID u, @NotNull String id, long s, long e) {
        log.warning("[YAML Storage] saveExpedition not fully implemented — use SQLite.");
    }
    @Override public void claimExpedition(@NotNull UUID u, @NotNull String id, long s) {}
    @Override public @NotNull List<ActiveExpeditionRow> loadExpeditions(@NotNull UUID u) { return List.of(); }
    @Override public void pruneOldExpeditions(long cutoffMs) {}

    // ─── Achievements ─────────────────────────────────────────────────────────

    @Override public @NotNull Map<String, AchievementProgressRow> loadAchievements(@NotNull UUID u) { return Map.of(); }
    @Override public void saveAchievementProgress(@NotNull UUID u, @NotNull String id, long p, boolean ul, long ulAt) {}

    // ─── Titles ───────────────────────────────────────────────────────────────

    @Override public @NotNull List<String> loadUnlockedTitles(@NotNull UUID u) { return List.of(); }
    @Override public void saveTitle(@NotNull UUID u, @NotNull String id, long at) {}
    @Override public @Nullable String loadActiveTitle(@NotNull UUID u) { return null; }
    @Override public void saveActiveTitle(@NotNull UUID u, @Nullable String id) {}

    // ─── Statistics ───────────────────────────────────────────────────────────

    @Override public @NotNull StatRow loadStats(@NotNull UUID u) { return StatRow.empty(); }
    @Override public void saveStats(@NotNull UUID u, @NotNull StatRow s) {}

    // ─── Expedition Slots ─────────────────────────────────────────────────────

    @Override public int loadExtraSlots(@NotNull UUID u) { return 0; }
    @Override public void saveExtraSlots(@NotNull UUID u, int extra) {}

    // ─── Market ───────────────────────────────────────────────────────────────

    @Override public @NotNull Map<String, MarketSupplyRow> loadMarketSupply() { return Map.of(); }
    @Override public void saveMarketSupply(@NotNull Map<String, MarketSupplyRow> supply) {}

    @Override public @NotNull List<MarketHistoryRow> loadMarketHistory(long sinceMs) { return List.of(); }
    @Override public void saveMarketHistory(@NotNull List<MarketHistoryRow> history) {}

    @Override public @Nullable MarketTrendRow loadActiveTrend() { return null; }
    @Override public void saveActiveTrend(@Nullable MarketTrendRow trend) {}
}