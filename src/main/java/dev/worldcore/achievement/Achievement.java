package dev.worldcore.achievement;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Static achievement definition loaded from {@code achievements.yml}.
 *
 * @param id           Unique ID.
 * @param name         MiniMessage display name.
 * @param description  Short description.
 * @param category     Achievement category.
 * @param type         INCREMENTAL (progress target) or ONE_SHOT.
 * @param target       Progress value needed for unlock.
 * @param icon         GUI display icon material.
 * @param worldStates  Set of world-state IDs required (empty = any).
 * @param titleReward  Title ID awarded on unlock, or null.
 */
public record Achievement(
        @NotNull  String              id,
        @NotNull  String              name,
        @NotNull  String              description,
        @NotNull  AchievementCategory category,
        @NotNull  AchievementType     type,
        long                          target,
        @NotNull  Material            icon,
        @NotNull  Set<String>         worldStates,
        @Nullable String              titleReward
) {
    /**
     * @param stateId Current world state ID.
     * @return true if this achievement can be progressed in the given state.
     */
    public boolean isValidInState(@NotNull String stateId) {
        return worldStates.isEmpty() || worldStates.contains(stateId.toLowerCase());
    }
}
