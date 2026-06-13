package dev.worldcore.market;

import dev.worldcore.market.modifiers.ModifierProvider;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Central engine for calculating dynamic commodity prices.
 */
public final class MarketManager {

    private final Logger log;
    private final MarketConfig config;
    private final MarketTrendManager trendManager;
    private final SupplyDemandTracker supplyDemandTracker;

    private final List<ModifierProvider> providers = new ArrayList<>();

    public MarketManager(@NotNull Logger log, @NotNull MarketConfig config,
                         @NotNull MarketTrendManager trendManager,
                         @NotNull SupplyDemandTracker supplyDemandTracker) {
        this.log = log;
        this.config = config;
        this.trendManager = trendManager;
        this.supplyDemandTracker = supplyDemandTracker;
    }

    public void registerProvider(@NotNull ModifierProvider provider) {
        providers.add(provider);
        log.fine("[Market] Registered provider: " + provider.getClass().getSimpleName());
    }

    /**
     * Calculates the final multiplier for the given material.
     * Formula: Final = Product of all providers
     */
    public double getMultiplier(@NotNull Material material) {
        String category = config.getCategory(material);
        
        double multiplier = 1.0;
        for (ModifierProvider provider : providers) {
            multiplier *= provider.getMultiplier(material, category);
        }
        
        // Safety cap, preventing prices from going to 0 or infinite
        if (multiplier < 0.1) multiplier = 0.1;
        if (multiplier > 10.0) multiplier = 10.0;
        
        return multiplier;
    }

    @NotNull public MarketConfig getConfig() { return config; }
    @NotNull public MarketTrendManager getTrendManager() { return trendManager; }
    @NotNull public SupplyDemandTracker getSupplyDemandTracker() { return supplyDemandTracker; }
}
