package dev.worldcore.gui;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.config.MainConfig;
import dev.worldcore.gui.util.GUIListener;
import dev.worldcore.gui.util.GuiUtil;
import dev.worldcore.state.WorldState;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class MainMenuGUI implements GUIListener.WorldCoreGUI {

    private final Inventory inventory;
    private final Player player;
    private final WorldCorePlugin plugin;

    public MainMenuGUI(@NotNull Player player, @NotNull WorldCorePlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        MainConfig cfg = plugin.getMainConfig();
        this.inventory = Bukkit.createInventory(this, 27, MiniMessage.miniMessage().deserialize(cfg.getGuiMainTitle()));
        build();
    }

    private void build() {
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, GuiUtil.filler());
        }

        WorldState state = plugin.getStateManager().getActiveState();
        ItemStack stateItem = GuiUtil.createItem(Material.COMPASS, 
                plugin.getMainConfig().getGuiStateTitle(),
                "<gray>Current State: <white>" + state.getDisplayName(),
                "",
                "<yellow>Click to view active modifiers."
        );
        inventory.setItem(10, stateItem);

        ItemStack expItem = GuiUtil.createItem(Material.OAK_BOAT, 
                plugin.getMainConfig().getGuiExpeditionsTitle(),
                "<gray>Manage your expeditions.",
                "",
                "<yellow>Click to open."
        );
        inventory.setItem(12, expItem);

        ItemStack achItem = GuiUtil.createItem(Material.GOLD_NUGGET, 
                plugin.getMainConfig().getGuiAchievementsTitle(),
                "<gray>View achievements and rewards.",
                "",
                "<yellow>Click to open."
        );
        inventory.setItem(14, achItem);

        ItemStack titleItem = GuiUtil.createItem(Material.NAME_TAG, 
                plugin.getMainConfig().getGuiTitlesTitle(),
                "<gray>Select your cosmetic title.",
                "",
                "<yellow>Click to open."
        );
        inventory.setItem(16, titleItem);
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 10) {
            // View world state modifiers (could be a separate GUI, for now just message)
            player.sendMessage(plugin.getMessageConfig().stateCurrent(plugin.getStateManager().getActiveState().getDisplayName()));
            player.closeInventory();
        } else if (slot == 12) {
            new ExpeditionsGUI(player, plugin).open();
        } else if (slot == 14) {
            new AchievementsGUI(player, plugin).open();
        } else if (slot == 16) {
            new TitlesGUI(player, plugin).open();
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
