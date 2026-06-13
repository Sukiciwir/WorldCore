package dev.worldcore.expedition;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * A cost that must be paid to start an expedition.
 *
 * @param type    Type of cost resource.
 * @param amount  Numeric amount (money/points) or item count.
 * @param material Item material, null for non-item costs.
 */
public record ExpeditionCost(
        @NotNull CostType type,
        double amount,
        @org.jetbrains.annotations.Nullable Material material
) {
    public enum CostType { MONEY, PLAYER_POINTS, ITEM }
}
