package dev.worldcore.market.modifiers;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for components that can influence the market price of an item.
 */
public interface ModifierProvider {

    /**
     * Calculate the multiplier for the given material and its category.
     *
     * @param material  The item being priced.
     * @param category  The item's market category (e.g. ORES), or null if uncategorised.
     * @return Multiplier (1.0 = no change).
     */
    double getMultiplier(@NotNull Material material, @Nullable String category);
}
