package dev.worldcore.state;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a world state type classification.
 */
public enum WorldStateType {
    /** A calendar-based season (Spring / Summer / Autumn / Winter). */
    SEASON,
    /** A dynamic world event (Blood Moon, Diamond Fever, etc.). */
    EVENT
}
