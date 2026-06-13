package dev.worldcore.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * SQLite-backed storage for WorldCore.
 *
 * <p>Schema managed via {@code CREATE TABLE IF NOT EXISTS} — safe to add new
 * tables without migration for additive changes.
 */
public final class SqliteBackend implements StorageBackend {

    private static final String DB_NAME = "worldcore.db";

    private final File    dataFolder;
    private final Logger  log;
    private       Connection conn;

    public SqliteBackend(@NotNull File dataFolder, @NotNull Logger log) {
        this.dataFolder = dataFolder;
        this.log        = log;
    }

    // ─── Init / Close ─────────────────────────────────────────────────────────

    @Override
    public void init() throws Exception {
        dataFolder.mkdirs();
        File dbFile = new File(dataFolder, DB_NAME);
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // Performance pragmas
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA synchronous=NORMAL");
            st.execute("PRAGMA foreign_keys=ON");
        }

        createTables();
        log.info("[Storage] SQLite initialised: " + dbFile.getAbsolutePath());
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            log.warning("[Storage] Error closing SQLite: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_expeditions (
                    uuid         TEXT    NOT NULL,
                    expedition   TEXT    NOT NULL,
                    started_at   INTEGER NOT NULL,
                    ends_at      INTEGER NOT NULL,
                    claimed      INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, expedition, started_at)
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_achievements (
                    uuid         TEXT    NOT NULL,
                    achievement  TEXT    NOT NULL,
                    progress     INTEGER NOT NULL DEFAULT 0,
                    unlocked     INTEGER NOT NULL DEFAULT 0,
                    unlocked_at  INTEGER,
                    PRIMARY KEY (uuid, achievement)
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_titles (
                    uuid        TEXT    NOT NULL,
                    title       TEXT    NOT NULL,
                    unlocked_at INTEGER NOT NULL,
                    PRIMARY KEY (uuid, title)
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_active_title (
                    uuid    TEXT PRIMARY KEY,
                    title   TEXT NOT NULL
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_statistics (
                    uuid                    TEXT PRIMARY KEY,
                    expeditions_started     INTEGER NOT NULL DEFAULT 0,
                    expeditions_completed   INTEGER NOT NULL DEFAULT 0,
                    achievements_unlocked   INTEGER NOT NULL DEFAULT 0,
                    event_participations    INTEGER NOT NULL DEFAULT 0,
                    titles_unlocked         INTEGER NOT NULL DEFAULT 0,
                    money_earned            INTEGER NOT NULL DEFAULT 0,
                    items_sold              INTEGER NOT NULL DEFAULT 0,
                    items_bought            INTEGER NOT NULL DEFAULT 0
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_expedition_slots (
                    uuid         TEXT PRIMARY KEY,
                    extra_slots  INTEGER NOT NULL DEFAULT 0
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_market_supply (
                    material     TEXT PRIMARY KEY,
                    total_sold   INTEGER NOT NULL DEFAULT 0,
                    total_bought INTEGER NOT NULL DEFAULT 0
                )""");
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_market_history (
                    timestamp    INTEGER NOT NULL,
                    material     TEXT NOT NULL,
                    multiplier   REAL NOT NULL,
                    trend_id     TEXT,
                    season_id    TEXT,
                    event_id     TEXT
                )""");
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_market_history_time ON wc_market_history(timestamp)
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS wc_market_trend (
                    id       INTEGER PRIMARY KEY CHECK (id = 1),
                    trend_id TEXT NOT NULL,
                    ends_at  INTEGER NOT NULL
                )""");
        }
    }

    // ─── Expeditions ──────────────────────────────────────────────────────────

    @Override
    public synchronized void saveExpedition(@NotNull UUID playerUuid, @NotNull String expeditionId,
                                            long startedAt, long endsAt) {
        String sql = """
            INSERT OR REPLACE INTO wc_expeditions
              (uuid, expedition, started_at, ends_at, claimed) VALUES (?,?,?,?,0)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, expeditionId);
            ps.setLong(3, startedAt);
            ps.setLong(4, endsAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Storage] saveExpedition: " + e.getMessage());
        }
    }

    @Override
    public synchronized void claimExpedition(@NotNull UUID playerUuid,
                                             @NotNull String expeditionId, long startedAt) {
        String sql = """
            UPDATE wc_expeditions SET claimed=1
             WHERE uuid=? AND expedition=? AND started_at=?""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, expeditionId);
            ps.setLong(3, startedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Storage] claimExpedition: " + e.getMessage());
        }
    }

    @Override
    public synchronized @NotNull List<ActiveExpeditionRow> loadExpeditions(@NotNull UUID playerUuid) {
        String sql = """
            SELECT expedition, started_at, ends_at, claimed
              FROM wc_expeditions WHERE uuid=? AND claimed=0""";
        List<ActiveExpeditionRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ActiveExpeditionRow(
                            rs.getString("expedition"),
                            rs.getLong("started_at"),
                            rs.getLong("ends_at"),
                            rs.getInt("claimed") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadExpeditions: " + e.getMessage());
        }
        return rows;
    }

    @Override
    public synchronized void pruneOldExpeditions(long cutoffMs) {
        String sql = "DELETE FROM wc_expeditions WHERE claimed=1 AND ends_at<?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cutoffMs);
            int deleted = ps.executeUpdate();
            if (deleted > 0) log.fine("[Storage] Pruned " + deleted + " old expeditions.");
        } catch (SQLException e) {
            log.warning("[Storage] pruneOldExpeditions: " + e.getMessage());
        }
    }

    // ─── Achievements ─────────────────────────────────────────────────────────

    @Override
    public synchronized @NotNull Map<String, AchievementProgressRow> loadAchievements(
            @NotNull UUID playerUuid) {
        String sql = "SELECT achievement, progress, unlocked, unlocked_at FROM wc_achievements WHERE uuid=?";
        Map<String, AchievementProgressRow> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("achievement");
                    map.put(id, new AchievementProgressRow(
                            id,
                            rs.getLong("progress"),
                            rs.getInt("unlocked") == 1,
                            rs.getLong("unlocked_at")
                    ));
                }
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadAchievements: " + e.getMessage());
        }
        return map;
    }

    @Override
    public synchronized void saveAchievementProgress(@NotNull UUID playerUuid,
                                                     @NotNull String achievementId,
                                                     long progress, boolean unlocked, long unlockedAt) {
        String sql = """
            INSERT INTO wc_achievements (uuid, achievement, progress, unlocked, unlocked_at)
            VALUES (?,?,?,?,?)
            ON CONFLICT(uuid, achievement) DO UPDATE SET
              progress=excluded.progress,
              unlocked=excluded.unlocked,
              unlocked_at=CASE WHEN excluded.unlocked=1 AND wc_achievements.unlocked=0
                               THEN excluded.unlocked_at
                               ELSE wc_achievements.unlocked_at END""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, achievementId);
            ps.setLong(3, progress);
            ps.setInt(4, unlocked ? 1 : 0);
            ps.setLong(5, unlockedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Storage] saveAchievementProgress: " + e.getMessage());
        }
    }

    // ─── Titles ───────────────────────────────────────────────────────────────

    @Override
    public synchronized @NotNull List<String> loadUnlockedTitles(@NotNull UUID playerUuid) {
        String sql = "SELECT title FROM wc_titles WHERE uuid=?";
        List<String> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("title"));
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadUnlockedTitles: " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized void saveTitle(@NotNull UUID playerUuid, @NotNull String titleId,
                                       long unlockedAt) {
        String sql = "INSERT OR IGNORE INTO wc_titles (uuid, title, unlocked_at) VALUES (?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, titleId);
            ps.setLong(3, unlockedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Storage] saveTitle: " + e.getMessage());
        }
    }

    @Override
    public synchronized @Nullable String loadActiveTitle(@NotNull UUID playerUuid) {
        String sql = "SELECT title FROM wc_active_title WHERE uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("title");
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadActiveTitle: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void saveActiveTitle(@NotNull UUID playerUuid, @Nullable String titleId) {
        if (titleId == null) {
            String sql = "DELETE FROM wc_active_title WHERE uuid=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[Storage] clearActiveTitle: " + e.getMessage());
            }
        } else {
            String sql = "INSERT OR REPLACE INTO wc_active_title (uuid, title) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, titleId);
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[Storage] saveActiveTitle: " + e.getMessage());
            }
        }
    }

    // ─── Statistics ───────────────────────────────────────────────────────────

    @Override
    public synchronized @NotNull StatRow loadStats(@NotNull UUID playerUuid) {
        String sql = "SELECT * FROM wc_statistics WHERE uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new StatRow(
                            rs.getLong("expeditions_started"),
                            rs.getLong("expeditions_completed"),
                            rs.getLong("achievements_unlocked"),
                            rs.getLong("event_participations"),
                            rs.getLong("titles_unlocked"),
                            rs.getLong("money_earned"),
                            rs.getLong("items_sold"),
                            rs.getLong("items_bought")
                    );
                }
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadStats: " + e.getMessage());
        }
        return StatRow.empty();
    }

    @Override
    public synchronized void saveStats(@NotNull UUID playerUuid, @NotNull StatRow stats) {
        String sql = """
            INSERT INTO wc_statistics
              (uuid, expeditions_started, expeditions_completed,
               achievements_unlocked, event_participations, titles_unlocked,
               money_earned, items_sold, items_bought)
            VALUES (?,?,?,?,?,?,?,?,?)
            ON CONFLICT(uuid) DO UPDATE SET
              expeditions_started=excluded.expeditions_started,
              expeditions_completed=excluded.expeditions_completed,
              achievements_unlocked=excluded.achievements_unlocked,
              event_participations=excluded.event_participations,
              titles_unlocked=excluded.titles_unlocked,
              money_earned=excluded.money_earned,
              items_sold=excluded.items_sold,
              items_bought=excluded.items_bought""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, stats.expeditionsStarted());
            ps.setLong(3, stats.expeditionsCompleted());
            ps.setLong(4, stats.achievementsUnlocked());
            ps.setLong(5, stats.eventParticipations());
            ps.setLong(6, stats.titlesUnlocked());
            ps.setLong(7, stats.moneyEarned());
            ps.setLong(8, stats.itemsSold());
            ps.setLong(9, stats.itemsBought());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Storage] saveStats: " + e.getMessage());
        }
    }

    // ─── Expedition Slots ─────────────────────────────────────────────────────

    @Override
    public synchronized int loadExtraSlots(@NotNull UUID playerUuid) {
        String sql = "SELECT extra_slots FROM wc_expedition_slots WHERE uuid=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("extra_slots");
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadExtraSlots: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public synchronized void saveExtraSlots(@NotNull UUID playerUuid, int extraSlots) {
        String sql = "INSERT OR REPLACE INTO wc_expedition_slots (uuid, extra_slots) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, extraSlots);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warning("[Storage] saveExtraSlots: " + e.getMessage());
        }
    }

    // ─── Market ───────────────────────────────────────────────────────────────

    @Override
    public synchronized @NotNull Map<String, MarketSupplyRow> loadMarketSupply() {
        String sql = "SELECT material, total_sold, total_bought FROM wc_market_supply";
        Map<String, MarketSupplyRow> map = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String mat = rs.getString("material");
                map.put(mat, new MarketSupplyRow(mat, rs.getLong("total_sold"), rs.getLong("total_bought")));
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadMarketSupply: " + e.getMessage());
        }
        return map;
    }

    @Override
    public synchronized void saveMarketSupply(@NotNull Map<String, MarketSupplyRow> supply) {
        if (supply.isEmpty()) return;
        String sql = """
            INSERT INTO wc_market_supply (material, total_sold, total_bought)
            VALUES (?,?,?)
            ON CONFLICT(material) DO UPDATE SET
              total_sold=excluded.total_sold, total_bought=excluded.total_bought""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (MarketSupplyRow row : supply.values()) {
                ps.setString(1, row.material());
                ps.setLong(2, row.totalSold());
                ps.setLong(3, row.totalBought());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            log.warning("[Storage] saveMarketSupply: " + e.getMessage());
        }
    }

    @Override
    public synchronized @NotNull List<MarketHistoryRow> loadMarketHistory(long sinceMs) {
        String sql = "SELECT * FROM wc_market_history WHERE timestamp >= ?";
        List<MarketHistoryRow> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sinceMs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MarketHistoryRow(
                            rs.getLong("timestamp"),
                            rs.getString("material"),
                            rs.getDouble("multiplier"),
                            rs.getString("trend_id"),
                            rs.getString("season_id"),
                            rs.getString("event_id")
                    ));
                }
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadMarketHistory: " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized void saveMarketHistory(@NotNull List<MarketHistoryRow> history) {
        if (history.isEmpty()) return;
        String sql = """
            INSERT INTO wc_market_history (timestamp, material, multiplier, trend_id, season_id, event_id)
            VALUES (?,?,?,?,?,?)""";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (MarketHistoryRow row : history) {
                ps.setLong(1, row.timestamp());
                ps.setString(2, row.material());
                ps.setDouble(3, row.multiplier());
                ps.setString(4, row.trendId());
                ps.setString(5, row.seasonId());
                ps.setString(6, row.eventId());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            log.warning("[Storage] saveMarketHistory: " + e.getMessage());
        }
    }

    @Override
    public synchronized @Nullable MarketTrendRow loadActiveTrend() {
        String sql = "SELECT trend_id, ends_at FROM wc_market_trend WHERE id=1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new MarketTrendRow(rs.getString("trend_id"), rs.getLong("ends_at"));
            }
        } catch (SQLException e) {
            log.warning("[Storage] loadActiveTrend: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized void saveActiveTrend(@Nullable MarketTrendRow trend) {
        if (trend == null) {
            String sql = "DELETE FROM wc_market_trend WHERE id=1";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[Storage] clearActiveTrend: " + e.getMessage());
            }
        } else {
            String sql = "INSERT OR REPLACE INTO wc_market_trend (id, trend_id, ends_at) VALUES (1,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, trend.trendId());
                ps.setLong(2, trend.endsAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                log.warning("[Storage] saveActiveTrend: " + e.getMessage());
            }
        }
    }
}
