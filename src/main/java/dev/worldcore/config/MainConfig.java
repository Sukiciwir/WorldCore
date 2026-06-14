package dev.worldcore.config;

import dev.worldcore.state.SeasonState;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Typed wrapper around WorldCore's {@code config.yml}.
 */
public final class MainConfig {

    private final Logger log;

    // Storage
    private String  storageType    = "SQLITE";
    private boolean autoMigrate    = true;

    // World state
    private SeasonState defaultSeason       = SeasonState.SPRING;
    private int         pollIntervalTicks   = 0;

    // Economy
    private boolean economyEnabled = true;
    private String  economyHook    = "AUTO";

    // Expeditions
    private boolean expeditionsEnabled     = true;
    private int     defaultExpeditionSlots = 1;
    private int     expeditionCheckTicks   = 600;

    private boolean dailyRotationEnabled   = true;
    private int     dailyGeneralCount      = 4;
    private int     dailySeasonalCount     = 2;
    private int     dailyEventCount        = 1;

    // Achievements
    private boolean achievementsEnabled   = true;
    private boolean broadcastUnlock       = true;

    // Titles
    private boolean titlesEnabled    = true;
    private boolean pushTab          = true;
    private boolean pushLuckPerms    = false;
    private String  tabPrefixFormat  = "&7[&f{title}&7] ";
    private String  lpPrefixFormat   = "&7[{title}]&r ";

    // GUI
    private String guiMainTitle        = "<gradient:#6a11cb:#2575fc>⚡ WorldCore</gradient>";
    private String guiStateTitle       = "<dark_purple>🌍 World State";
    private String guiAchievements     = "<gold>🏆 Achievements";
    private String guiExpeditions      = "<aqua>⛵ Expeditions";
    private String guiTitles           = "<yellow>👑 Titles";
    private String guiStatistics       = "<green>📊 Statistics";

    public MainConfig(@NotNull Logger log) { this.log = log; }

    public void reload(@NotNull FileConfiguration cfg) {
        storageType    = cfg.getString("storage.type", "SQLITE");
        autoMigrate    = cfg.getBoolean("storage.auto-migrate", true);

        String seasonName = cfg.getString("world-state.default-season", "SPRING");
        defaultSeason     = SeasonState.fromName(seasonName);
        pollIntervalTicks = cfg.getInt("world-state.poll-interval-ticks", 0);

        economyEnabled = cfg.getBoolean("economy-modifiers.enabled", true);
        economyHook    = cfg.getString("economy-modifiers.hook", "AUTO");

        expeditionsEnabled     = cfg.getBoolean("expeditions.enabled", true);
        defaultExpeditionSlots = cfg.getInt("expeditions.default-slots", 1);
        expeditionCheckTicks   = cfg.getInt("expeditions.check-interval-ticks", 600);

        dailyRotationEnabled   = cfg.getBoolean("expeditions.daily-rotation.enabled", true);
        dailyGeneralCount      = cfg.getInt("expeditions.daily-rotation.pool-composition.general", 4);
        dailySeasonalCount     = cfg.getInt("expeditions.daily-rotation.pool-composition.seasonal", 2);
        dailyEventCount        = cfg.getInt("expeditions.daily-rotation.pool-composition.event", 1);

        achievementsEnabled = cfg.getBoolean("achievements.enabled", true);
        broadcastUnlock     = cfg.getBoolean("achievements.broadcast-unlock", true);

        titlesEnabled  = cfg.getBoolean("titles.enabled", true);
        pushTab        = cfg.getBoolean("titles.push-tab", true);
        pushLuckPerms  = cfg.getBoolean("titles.push-luckperms", false);
        tabPrefixFormat = cfg.getString("titles.tab-prefix-format", "&7[&f{title}&7] ");
        lpPrefixFormat  = cfg.getString("titles.luckperms-prefix-format", "&7[{title}]&r ");

        guiMainTitle    = cfg.getString("gui.main-title", guiMainTitle);
        guiStateTitle   = cfg.getString("gui.world-state-title", guiStateTitle);
        guiAchievements = cfg.getString("gui.achievements-title", guiAchievements);
        guiExpeditions  = cfg.getString("gui.expeditions-title", guiExpeditions);
        guiTitles       = cfg.getString("gui.titles-title", guiTitles);
        guiStatistics   = cfg.getString("gui.statistics-title", guiStatistics);

        log.info("[Config] Loaded — storage=" + storageType
                + ", defaultSeason=" + defaultSeason.getId()
                + ", economy=" + economyEnabled);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    @NotNull public String  getStorageType()            { return storageType; }
    public boolean          isAutoMigrate()             { return autoMigrate; }
    @NotNull public SeasonState getDefaultSeason()      { return defaultSeason; }
    public int              getPollIntervalTicks()      { return pollIntervalTicks; }
    public boolean          isEconomyEnabled()          { return economyEnabled; }
    @NotNull public String  getEconomyHook()            { return economyHook; }
    public boolean          isExpeditionsEnabled()      { return expeditionsEnabled; }
    public int              getDefaultExpeditionSlots() { return defaultExpeditionSlots; }
    public int              getExpeditionCheckTicks()   { return expeditionCheckTicks; }
    
    public boolean          isDailyRotationEnabled()    { return dailyRotationEnabled; }
    public int              getDailyGeneralCount()      { return dailyGeneralCount; }
    public int              getDailySeasonalCount()     { return dailySeasonalCount; }
    public int              getDailyEventCount()        { return dailyEventCount; }

    public boolean          isAchievementsEnabled()     { return achievementsEnabled; }
    public boolean          isBroadcastUnlock()         { return broadcastUnlock; }
    public boolean          isTitlesEnabled()           { return titlesEnabled; }
    public boolean          isPushTab()                 { return pushTab; }
    public boolean          isPushLuckPerms()           { return pushLuckPerms; }
    @NotNull public String  getTabPrefixFormat()        { return tabPrefixFormat; }
    @NotNull public String  getLpPrefixFormat()         { return lpPrefixFormat; }
    @NotNull public String  getGuiMainTitle()           { return guiMainTitle; }
    @NotNull public String  getGuiStateTitle()          { return guiStateTitle; }
    @NotNull public String  getGuiAchievementsTitle()   { return guiAchievements; }
    @NotNull public String  getGuiExpeditionsTitle()    { return guiExpeditions; }
    @NotNull public String  getGuiTitlesTitle()         { return guiTitles; }
    @NotNull public String  getGuiStatisticsTitle()     { return guiStatistics; }
}
