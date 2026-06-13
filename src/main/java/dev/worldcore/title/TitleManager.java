package dev.worldcore.title;

import dev.worldcore.config.MainConfig;
import dev.worldcore.config.MessageConfig;
import dev.worldcore.statistics.StatisticsManager;
import dev.worldcore.storage.StorageBackend;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
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
 * Manages cosmetic title unlocking, equipping, and display propagation.
 *
 * <p>Titles are awarded by:
 * <ul>
 *   <li>Achievement unlocks (via {@link #awardTitle}).</li>
 *   <li>Admin commands.</li>
 * </ul>
 *
 * <p>Display is pushed to TAB (via reflection hook) and LuckPerms (optional).
 */
public final class TitleManager {

    private final Plugin        plugin;
    private final Logger        log;
    private final MainConfig    config;
    private final MessageConfig messages;
    private final StorageBackend storage;
    private final StatisticsManager stats;

    /** All title definitions, keyed by ID. */
    private final Map<String, Title> definitions = new LinkedHashMap<>();

    /** Per-player unlocked titles (in-memory cache). */
    private final Map<UUID, Set<String>>  unlockedTitles = new ConcurrentHashMap<>();

    /** Per-player active title (null = none). */
    private final Map<UUID, String>       activeTitles   = new ConcurrentHashMap<>();

    // Optional display hooks
    private @Nullable dev.worldcore.title.hooks.TABHook       tabHook;
    private @Nullable dev.worldcore.title.hooks.LuckPermsHook  lpHook;

    public TitleManager(@NotNull Plugin plugin, @NotNull Logger log,
                        @NotNull MainConfig config, @NotNull MessageConfig messages,
                        @NotNull StorageBackend storage, @NotNull StatisticsManager stats) {
        this.plugin   = plugin;
        this.log      = log;
        this.config   = config;
        this.messages = messages;
        this.storage  = storage;
        this.stats    = stats;
    }

    public void setTabHook(@Nullable dev.worldcore.title.hooks.TABHook hook) { this.tabHook = hook; }
    public void setLpHook(@Nullable dev.worldcore.title.hooks.LuckPermsHook hook)  { this.lpHook = hook; }

    // ─── Config Loading ───────────────────────────────────────────────────────

    public void loadDefinitions(@NotNull File titlesFile) {
        definitions.clear();
        if (!titlesFile.exists()) { log.warning("[Titles] titles.yml not found."); return; }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(titlesFile);
        ConfigurationSection sec = yaml.getConfigurationSection("titles");
        if (sec == null) return;

        for (String id : sec.getKeys(false)) {
            ConfigurationSection ts = sec.getConfigurationSection(id);
            if (ts == null) continue;
            definitions.put(id, new Title(
                    id,
                    ts.getString("display", id),
                    ts.getString("prefix", ""),
                    ts.getString("description", ""),
                    ts.getBoolean("seasonal", false)
            ));
        }
        log.info("[Titles] Loaded " + definitions.size() + " title definitions.");
    }

    // ─── Player Cache ─────────────────────────────────────────────────────────

    public void loadPlayer(@NotNull UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<String> unlocked = storage.loadUnlockedTitles(uuid);
            String       active   = storage.loadActiveTitle(uuid);
            unlockedTitles.put(uuid, new HashSet<>(unlocked));
            if (active != null) activeTitles.put(uuid, active);
        });
    }

    public void unloadPlayer(@NotNull UUID uuid) {
        unlockedTitles.remove(uuid);
        activeTitles.remove(uuid);
    }

    // ─── Award ────────────────────────────────────────────────────────────────

    /**
     * Award a title to a player.
     *
     * @param uuid     Player UUID.
     * @param titleId  Title to award.
     * @param silent   If true, no notification is sent.
     */
    public void awardTitle(@NotNull UUID uuid, @NotNull String titleId, boolean silent) {
        Title title = definitions.get(titleId);
        if (title == null) return;

        Set<String> unlocked = unlockedTitles.computeIfAbsent(uuid, k -> new HashSet<>());
        if (unlocked.contains(titleId)) return; // already owned

        unlocked.add(titleId);
        long now = System.currentTimeMillis();
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> storage.saveTitle(uuid, titleId, now));
        stats.incrementTitlesUnlocked(uuid);

        if (!silent) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.sendMessage(messages.titleAwarded(title.display()));
            });
        }
        log.fine("[Titles] Awarded " + titleId + " to " + uuid);
    }

    // ─── Set Active ───────────────────────────────────────────────────────────

    public boolean setActiveTitle(@NotNull Player player, @NotNull String titleId) {
        Set<String> unlocked = unlockedTitles.getOrDefault(player.getUniqueId(), Set.of());
        if (!unlocked.contains(titleId)) {
            player.sendMessage(messages.titleNotOwned());
            return false;
        }
        Title title = definitions.get(titleId);
        if (title == null) {
            player.sendMessage(messages.titleNotFound());
            return false;
        }
        activeTitles.put(player.getUniqueId(), titleId);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> storage.saveActiveTitle(player.getUniqueId(), titleId));

        pushDisplay(player, title);
        player.sendMessage(messages.titleSet(title.display()));
        return true;
    }

    public void clearActiveTitle(@NotNull Player player) {
        activeTitles.remove(player.getUniqueId());
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> storage.saveActiveTitle(player.getUniqueId(), null));
        pushDisplay(player, null);
        player.sendMessage(messages.titleCleared());
    }

    // ─── Display Push ─────────────────────────────────────────────────────────

    private void pushDisplay(@NotNull Player player, @Nullable Title title) {
        String prefix = title != null ? config.getTabPrefixFormat()
                .replace("{title}", stripMini(title.prefix())) : "";

        if (config.isPushTab() && tabHook != null && tabHook.isReady()) {
            tabHook.setPrefix(player, prefix);
        }
        if (config.isPushLuckPerms() && lpHook != null && lpHook.isReady()) {
            String lpPrefix = title != null ? config.getLpPrefixFormat()
                    .replace("{title}", stripMini(title.display())) : "";
            lpHook.setPrefix(player, lpPrefix);
        }
    }

    /** Strip MiniMessage tags for plain-text contexts (TAB/LP). */
    private static @NotNull String stripMini(@NotNull String s) {
        return s.replaceAll("<[^>]+>", "");
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    @NotNull public Collection<Title> getDefinitions() { return definitions.values(); }
    @Nullable public Title getDefinition(@NotNull String id) { return definitions.get(id); }

    @NotNull public Set<String> getUnlockedTitles(@NotNull UUID uuid) {
        return Collections.unmodifiableSet(unlockedTitles.getOrDefault(uuid, Set.of()));
    }

    @Nullable public String getActiveTitle(@NotNull UUID uuid) {
        return activeTitles.get(uuid);
    }

    @Nullable public Title getActiveTitleObj(@NotNull UUID uuid) {
        String id = activeTitles.get(uuid);
        return id != null ? definitions.get(id) : null;
    }
}
