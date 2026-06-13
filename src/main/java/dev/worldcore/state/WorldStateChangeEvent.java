package dev.worldcore.state;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired on the Bukkit event bus whenever the active world state changes.
 *
 * <p>External plugins can listen to this event to react to state transitions.
 *
 * <pre>{@code
 * @EventHandler
 * public void onStateChange(WorldStateChangeEvent event) {
 *     WorldState newState = event.getNewState();
 *     // react...
 * }
 * }</pre>
 */
public final class WorldStateChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final @Nullable WorldState previousState;
    private final @NotNull  WorldState newState;

    public WorldStateChangeEvent(@Nullable WorldState previousState, @NotNull WorldState newState) {
        this.previousState = previousState;
        this.newState      = newState;
    }

    /** @return The state that was active before this change, or {@code null} on first load. */
    @Nullable public WorldState getPreviousState() { return previousState; }

    /** @return The newly active state. */
    @NotNull  public WorldState getNewState()      { return newState; }

    @Override @NotNull public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList()          { return HANDLERS; }
}
