package dev.worldcore.market;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

/**
 * Parses market.yml.
 */
public final class MarketConfig {

    private final Logger log;

    // Supply & Demand
    private double maxPositiveModifier = 1.5;
    private double maxNegativeModifier = 0.5;
    private long   historyIntervalTicks = 72000L;

    // Categories
    private final Map<String, Set<Material>> categories = new HashMap<>();
    private final Map<Material, String>      materialToCategory = new HashMap<>();

    // Trends
    private final Map<String, MarketTrendDef> trends = new HashMap<>();

    // Modifiers
    private final Map<String, Map<String, Double>> seasonModifiers = new HashMap<>(); // season -> (category -> mod)
    private final Map<String, Map<String, Double>> eventModifiers  = new HashMap<>(); // event -> (category -> mod)

    public MarketConfig(@NotNull Logger log) {
        this.log = log;
    }

    public void load(@NotNull File file) {
        categories.clear();
        materialToCategory.clear();
        trends.clear();
        seasonModifiers.clear();
        eventModifiers.clear();

        if (!file.exists()) {
            log.warning("[Market] market.yml not found.");
            return;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // Settings
        maxPositiveModifier  = cfg.getDouble("supply-demand.max-positive-modifier", 1.5);
        maxNegativeModifier  = cfg.getDouble("supply-demand.max-negative-modifier", 0.5);
        historyIntervalTicks = cfg.getLong("history.snapshot-interval-ticks", 72000L);

        // Categories
        ConfigurationSection catSec = cfg.getConfigurationSection("categories");
        if (catSec != null) {
            for (String cat : catSec.getKeys(false)) {
                List<String> mats = catSec.getStringList(cat);
                Set<Material> matSet = new HashSet<>();
                for (String m : mats) {
                    Material mat = Material.matchMaterial(m);
                    if (mat != null) {
                        matSet.add(mat);
                        materialToCategory.put(mat, cat.toUpperCase());
                    }
                }
                categories.put(cat.toUpperCase(), matSet);
            }
        }

        // Trends
        ConfigurationSection trendSec = cfg.getConfigurationSection("trends");
        if (trendSec != null) {
            for (String tId : trendSec.getKeys(false)) {
                ConfigurationSection ts = trendSec.getConfigurationSection(tId);
                if (ts == null) continue;
                double mult = ts.getDouble("multiplier", 1.0);
                long   dur  = ts.getLong("duration-days", 7);
                int    ch   = ts.getInt("chance", 10);
                
                Map<String, Double> catMods = new HashMap<>();
                ConfigurationSection cmSec = ts.getConfigurationSection("category-modifiers");
                if (cmSec != null) {
                    for (String c : cmSec.getKeys(false)) {
                        catMods.put(c.toUpperCase(), cmSec.getDouble(c));
                    }
                }
                trends.put(tId, new MarketTrendDef(tId, mult, dur, ch, catMods));
            }
        }

        // Season Modifiers
        loadModifiers(cfg, "season-modifiers", seasonModifiers);
        // Event Modifiers
        loadModifiers(cfg, "event-modifiers", eventModifiers);

        log.info("[Market] Loaded " + categories.size() + " categories and " + trends.size() + " trends.");
    }

    private void loadModifiers(FileConfiguration cfg, String key, Map<String, Map<String, Double>> target) {
        ConfigurationSection sec = cfg.getConfigurationSection(key);
        if (sec == null) return;
        for (String stateId : sec.getKeys(false)) {
            ConfigurationSection cs = sec.getConfigurationSection(stateId);
            if (cs == null) continue;
            Map<String, Double> mods = new HashMap<>();
            for (String cat : cs.getKeys(false)) {
                mods.put(cat.toUpperCase(), cs.getDouble(cat));
            }
            target.put(stateId.toLowerCase(), mods);
        }
    }

    public double getMaxPositiveModifier() { return maxPositiveModifier; }
    public double getMaxNegativeModifier() { return maxNegativeModifier; }
    public long getHistoryIntervalTicks()  { return historyIntervalTicks; }

    @Nullable public String getCategory(@NotNull Material material) {
        return materialToCategory.get(material);
    }

    @NotNull public Collection<MarketTrendDef> getTrends() {
        return trends.values();
    }
    
    @Nullable public MarketTrendDef getTrend(@NotNull String id) {
        return trends.get(id);
    }

    public double getSeasonModifier(@NotNull String seasonId, @Nullable String category) {
        if (category == null) return 1.0;
        return seasonModifiers.getOrDefault(seasonId.toLowerCase(), Map.of()).getOrDefault(category, 1.0);
    }

    public double getEventModifier(@NotNull String eventId, @Nullable String category) {
        if (category == null) return 1.0;
        return eventModifiers.getOrDefault(eventId.toLowerCase(), Map.of()).getOrDefault(category, 1.0);
    }

    public record MarketTrendDef(
            @NotNull String id,
            double globalMultiplier,
            long durationDays,
            int chance,
            @NotNull Map<String, Double> categoryModifiers
    ) {}
}
