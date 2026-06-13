package dev.worldcore.economy.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * Vault economy hook for providing API access to Vault (used by Expeditions, Achievements, etc).
 */
public final class VaultEconomyHook {

    private final Logger log;
    private Economy vaultEconomy;
    private boolean ready = false;

    public VaultEconomyHook(@NotNull Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;
        vaultEconomy = rsp.getProvider();
        ready = true;
        log.info("[Economy/Vault] Hook active.");
    }

    public boolean isReady() { return ready; }

    @Nullable public Economy getEconomy() { return vaultEconomy; }
}
