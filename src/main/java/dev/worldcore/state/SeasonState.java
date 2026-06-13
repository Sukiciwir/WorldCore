package dev.worldcore.state;

import org.jetbrains.annotations.NotNull;

/**
 * Built-in season states. Priority range: 0–50.
 *
 * <p>Seasons are resolved from AeternumSeasons if present, or from the
 * {@code world-state.default-season} config option.
 */
public enum SeasonState implements WorldState {

    SPRING("spring", "<green>🌸 Spring", 10),
    SUMMER("summer", "<yellow>☀ Summer", 11),
    AUTUMN("autumn", "<dark_orange>🍂 Autumn", 12),
    WINTER("winter", "<aqua>❄ Winter", 13);

    private final String id;
    private final String displayName;
    private final int priority;

    SeasonState(@NotNull String id, @NotNull String displayName, int priority) {
        this.id          = id;
        this.displayName = displayName;
        this.priority    = priority;
    }

    @Override @NotNull public String getId()          { return id; }
    @Override @NotNull public String getDisplayName() { return displayName; }
    @Override          public int    getPriority()    { return priority; }
    @Override @NotNull public WorldStateType getType() { return WorldStateType.SEASON; }

    /**
     * Seasons declared as enum are always "potentially active" — the
     * WorldStateManager decides which one is currently live.
     */
    @Override public boolean isActive() { return true; }

    /**
     * Parse from an AeternumSeasons {@code Season} name (case-insensitive).
     *
     * @param name season name (e.g. "WINTER")
     * @return matching SeasonState or SPRING as fallback
     */
    @NotNull
    public static SeasonState fromName(@NotNull String name) {
        for (SeasonState s : values()) {
            if (s.name().equalsIgnoreCase(name)) return s;
        }
        return SPRING;
    }
}
