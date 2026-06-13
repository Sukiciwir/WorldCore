package dev.worldcore.market.modifiers;

import dev.worldcore.market.MarketConfig;
import dev.worldcore.state.WorldStateManager;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Applies active world event modifiers.
 */
public final class EventModifierProvider implements ModifierProvider {

    private final MarketConfig      config;
    private final WorldStateManager stateManager;

    public EventModifierProvider(@NotNull MarketConfig config, @NotNull WorldStateManager stateManager) {
        this.config       = config;
        this.stateManager = stateManager;
    }

    @Override
    public double getMultiplier(@NotNull Material material, @Nullable String category) {
        if (!stateManager.isEventActive()) return 1.0;
        String eventId = stateManager.getActiveEventId();
        if (eventId == null) return 1.0;
        return config.getEventModifier(eventId, category);
    }
}
