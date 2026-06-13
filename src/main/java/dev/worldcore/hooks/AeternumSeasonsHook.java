package dev.worldcore.hooks;

import dev.worldcore.state.SeasonState;
import dev.worldcore.state.WorldState;
import dev.worldcore.state.WorldStateManager;
import dev.worldcore.state.WorldStateType;
import Kinkin.aeternum.AeternumSeasonsPlugin;
import Kinkin.aeternum.calendar.CalendarState;
import Kinkin.aeternum.calendar.SeasonUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * AeternumSeasons hook.
 *
 * <p>Listens to {@link SeasonUpdateEvent} and pushes the new season to the
 * {@link WorldStateManager}. On hook load, reads the current season to
 * initialise the state immediately.
 */
public final class AeternumSeasonsHook implements Listener {

    private final WorldStateManager stateManager;
    private final Logger            log;
    private final Plugin            plugin;

    private AeternumSeasonsPlugin aeternumPlugin;

    /** WorldState provider object that is registered with WorldStateManager. */
    private SeasonWorldState activeProvider = null;

    public AeternumSeasonsHook(@NotNull Plugin plugin, @NotNull WorldStateManager stateManager,
                               @NotNull Logger log) {
        this.plugin       = plugin;
        this.stateManager = stateManager;
        this.log          = log;
    }

    public boolean load() {
        Plugin p = Bukkit.getPluginManager().getPlugin("AeternumSeasons");
        if (!(p instanceof AeternumSeasonsPlugin asp)) return false;
        this.aeternumPlugin = asp;

        // Seed current season
        try {
            CalendarState state = asp.getSeasons().getStateCopy();
            updateState(state);
        } catch (Exception e) {
            log.warning("[Hook/AeternumSeasons] Failed to read initial state: " + e.getMessage());
        }

        // Register listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        log.info("[Hook/AeternumSeasons] Hooked into AeternumSeasons.");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSeasonUpdate(@NotNull SeasonUpdateEvent event) {
        updateState(event.getState());
    }

    private void updateState(@NotNull CalendarState calendarState) {
        // Remove previous provider
        if (activeProvider != null) stateManager.removeProvider(activeProvider);

        SeasonState season = SeasonState.fromName(calendarState.season.name());
        activeProvider = new SeasonWorldState(season);
        stateManager.registerProvider(activeProvider);

        log.fine("[Hook/AeternumSeasons] Season → " + season.getId());
    }

    // ─── Inner WorldState Provider ────────────────────────────────────────────

    private static final class SeasonWorldState implements WorldState {
        private final SeasonState season;

        SeasonWorldState(@NotNull SeasonState season) { this.season = season; }

        @Override @NotNull public String getId()          { return season.getId(); }
        @Override @NotNull public String getDisplayName() { return season.getDisplayName(); }
        @Override          public int    getPriority()    { return season.getPriority(); }
        @Override          public boolean isActive()      { return true; }
        @Override @NotNull public WorldStateType getType() { return WorldStateType.SEASON; }
    }
}
