package dev.worldcore.market.modifiers;

import dev.worldcore.market.MarketConfig;
import dev.worldcore.market.MarketTrendManager;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies the active market trend multiplier.
 */
public final class MarketTrendProvider implements ModifierProvider {

    private final MarketTrendManager trendManager;

    public MarketTrendProvider(@NotNull MarketTrendManager trendManager) {
        this.trendManager = trendManager;
    }

    @Override
    public double getMultiplier(@NotNull Material material, @Nullable String category) {
        MarketConfig.MarketTrendDef trend = trendManager.getActiveTrend();
        if (trend == null) return 1.0;

        double mult = trend.globalMultiplier();
        if (category != null) {
            mult *= trend.categoryModifiers().getOrDefault(category, 1.0);
        }
        return mult;
    }
}
