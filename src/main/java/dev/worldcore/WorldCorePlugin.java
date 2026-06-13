package dev.worldcore;

import dev.worldcore.achievement.AchievementManager;
import dev.worldcore.achievement.listeners.CombatListener;
import dev.worldcore.achievement.listeners.EconomyListener;
import dev.worldcore.achievement.listeners.ExternalEventListener;
import dev.worldcore.achievement.listeners.PlaytimeListener;
import dev.worldcore.api.WorldCoreAPI;
import dev.worldcore.commands.ExpeditionCommand;
import dev.worldcore.commands.WorldCoreCommand;
import dev.worldcore.config.MainConfig;
import dev.worldcore.config.MessageConfig;
import dev.worldcore.economy.hooks.VaultEconomyHook;
import dev.worldcore.market.*;
import dev.worldcore.market.modifiers.*;
import dev.worldcore.market.transactions.QuickShopProvider;
import dev.worldcore.expedition.ExpeditionManager;
import dev.worldcore.gui.util.GUIListener;
import dev.worldcore.hooks.*;
import dev.worldcore.state.WorldStateManager;
import dev.worldcore.statistics.StatisticsManager;
import dev.worldcore.storage.SqliteBackend;
import dev.worldcore.storage.StorageBackend;
import dev.worldcore.storage.YamlBackend;
import dev.worldcore.title.TitleManager;
import dev.worldcore.title.hooks.LuckPermsHook;
import dev.worldcore.title.hooks.TABHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * WorldCore Main Plugin Class.
 */
public final class WorldCorePlugin extends JavaPlugin implements Listener {

    private MainConfig             mainConfig;
    private MessageConfig          messageConfig;
    private StorageBackend         storage;
    private WorldStateManager      stateManager;
    private MarketManager          marketManager;
    private MarketConfig           marketConfig;
    private ExpeditionManager      expeditionManager;
    private AchievementManager     achievementManager;
    private TitleManager           titleManager;
    private StatisticsManager      statsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("expeditions.yml", false);
        saveResource("achievements.yml", false);
        saveResource("titles.yml", false);
        saveResource("market.yml", false);

        // Configs
        mainConfig    = new MainConfig(getLogger());
        messageConfig = new MessageConfig();
        reloadConfigs();

        // Storage
        if ("SQLITE".equalsIgnoreCase(mainConfig.getStorageType())) {
            storage = new SqliteBackend(getDataFolder(), getLogger());
        } else {
            storage = new YamlBackend(getDataFolder(), getLogger());
        }
        try { storage.init(); }
        catch (Exception e) { getLogger().severe("Storage init failed: " + e.getMessage()); }

        // State Engine
        stateManager = new WorldStateManager(this, getLogger());
        stateManager.init(mainConfig.getDefaultSeason());

        // Stats
        statsManager = new StatisticsManager(this, getLogger(), storage);

        // Market Engine
        marketConfig = new MarketConfig(getLogger());
        marketConfig.load(new File(getDataFolder(), "market.yml"));
        
        MarketTrendManager trendManager = new MarketTrendManager(this, getLogger(), marketConfig, storage);
        SupplyDemandTracker supplyDemandTracker = new SupplyDemandTracker(this, getLogger(), marketConfig, storage);
        
        marketManager = new MarketManager(getLogger(), marketConfig, trendManager, supplyDemandTracker);
        marketManager.registerProvider(new SeasonModifierProvider(marketConfig, stateManager));
        marketManager.registerProvider(new EventModifierProvider(marketConfig, stateManager));
        marketManager.registerProvider(new MarketTrendProvider(trendManager));
        marketManager.registerProvider(supplyDemandTracker);
        
        new MarketHistoryTracker(this, getLogger(), marketConfig, marketManager, storage);
        
        if (mainConfig.isEconomyEnabled()) {
            new QuickShopProvider(this, getLogger(), marketManager, supplyDemandTracker, statsManager);
        }

        // Expeditions
        expeditionManager = new ExpeditionManager(this, getLogger(), mainConfig, messageConfig, storage, statsManager);
        expeditionManager.loadDefinitions(new File(getDataFolder(), "expeditions.yml"));
        stateManager.registerReaction((prev, curr) -> expeditionManager.onStateChange(curr));
        // Provide hooks to expedition manager for rewards/costs
        VaultEconomyHook veh = new VaultEconomyHook(getLogger());
        expeditionManager.setVaultHook(veh);
        expeditionManager.setCratesHook(new CrazyCratesHook(getLogger()));
        expeditionManager.setPlayerPointsHook(new PlayerPointsHook(getLogger()));

        // Achievements
        achievementManager = new AchievementManager(this, getLogger(), mainConfig, messageConfig, storage, statsManager);
        achievementManager.loadDefinitions(new File(getDataFolder(), "achievements.yml"));
        stateManager.registerReaction((prev, curr) -> achievementManager.onStateChange(curr));
        
        // Listeners for achievements
        Bukkit.getPluginManager().registerEvents(new CombatListener(achievementManager), this);
        new EconomyListener(this, achievementManager, veh);
        new ExternalEventListener(this, achievementManager);
        new PlaytimeListener(this, achievementManager);

        // Titles
        titleManager = new TitleManager(this, getLogger(), mainConfig, messageConfig, storage, statsManager);
        titleManager.loadDefinitions(new File(getDataFolder(), "titles.yml"));
        titleManager.setTabHook(new TABHook(getLogger()));
        titleManager.setLpHook(new LuckPermsHook(getLogger()));
        achievementManager.setTitleManager(titleManager); // link for rewards

        // API setup
        new WorldCoreAPI(this);

        // External Hooks
        new AeternumSeasonsHook(this, stateManager, getLogger()).load();
        new WorldEventsHook(this, stateManager, getLogger()).load();
        new DiscoveryCoreHook(getLogger(), achievementManager);
        new RarityCoreHook(getLogger());
        new ODailyQuestsHook(getLogger());

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new WorldCorePAPI(this).register();
        }

        // Commands
        getCommand("worldcore").setExecutor(new WorldCoreCommand(this));
        getCommand("expedition").setExecutor(new ExpeditionCommand(this));

        // Listeners
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new GUIListener(), this);

        // Load online players (on reload)
        for (Player p : Bukkit.getOnlinePlayers()) {
            loadPlayerData(p.getUniqueId());
        }

        getLogger().info("WorldCore initialized successfully.");
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            unloadPlayerData(p.getUniqueId());
        }
        statsManager.saveAll();
        if (storage != null) storage.close();
        getLogger().info("WorldCore disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadConfigs();
        marketConfig.load(new File(getDataFolder(), "market.yml"));
        expeditionManager.loadDefinitions(new File(getDataFolder(), "expeditions.yml"));
        achievementManager.loadDefinitions(new File(getDataFolder(), "achievements.yml"));
        titleManager.loadDefinitions(new File(getDataFolder(), "titles.yml"));
        stateManager.reevaluate();
    }

    private void reloadConfigs() {
        mainConfig.reload(getConfig());
        org.bukkit.configuration.file.FileConfiguration msgCfg = 
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        messageConfig.reload(msgCfg);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer().getUniqueId());
        
        // Re-apply active title on join
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (event.getPlayer().isOnline()) {
                String active = titleManager.getActiveTitle(event.getPlayer().getUniqueId());
                if (active != null) titleManager.setActiveTitle(event.getPlayer(), active);
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        unloadPlayerData(event.getPlayer().getUniqueId());
    }

    private void loadPlayerData(java.util.UUID uuid) {
        statsManager.loadPlayer(uuid);
        expeditionManager.loadPlayer(uuid);
        achievementManager.loadPlayer(uuid);
        titleManager.loadPlayer(uuid);
    }

    private void unloadPlayerData(java.util.UUID uuid) {
        statsManager.unloadPlayer(uuid);
        expeditionManager.unloadPlayer(uuid);
        achievementManager.unloadPlayer(uuid);
        titleManager.unloadPlayer(uuid);
    }

    // Getters
    public MainConfig getMainConfig() { return mainConfig; }
    public MessageConfig getMessageConfig() { return messageConfig; }
    public StorageBackend getStorage() { return storage; }
    public WorldStateManager getStateManager() { return stateManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public ExpeditionManager getExpeditionManager() { return expeditionManager; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public TitleManager getTitleManager() { return titleManager; }
    public StatisticsManager getStatsManager() { return statsManager; }
}
