package dev.worldcore.expedition;

import dev.worldcore.config.MainConfig;
import dev.worldcore.config.MessageConfig;
import dev.worldcore.economy.hooks.VaultEconomyHook;
import dev.worldcore.hooks.CrazyCratesHook;
import dev.worldcore.hooks.PlayerPointsHook;
import dev.worldcore.state.WorldState;
import dev.worldcore.statistics.StatisticsManager;
import dev.worldcore.storage.StorageBackend;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.Calendar;

/**
 * Manages the full expedition lifecycle:
 * <ul>
 *   <li>Loading expedition definitions from {@code expeditions.yml}</li>
 *   <li>Starting expeditions (cost deduction + DB write)</li>
 *   <li>Claiming expeditions (reward delivery + DB write)</li>
 *   <li>Per-player slot management</li>
 * </ul>
 */
public final class ExpeditionManager {

    private final Plugin           plugin;
    private final Logger           log;
    private final MainConfig       config;
    private final MessageConfig    messages;
    private final StorageBackend   storage;
    private final StatisticsManager stats;

    /** All expedition definitions keyed by ID. */
    private final Map<String, Expedition> definitions = new LinkedHashMap<>();

    /** Per-player active expeditions (in-memory cache). */
    private final Map<UUID, List<ActiveExpedition>> activeCache = new ConcurrentHashMap<>();

    /** Currently active world state ID (updated by WorldStateManager hook). */
    private volatile String currentStateId = "spring";

    // Optional economy hooks
    private @Nullable VaultEconomyHook vaultHook;
    private @Nullable CrazyCratesHook  cratesHook;
    private @Nullable PlayerPointsHook ppHook;

    // Daily Rotation
    private final Set<String> dailyPool = new HashSet<>();
    private int currentDayOfYear = -1;
    private final File dailyFile;

    public ExpeditionManager(@NotNull Plugin plugin, @NotNull Logger log,
                             @NotNull MainConfig config, @NotNull MessageConfig messages,
                             @NotNull StorageBackend storage, @NotNull StatisticsManager stats) {
        this.plugin   = plugin;
        this.log      = log;
        this.config   = config;
        this.messages = messages;
        this.storage  = storage;
        this.stats    = stats;
        
        this.dailyFile = new File(plugin.getDataFolder(), "daily_pool.yml");
        loadDailyPool();
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkDailyRoll, 100L, 1200L);
    }

    // ─── Dependency Injection ─────────────────────────────────────────────────
    public void setVaultHook(@Nullable VaultEconomyHook hook) { this.vaultHook  = hook; }
    public void setCratesHook(@Nullable CrazyCratesHook hook) { this.cratesHook = hook; }
    public void setPlayerPointsHook(@Nullable PlayerPointsHook hook) { this.ppHook = hook; }

    // ─── Config Loading ───────────────────────────────────────────────────────

    public void loadDefinitions(@NotNull File expeditionsFile) {
        definitions.clear();
        if (!expeditionsFile.exists()) {
            log.warning("[Expeditions] expeditions.yml not found.");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(expeditionsFile);
        List<Map<?, ?>> list = yaml.getMapList("expeditions");

        for (Map<?, ?> raw : list) {
            try {
                Expedition exp = parseExpedition(raw);
                definitions.put(exp.id(), exp);
            } catch (Exception e) {
                log.warning("[Expeditions] Failed to parse expedition: " + e.getMessage());
            }
        }
        log.info("[Expeditions] Loaded " + definitions.size() + " expedition definitions.");
        checkDailyRoll();
    }

    @NotNull
    private Expedition parseExpedition(@NotNull Map<?, ?> raw) {
        String id          = str(raw.get("id"));
        String name        = str(raw.get("name"));
        String desc        = str(raw.get("description"));
        String catStr      = str(raw.get("category"));
        long   duration    = toLong(raw.get("duration-seconds"), 3600);
        ExpeditionCategory cat = parseCategory(catStr);

        // World states
        Set<String> worldStates = new HashSet<>();
        Object wsObj = raw.get("world-states");
        if (wsObj instanceof List<?> wsList) {
            for (Object ws : wsList) worldStates.add(ws.toString().toLowerCase());
        }

        // Costs
        List<ExpeditionCost> costs = new ArrayList<>();
        Object costsObj = raw.get("costs");
        if (costsObj instanceof List<?> costList) {
            for (Object c : costList) {
                if (c instanceof Map<?, ?> cm) costs.add(parseCost(cm));
            }
        }

        // Rewards
        List<ExpeditionReward> rewards = new ArrayList<>();
        Object rewardsObj = raw.get("rewards");
        if (rewardsObj instanceof List<?> rewardList) {
            for (Object r : rewardList) {
                if (r instanceof Map<?, ?> rm) rewards.add(parseReward(rm));
            }
        }

        return new Expedition(id, name, desc, cat, worldStates, duration,
                Collections.unmodifiableList(costs), Collections.unmodifiableList(rewards));
    }

    @NotNull private ExpeditionCost parseCost(@NotNull Map<?, ?> m) {
        String typeStr = str(m.get("type")).toUpperCase();
        double amount  = toDouble(m.get("amount"), 0);
        Material mat   = null;
        if ("ITEM".equals(typeStr)) {
            mat = Material.matchMaterial(str(m.get("material")));
        }
        ExpeditionCost.CostType type;
        try { type = ExpeditionCost.CostType.valueOf(typeStr); }
        catch (Exception e) { type = ExpeditionCost.CostType.MONEY; }
        return new ExpeditionCost(type, amount, mat);
    }

    @NotNull private ExpeditionReward parseReward(@NotNull Map<?, ?> m) {
        String typeStr = str(m.get("type")).toUpperCase();
        double amount  = toDouble(m.get("amount"), 1);
        int    chance  = (int) toDouble(m.get("chance"), 100);
        Material mat   = null;
        String crateId = null;
        if ("ITEM".equals(typeStr))      mat    = Material.matchMaterial(str(m.get("material")));
        if ("CRATE_KEY".equals(typeStr)) crateId = str(m.get("crate"));
        ExpeditionReward.RewardType type;
        try { type = ExpeditionReward.RewardType.valueOf(typeStr); }
        catch (Exception e) { type = ExpeditionReward.RewardType.MONEY; }
        return new ExpeditionReward(type, amount, mat, crateId, chance);
    }

    // ─── State Update ─────────────────────────────────────────────────────────

    public void onStateChange(@NotNull WorldState state) {
        currentStateId = state.getId();
        checkDailyRoll(); // Re-roll if necessary based on new state? Or just let daily handle it.
    }

    // ─── Daily Rotation ───────────────────────────────────────────────────────

    private void checkDailyRoll() {
        if (!config.isDailyRotationEnabled()) return;

        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_YEAR) + (cal.get(Calendar.YEAR) * 365);
        if (today != currentDayOfYear) {
            rollDailyPool(today);
        }
    }

    private void rollDailyPool(int today) {
        currentDayOfYear = today;
        dailyPool.clear();

        List<Expedition> generals = new ArrayList<>();
        List<Expedition> seasonals = new ArrayList<>();
        List<Expedition> events = new ArrayList<>();

        for (Expedition e : definitions.values()) {
            if (e.worldStates().isEmpty()) {
                generals.add(e);
            } else if (e.worldStates().contains("spring") || e.worldStates().contains("summer") ||
                       e.worldStates().contains("autumn") || e.worldStates().contains("winter")) {
                if (e.isAvailable(currentStateId)) seasonals.add(e);
            } else {
                if (e.isAvailable(currentStateId)) events.add(e);
            }
        }

        Collections.shuffle(generals);
        Collections.shuffle(seasonals);
        Collections.shuffle(events);

        for (int i = 0; i < config.getDailyGeneralCount() && i < generals.size(); i++) {
            dailyPool.add(generals.get(i).id());
        }
        for (int i = 0; i < config.getDailySeasonalCount() && i < seasonals.size(); i++) {
            dailyPool.add(seasonals.get(i).id());
        }
        for (int i = 0; i < config.getDailyEventCount() && i < events.size(); i++) {
            dailyPool.add(events.get(i).id());
        }

        saveDailyPool();
    }

    private void loadDailyPool() {
        if (!dailyFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dailyFile);
        currentDayOfYear = yaml.getInt("day-of-year", -1);
        List<String> list = yaml.getStringList("pool");
        dailyPool.addAll(list);
    }

    private void saveDailyPool() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("day-of-year", currentDayOfYear);
        yaml.set("pool", new ArrayList<>(dailyPool));
        try {
            yaml.save(dailyFile);
        } catch (Exception e) {
            log.warning("Failed to save daily pool: " + e.getMessage());
        }
    }

    public Set<String> getDailyPool() {
        return dailyPool;
    }

    // ─── Player Cache ─────────────────────────────────────────────────────────

    public void loadPlayer(@NotNull UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<StorageBackend.ActiveExpeditionRow> rows = storage.loadExpeditions(uuid);
            List<ActiveExpedition> active = new ArrayList<>();
            for (StorageBackend.ActiveExpeditionRow row : rows) {
                if (!row.claimed()) active.add(new ActiveExpedition(row.expeditionId(), row.startedAt(), row.endsAt()));
            }
            activeCache.put(uuid, active);
        });
    }

    public void unloadPlayer(@NotNull UUID uuid) {
        activeCache.remove(uuid);
    }

    // ─── Slots ────────────────────────────────────────────────────────────────

    public int getMaxSlots(@NotNull Player player) {
        // Unlock all 9 top row slots by default for all players
        return 9;
    }

    public int getUsedSlots(@NotNull UUID uuid) {
        List<ActiveExpedition> active = activeCache.getOrDefault(uuid, List.of());
        return (int) active.stream().filter(a -> !a.isReady()).count();
    }

    // ─── Start Expedition ─────────────────────────────────────────────────────

    public enum StartResult {
        SUCCESS, NOT_FOUND, NOT_AVAILABLE, NO_SLOTS, INSUFFICIENT_COST
    }

    @NotNull
    public StartResult startExpedition(@NotNull Player player, @NotNull String expeditionId) {
        Expedition def = definitions.get(expeditionId.toLowerCase());
        if (def == null) return StartResult.NOT_FOUND;
        if (!def.isAvailable(currentStateId)) return StartResult.NOT_AVAILABLE;

        int max  = getMaxSlots(player);
        int used = getUsedSlots(player.getUniqueId());
        if (used >= max) return StartResult.NO_SLOTS;

        // Check costs
        if (!canAffordCosts(player, def.costs())) return StartResult.INSUFFICIENT_COST;

        // Deduct costs
        deductCosts(player, def.costs());

        // Create active expedition
        long now     = System.currentTimeMillis();
        long endsAt  = now + (def.durationSeconds() * 1000L);
        ActiveExpedition ae = new ActiveExpedition(expeditionId, now, endsAt);

        activeCache.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(ae);
        Bukkit.getScheduler().runTaskAsynchronously(plugin,
                () -> storage.saveExpedition(player.getUniqueId(), expeditionId, now, endsAt));

        // Stats
        stats.incrementExpeditionsStarted(player.getUniqueId());

        player.sendMessage(messages.expeditionStarted(def.name(), ae.formatRemaining()));
        log.fine("[Expedition] " + player.getName() + " started: " + expeditionId);
        return StartResult.SUCCESS;
    }

    // ─── Claim Expedition ─────────────────────────────────────────────────────

    public enum ClaimResult {
        SUCCESS, NO_ACTIVE, NOT_READY
    }

    /** Claim all ready expeditions for a player. Returns number claimed. */
    public int claimAll(@NotNull Player player) {
        List<ActiveExpedition> active = activeCache.getOrDefault(player.getUniqueId(), new ArrayList<>());
        List<ActiveExpedition> ready  = active.stream().filter(ActiveExpedition::isReady).toList();
        if (ready.isEmpty()) return 0;

        for (ActiveExpedition ae : ready) {
            deliverRewards(player, ae.expeditionId());
            active.remove(ae);
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> storage.claimExpedition(player.getUniqueId(), ae.expeditionId(), ae.startedAt()));
            stats.incrementExpeditionsCompleted(player.getUniqueId());
        }
        return ready.size();
    }

    private void deliverRewards(@NotNull Player player, @NotNull String expeditionId) {
        Expedition def = definitions.get(expeditionId);
        if (def == null) return;
        for (ExpeditionReward reward : def.rewards()) {
            if (!reward.roll()) continue;
            deliverReward(player, reward);
        }
        player.sendMessage(messages.expeditionClaimed(def.name()));
    }

    private void deliverReward(@NotNull Player player, @NotNull ExpeditionReward reward) {
        switch (reward.type()) {
            case MONEY -> {
                if (vaultHook != null && vaultHook.isReady() && vaultHook.getEconomy() != null) {
                    vaultHook.getEconomy().depositPlayer(player, reward.amount());
                }
            }
            case ITEM -> {
                if (reward.material() != null) {
                    ItemStack item = new ItemStack(reward.material(), (int) reward.amount());
                    player.getInventory().addItem(item).forEach((slot, leftover) ->
                            player.getWorld().dropItem(player.getLocation(), leftover));
                }
            }
            case CRATE_KEY -> {
                if (cratesHook != null && cratesHook.isReady() && reward.crateId() != null) {
                    cratesHook.giveKey(player, reward.crateId(), (int) reward.amount());
                }
            }
            case PLAYER_POINTS -> {
                if (ppHook != null && ppHook.isReady()) {
                    ppHook.give(player, (int) reward.amount());
                }
            }
        }
    }

    // ─── Cost Checks ──────────────────────────────────────────────────────────

    private boolean canAffordCosts(@NotNull Player player, @NotNull List<ExpeditionCost> costs) {
        for (ExpeditionCost cost : costs) {
            if (!canAfford(player, cost)) return false;
        }
        return true;
    }

    private boolean canAfford(@NotNull Player player, @NotNull ExpeditionCost cost) {
        return switch (cost.type()) {
            case MONEY -> {
                Economy eco = vaultHook != null ? vaultHook.getEconomy() : null;
                yield eco != null && eco.has(player, cost.amount());
            }
            case PLAYER_POINTS -> ppHook != null && ppHook.isReady()
                    && ppHook.getPoints(player) >= (int) cost.amount();
            case ITEM -> cost.material() != null
                    && player.getInventory().containsAtLeast(new ItemStack(cost.material()), (int) cost.amount());
        };
    }

    private void deductCosts(@NotNull Player player, @NotNull List<ExpeditionCost> costs) {
        for (ExpeditionCost cost : costs) {
            switch (cost.type()) {
                case MONEY -> {
                    Economy eco = vaultHook != null ? vaultHook.getEconomy() : null;
                    if (eco != null) eco.withdrawPlayer(player, cost.amount());
                }
                case PLAYER_POINTS -> {
                    if (ppHook != null) ppHook.take(player, (int) cost.amount());
                }
                case ITEM -> {
                    if (cost.material() != null) {
                        player.getInventory().removeItem(new ItemStack(cost.material(), (int) cost.amount()));
                    }
                }
            }
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /** @return All loaded expedition definitions. */
    @NotNull public Collection<Expedition> getDefinitions() { return definitions.values(); }

    /** @return Definition by ID, or null. */
    @Nullable public Expedition getDefinition(@NotNull String id) { return definitions.get(id.toLowerCase()); }

    /** @return Active expeditions for a player (in-memory cache). */
    @NotNull public List<ActiveExpedition> getActiveExpeditions(@NotNull UUID uuid) {
        return activeCache.getOrDefault(uuid, List.of());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static @NotNull String str(@Nullable Object o) { return o == null ? "" : o.toString(); }
    private static double toDouble(@Nullable Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        if (o != null) try { return Double.parseDouble(o.toString()); } catch (NumberFormatException ignored) {}
        return def;
    }
    private static long toLong(@Nullable Object o, long def) { return (long) toDouble(o, def); }
    private static @NotNull ExpeditionCategory parseCategory(@NotNull String s) {
        try { return ExpeditionCategory.valueOf(s.toUpperCase()); }
        catch (Exception e) { return ExpeditionCategory.CUSTOM; }
    }
}
