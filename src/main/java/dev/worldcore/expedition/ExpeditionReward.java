package dev.worldcore.expedition;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A reward given when an expedition is claimed.
 *
 * @param type      Type of reward.
 * @param amount    Numeric amount or item count.
 * @param material  Item material (for ITEM rewards), or null.
 * @param crateId   Crate ID (for CRATE_KEY rewards), or null.
 * @param chance    Probability of this reward (0–100).
 */
public record ExpeditionReward(
        @NotNull RewardType type,
        double amount,
        @Nullable Material material,
        @Nullable String crateId,
        int chance
) {
    public enum RewardType { MONEY, ITEM, CRATE_KEY, PLAYER_POINTS }

    /** @return true if this reward should be rolled (chance check passed). */
    public boolean roll() {
        return Math.random() * 100 < chance;
    }
}
