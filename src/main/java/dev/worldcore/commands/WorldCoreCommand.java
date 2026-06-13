package dev.worldcore.commands;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.gui.MainMenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WorldCoreCommand implements CommandExecutor {

    private final WorldCorePlugin plugin;

    public WorldCoreCommand(@NotNull WorldCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("worldcore.admin")) {
                sender.sendMessage(plugin.getMessageConfig().noPermission());
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(plugin.getMessageConfig().reloadSuccess());
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageConfig().playerOnly());
            return true;
        }

        new MainMenuGUI(player, plugin).open();
        return true;
    }
}
