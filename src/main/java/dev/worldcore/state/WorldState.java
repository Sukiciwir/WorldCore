package dev.worldcore.state;

import org.jetbrains.annotations.NotNull;

/**
 * A WorldState represents any condition that can be the "current active state"
 * of the living world (season or event).
 *
 * <p>All WorldState implementations must be immutable.
 */
public interface WorldState {

    /**
     * @return Unique string identifier (lowercase, no spaces).
     *         Example: {@code "blood_moon"}, {@code "winter"}.
     */
    @NotNull String getId();

    /**
     * @return Human-readable display name (MiniMessage supported).
     *         Example: {@code "<dark_red>Blood Moon"}.
     */
    @NotNull String getDisplayName();

    /**
     * @return Priority order. Higher priority states override lower ones.
     *         Events have higher priority than seasons.
     *         <ul>
     *           <li>Events: priority ≥ 100</li>
     *           <li>Seasons: priority 0–99</li>
     *         </ul>
     */
    int getPriority();

    /**
     * @return Whether this state is currently active (callable at any time).
     */
    boolean isActive();

    /**
     * @return The type of this world state.
     */
    @NotNull WorldStateType getType();
}
