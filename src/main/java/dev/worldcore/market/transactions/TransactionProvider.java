package dev.worldcore.market.transactions;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for economy shop plugins to hook into WorldCore's Market Engine.
 */
public interface TransactionProvider {

    /** @return true if the hook is active. */
    boolean isReady();

    /** @return Name of the provider (e.g. QuickShop). */
    @NotNull String getName();
}
