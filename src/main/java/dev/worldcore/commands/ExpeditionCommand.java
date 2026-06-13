package dev.worldcore.commands;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.gui.ExpeditionsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ExpeditionCommand implements CommandExecutor {

    private final WorldCorePlugin plugin;

    public ExpeditionCommand(@NotNull WorldCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageConfig().playerOnly());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("claim")) {
            int claimed = plugin.getExpeditionManager().claimAll(player);
            if (claimed == 0) {
                player.sendMessage(plugin.getMessageConfig().expeditionNoActive());
            }
            return true;
        }

        new ExpeditionsGUI(player, plugin).open();
        return true;
    }
}
