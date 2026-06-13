package dev.worldcore.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstraction over the persistence backend (SQLite or YAML).
 *
 * <p>Implementations must be thread-safe — many calls happen from async tasks.
 */
public interface StorageBackend {

    /** Initialise (create tables / load files). */
    void init() throws Exception;

    /** Close any open connections or file handles. */
    void close();

    // ─── Expeditions ──────────────────────────────────────────────────────────

    /** Save a new active expedition. */
    void saveExpedition(@NotNull UUID playerUuid, @NotNull String expeditionId,
                        long startedAt, long endsAt);

    /** Mark an expedition as claimed. */
    void claimExpedition(@NotNull UUID playerUuid, @NotNull String expeditionId, long startedAt);

    /** Load all unclaimed active expeditions for a player. */
    @NotNull List<ActiveExpeditionRow> loadExpeditions(@NotNull UUID playerUuid);

    /** Remove all completed+claimed expeditions older than cutoff (ms since epoch). */
    void pruneOldExpeditions(long cutoffMs);

    // ─── Achievements ─────────────────────────────────────────────────────────

    /** Load all achievement progress for a player. */
    @NotNull Map<String, AchievementProgressRow> loadAchievements(@NotNull UUID playerUuid);

    /** Upsert achievement progress. */
    void saveAchievementProgress(@NotNull UUID playerUuid, @NotNull String achievementId,
                                 long progress, boolean unlocked, long unlockedAt);

    // ─── Titles ───────────────────────────────────────────────────────────────

    /** Load all unlocked title IDs for a player. */
    @NotNull List<String> loadUnlockedTitles(@NotNull UUID playerUuid);

    /** Record a newly unlocked title. */
    void saveTitle(@NotNull UUID playerUuid, @NotNull String titleId, long unlockedAt);

    /** Get the active title for a player (null = none set). */
    @Nullable String loadActiveTitle(@NotNull UUID playerUuid);

    /** Set or clear the active title for a player. */
    void saveActiveTitle(@NotNull UUID playerUuid, @Nullable String titleId);

    // ─── Statistics ───────────────────────────────────────────────────────────

    /** Load stat counters for a player. */
    @NotNull StatRow loadStats(@NotNull UUID playerUuid);

    /** Persist stat counters for a player. */
    void saveStats(@NotNull UUID playerUuid, @NotNull StatRow stats);

    // ─── Expedition Slots ─────────────────────────────────────────────────────

    /** Get how many extra (purchased/granted) slots a player has (0 = permission-only). */
    int loadExtraSlots(@NotNull UUID playerUuid);

    /** Set extra slots for a player. */
    void saveExtraSlots(@NotNull UUID playerUuid, int extraSlots);

    // ─── Market ───────────────────────────────────────────────────────────────

    /** Load all supply/demand records. */
    @NotNull Map<String, MarketSupplyRow> loadMarketSupply();

    /** Save a batch of supply/demand updates. */
    void saveMarketSupply(@NotNull Map<String, MarketSupplyRow> supply);

    /** Load market history within a timestamp range. */
    @NotNull List<MarketHistoryRow> loadMarketHistory(long sinceMs);

    /** Save a market history snapshot. */
    void saveMarketHistory(@NotNull List<MarketHistoryRow> history);

    /** Load active market trend. */
    @Nullable MarketTrendRow loadActiveTrend();

    /** Save active market trend. */
    void saveActiveTrend(@Nullable MarketTrendRow trend);

    // ─── Inner Record Types ───────────────────────────────────────────────────

    record ActiveExpeditionRow(
            @NotNull String expeditionId,
            long startedAt,
            long endsAt,
            boolean claimed
    ) {}

    record AchievementProgressRow(
            @NotNull String achievementId,
            long progress,
            boolean unlocked,
            long unlockedAt
    ) {}

    record StatRow(
            long expeditionsStarted,
            long expeditionsCompleted,
            long achievementsUnlocked,
            long eventParticipations,
            long titlesUnlocked,
            long moneyEarned,
            long itemsSold,
            long itemsBought
    ) {
        public static StatRow empty() {
            return new StatRow(0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    record MarketSupplyRow(
            @NotNull String material,
            long totalSold,
            long totalBought
    ) {}

    record MarketHistoryRow(
            long timestamp,
            @NotNull String material,
            double multiplier,
            @Nullable String trendId,
            @Nullable String seasonId,
            @Nullable String eventId
    ) {}

    record MarketTrendRow(
            @NotNull String trendId,
            long endsAt
    ) {}
}
