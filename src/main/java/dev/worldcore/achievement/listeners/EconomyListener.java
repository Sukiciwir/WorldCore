package dev.worldcore.achievement.listeners;

import dev.worldcore.achievement.AchievementCategory;
import dev.worldcore.achievement.AchievementManager;
import dev.worldcore.economy.hooks.VaultEconomyHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class EconomyListener {

    private final AchievementManager achievements;
    private final VaultEconomyHook   vaultHook;

    public EconomyListener(@NotNull Plugin plugin, @NotNull AchievementManager achievements,
                           @NotNull VaultEconomyHook vaultHook) {
        this.achievements = achievements;
        this.vaultHook    = vaultHook;

        // Schedule a repeating task to check balances
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkBalances, 1200L, 1200L); // every minute
    }

    private void checkBalances() {
        if (!vaultHook.isReady() || vaultHook.getEconomy() == null) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            double balance = vaultHook.getEconomy().getBalance(player);
            achievements.setProgress(player.getUniqueId(), "economy_10k", (long) balance);
            achievements.setProgress(player.getUniqueId(), "economy_100k", (long) balance);
            achievements.setProgress(player.getUniqueId(), "economy_1m", (long) balance);
            achievements.setProgress(player.getUniqueId(), "economy_10m", (long) balance);
        }
    }
}
