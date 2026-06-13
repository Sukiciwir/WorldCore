package dev.worldcore.expedition;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * Static definition of an expedition loaded from {@code expeditions.yml}.
 *
 * @param id               Unique identifier (e.g. {@code "frozen_lake"}).
 * @param name             MiniMessage display name.
 * @param description      Short description.
 * @param category         Category enum.
 * @param worldStates      Set of world-state IDs this is available for (empty = always).
 * @param durationSeconds  How long the expedition takes.
 * @param costs            List of costs. All costs are ANDed together.
 * @param rewards          List of potential rewards.
 */
public record Expedition(
        @NotNull String id,
        @NotNull String name,
        @NotNull String description,
        @NotNull ExpeditionCategory category,
        @NotNull Set<String> worldStates,
        long durationSeconds,
        @NotNull List<ExpeditionCost> costs,
        @NotNull List<ExpeditionReward> rewards
) {

    /**
     * @param stateId Current active world state ID.
     * @return true if this expedition is available in the given state.
     */
    public boolean isAvailable(@NotNull String stateId) {
        return worldStates.isEmpty() || worldStates.contains(stateId.toLowerCase());
    }
}
