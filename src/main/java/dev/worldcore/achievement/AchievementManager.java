package dev.worldcore.achievement;

import dev.worldcore.config.MainConfig;
import dev.worldcore.config.MessageConfig;
import dev.worldcore.state.WorldState;
import dev.worldcore.statistics.StatisticsManager;
import dev.worldcore.storage.StorageBackend;
import dev.worldcore.title.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central manager for WorldCore's achievement system.
 *
 * <p>Achievements are defined in {@code achievements.yml} and tracked per
 * player in the storage backend. On unlock, a title may be awarded and a
 * server-wide broadcast sent (if configured).
 */
public final class AchievementManager {

    private final Plugin           plugin;
    private final Logger           log;
    private final MainConfig       config;
    private final MessageConfig    messages;
    private final StorageBackend   storage;
    private final StatisticsManager stats;
    private @Nullable TitleManager  titleManager; // set after TitleManager init

    /** All achievement definitions, keyed by ID. */
    private final Map<String, Achievement> definitions = new LinkedHashMap<>();

    /** Per-player achievement progress cache. */
    private final Map<UUID, Map<String, AchievementProgress>> progressCache = new ConcurrentHashMap<>();

    /** Current world state ID (updated on state change). */
    private volatile String currentStateId = "spring";

    public AchievementManager(@NotNull Plugin plugin, @NotNull Logger log,
                              @NotNull MainConfig config, @NotNull MessageConfig messages,
                              @NotNull StorageBackend storage, @NotNull StatisticsManager stats) {
        this.plugin   = plugin;
        this.log      = log;
        this.config   = config;
        this.messages = messages;
        this.storage  = storage;
        this.stats    = stats;
    }

    public void setTitleManager(@Nullable TitleManager tm) { this.titleManager = tm; }

    // ─── Config Loading ───────────────────────────────────────────────────────

    public void loadDefinitions(@NotNull File achievementsFile) {
        definitions.clear();
        if (!achievementsFile.exists()) {
            log.warning("[Achievements] achievements.yml not found.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(achievementsFile);
        List<Map<?, ?>> list = yaml.getMapList("achievements");
        for (Map<?, ?> raw : list) {
            try {
                Achievement ach = parseAchievement(raw);
                definitions.put(ach.id(), ach);
            } catch (Exception e) {
                log.warning("[Achievements] Parse error: " + e.getMessage());
            }
        }
        log.info("[Achievements] Loaded " + definitions.size() + " definitions.");
    }

    @NotNull
    private Achievement parseAchievement(@NotNull Map<?, ?> raw) {
        String id     = str(raw.get("id"));
        String name   = str(raw.get("name"));
        String desc   = str(raw.get("description"));
        AchievementCategory cat = AchievementCategory.fromString(str(raw.get("category")));
        AchievementType type    = AchievementType.fromString(str(raw.get("type")));
        long target             = toLong(raw.get("target"), 1);
        Material icon           = parseMaterial(str(raw.get("icon")));
        String titleReward      = raw.containsKey("title-reward") ? str(raw.get("title-reward")) : null;

        Set<String> worldStates = new HashSet<>();
        Object wsObj = raw.get("world-states");
        if (wsObj instanceof List<?> wsList)
            for (Object ws : wsList) worldStates.add(ws.toString().toLowerCase());

        return new Achievement(id, name, desc, cat, type, target, icon, Set.copyOf(worldStates), titleReward);
    }

    // ─── State ────────────────────────────────────────────────────────────────

    public void onStateChange(@NotNull WorldState state) {
        currentStateId = state.getId();
    }

    // ─── Player Cache ─────────────────────────────────────────────────────────

    public void loadPlayer(@NotNull UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, StorageBackend.AchievementProgressRow> rows = storage.loadAchievements(uuid);
            Map<String, AchievementProgress> progress = new HashMap<>();
            for (StorageBackend.AchievementProgressRow row : rows.values()) {
                progress.put(row.achievementId(), new AchievementProgress(
                        row.achievementId(), row.progress(), row.unlocked(), row.unlockedAt()));
            }
            progressCache.put(uuid, progress);
        });
    }

    public void savePlayer(@NotNull UUID uuid) {
        Map<String, AchievementProgress> progress = progressCache.get(uuid);
        if (progress == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (AchievementProgress ap : progress.values()) {
                storage.saveAchievementProgress(uuid, ap.getId(), ap.getProgress(),
                        ap.isUnlocked(), ap.getUnlockedAt());
            }
        });
    }

    public void unloadPlayer(@NotNull UUID uuid) {
        savePlayer(uuid);
        progressCache.remove(uuid);
    }

    // ─── Progress Update ──────────────────────────────────────────────────────

    /**
     * Increment progress for a specific achievement type/category for a player.
     *
     * @param uuid      Player UUID.
     * @param filter    Achievement category to update.
     * @param delta     Progress amount to add.
     */
    public void addProgress(@NotNull UUID uuid, @NotNull AchievementCategory filter, long delta) {
        Map<String, AchievementProgress> progress = progressCache.get(uuid);
        if (progress == null) return;

        for (Achievement def : definitions.values()) {
            if (def.category() != filter) continue;
            if (!def.isValidInState(currentStateId)) continue;

            AchievementProgress ap = progress.computeIfAbsent(def.id(), AchievementProgress::fresh);
            if (ap.isUnlocked()) continue;

            long newProgress = ap.addProgress(delta);
            if (newProgress >= def.target()) {
                unlock(uuid, def, ap);
            }
        }
    }

    /**
     * Trigger a ONE_SHOT unlock for matching achievements.
     */
    public void triggerOneShot(@NotNull UUID uuid, @NotNull AchievementCategory filter) {
        Map<String, AchievementProgress> progress = progressCache.get(uuid);
        if (progress == null) return;

        for (Achievement def : definitions.values()) {
            if (def.category() != filter) continue;
            if (def.type() != AchievementType.ONE_SHOT) continue;
            if (!def.isValidInState(currentStateId)) continue;

            AchievementProgress ap = progress.computeIfAbsent(def.id(), AchievementProgress::fresh);
            if (!ap.isUnlocked()) unlock(uuid, def, ap);
        }
    }

    /**
     * Manually set progress (e.g. for balance-based achievements).
     */
    public void setProgress(@NotNull UUID uuid, @NotNull String achievementId, long value) {
        Map<String, AchievementProgress> progress = progressCache.get(uuid);
        if (progress == null) return;
        Achievement def = definitions.get(achievementId);
        if (def == null) return;

        AchievementProgress ap = progress.computeIfAbsent(achievementId, AchievementProgress::fresh);
        if (ap.isUnlocked()) return;
        ap.setProgress(value);
        if (value >= def.target()) unlock(uuid, def, ap);
    }

    private void unlock(@NotNull UUID uuid, @NotNull Achievement def,
                        @NotNull AchievementProgress ap) {
        ap.markUnlocked();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                storage.saveAchievementProgress(uuid, def.id(), ap.getProgress(),
                        true, ap.getUnlockedAt()));

        // Notify + broadcast (on main thread)
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(messages.achievementUnlocked(def.name()));
                if (config.isBroadcastUnlock()) {
                    Bukkit.broadcast(messages.broadcastAchievement(player.getName(), def.name()));
                }
            }
            // Award title
            if (def.titleReward() != null && titleManager != null) {
                titleManager.awardTitle(uuid, def.titleReward(), false);
            }
            // Stats
            stats.incrementAchievementsUnlocked(uuid);
        });
        log.fine("[Achievements] " + uuid + " unlocked: " + def.id());
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    @NotNull public Collection<Achievement> getDefinitions() { return definitions.values(); }
    @Nullable public Achievement getDefinition(@NotNull String id) { return definitions.get(id); }

    @NotNull public Map<String, AchievementProgress> getProgress(@NotNull UUID uuid) {
        return progressCache.getOrDefault(uuid, Map.of());
    }

    public int getUnlockedCount(@NotNull UUID uuid) {
        return (int) getProgress(uuid).values().stream().filter(AchievementProgress::isUnlocked).count();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private static @NotNull String str(@Nullable Object o) { return o == null ? "" : o.toString(); }
    private static long toLong(@Nullable Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        if (o != null) try { return Long.parseLong(o.toString()); } catch (NumberFormatException ignored) {}
        return def;
    }
    private static @NotNull Material parseMaterial(@NotNull String s) {
        Material m = Material.matchMaterial(s);
        return m != null ? m : Material.BOOK;
    }
}
