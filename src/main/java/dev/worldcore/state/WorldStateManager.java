package dev.worldcore.state;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Manages the current active {@link WorldState}.
 *
 * <p>Priority resolution:
 * <ol>
 *   <li>Highest-priority active state wins.</li>
 *   <li>Event states (priority ≥ 100) override season states.</li>
 *   <li>If no hook is active, the default configured season is used.</li>
 * </ol>
 *
 * <p>Hooks register state providers via {@link #registerProvider}.
 * When a state changes, a {@link WorldStateChangeEvent} is fired on the Bukkit
 * event bus, and all registered {@link StateChangeReaction}s are called.
 */
public final class WorldStateManager {

    /** Functional interface for reacting to state changes internally. */
    @FunctionalInterface
    public interface StateChangeReaction {
        void onStateChange(@NotNull WorldState previous, @NotNull WorldState current);
    }

    private final Logger log;
    private final org.bukkit.plugin.Plugin plugin;

    /** All registered state providers, sorted by descending priority. */
    private final List<WorldState> providers = new CopyOnWriteArrayList<>();

    /** Internal reaction callbacks (economy, expedition, achievement, title managers). */
    private final List<StateChangeReaction> reactions = new CopyOnWriteArrayList<>();

    /** Currently active state. Never null after init(). */
    private volatile WorldState activeState;

    /** Default fallback season, set from config. */
    private SeasonState defaultSeason = SeasonState.SPRING;

    public WorldStateManager(@NotNull org.bukkit.plugin.Plugin plugin, @NotNull Logger log) {
        this.plugin = plugin;
        this.log    = log;
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    /**
     * Initialise with the configured default season.
     * Always call this before hooks register their providers.
     */
    public void init(@NotNull SeasonState defaultSeason) {
        this.defaultSeason = defaultSeason;
        this.activeState   = defaultSeason;
        log.info("[WorldState] Initialised — default: " + defaultSeason.getId());
    }

    // ─── Provider Registry ────────────────────────────────────────────────────

    /**
     * Register a state provider. Hooks call this when they load.
     * The manager re-evaluates after registration.
     */
    public synchronized void registerProvider(@NotNull WorldState state) {
        providers.add(state);
        providers.sort(Comparator.comparingInt(WorldState::getPriority).reversed());
        reevaluate();
    }

    /**
     * Remove a provider. Hooks call this on unload / when an event ends.
     */
    public synchronized void removeProvider(@NotNull WorldState state) {
        providers.remove(state);
        reevaluate();
    }

    // ─── Reaction Registry ────────────────────────────────────────────────────

    public void registerReaction(@NotNull StateChangeReaction reaction) {
        reactions.add(reaction);
    }

    // ─── Resolution ───────────────────────────────────────────────────────────

    /**
     * Force a specific state (admin command / testing).
     */
    public synchronized void forceState(@NotNull WorldState state) {
        applyState(state);
    }

    /**
     * Re-evaluate which state is active based on current providers.
     * Called automatically when providers change; also callable by hooks when
     * their own data changes (e.g. a WorldEvent ends).
     */
    public synchronized void reevaluate() {
        WorldState best = defaultSeason;
        for (WorldState provider : providers) {
            if (provider.isActive() && provider.getPriority() > best.getPriority()) {
                best = provider;
            }
        }
        applyState(best);
    }

    private void applyState(@NotNull WorldState newState) {
        WorldState prev = activeState;
        if (prev != null && prev.getId().equals(newState.getId())) return; // no change

        activeState = newState;
        log.info("[WorldState] State → " + newState.getId()
                + (prev != null ? " (was: " + prev.getId() + ")" : ""));

        // Fire Bukkit event (on main thread — called from Bukkit listener context)
        WorldStateChangeEvent event = new WorldStateChangeEvent(prev, newState);
        plugin.getServer().getPluginManager().callEvent(event);

        // Notify internal reaction managers
        for (StateChangeReaction reaction : reactions) {
            try {
                reaction.onStateChange(prev != null ? prev : newState, newState);
            } catch (Exception e) {
                log.warning("[WorldState] Reaction error: " + e.getMessage());
            }
        }
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    /** @return The currently active world state (never null after init). */
    @NotNull public WorldState getActiveState() { return activeState; }

    /** @return All registered providers. */
    @NotNull public List<WorldState> getProviders() { return new ArrayList<>(providers); }

    /** @return Whether the active state is a world event. */
    public boolean isEventActive() { return activeState.getType() == WorldStateType.EVENT; }

    /** @return The active event ID if active, else null. */
    @org.jetbrains.annotations.Nullable
    public String getActiveEventId() {
        return isEventActive() ? activeState.getId() : null;
    }

    /** @return The active season ID. */
    @NotNull
    public String getActiveSeasonId() {
        if (activeState.getType() == WorldStateType.SEASON) return activeState.getId();
        // Find the highest-priority season provider
        for (WorldState p : providers) {
            if (p.getType() == WorldStateType.SEASON && p.isActive()) return p.getId();
        }
        return defaultSeason.getId();
    }
}
