package dev.worldcore.market.modifiers;

import dev.worldcore.market.MarketConfig;
import dev.worldcore.state.WorldStateManager;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies the current season's modifier for the item's category.
 */
public final class SeasonModifierProvider implements ModifierProvider {

    private final MarketConfig      config;
    private final WorldStateManager stateManager;

    public SeasonModifierProvider(@NotNull MarketConfig config, @NotNull WorldStateManager stateManager) {
        this.config       = config;
        this.stateManager = stateManager;
    }

    @Override
    public double getMultiplier(@NotNull Material material, @Nullable String category) {
        String seasonId = stateManager.getActiveSeasonId();
        return config.getSeasonModifier(seasonId, category);
    }
}
