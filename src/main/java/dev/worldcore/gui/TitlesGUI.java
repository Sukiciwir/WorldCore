package dev.worldcore.gui;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.gui.util.GUIListener;
import dev.worldcore.gui.util.GuiUtil;
import dev.worldcore.title.Title;
import dev.worldcore.title.TitleManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TitlesGUI implements GUIListener.WorldCoreGUI {

    private final Inventory inventory;
    private final Player player;
    private final WorldCorePlugin plugin;
    private int page = 0;
    private final List<Title> titles = new ArrayList<>();

    public TitlesGUI(@NotNull Player player, @NotNull WorldCorePlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, MiniMessage.miniMessage().deserialize(plugin.getMainConfig().getGuiTitlesTitle()));
        this.titles.addAll(plugin.getTitleManager().getDefinitions());
        build();
    }

    private void build() {
        inventory.clear();
        TitleManager tm = plugin.getTitleManager();
        Set<String> unlocked = tm.getUnlockedTitles(player.getUniqueId());
        String active = tm.getActiveTitle(player.getUniqueId());

        int start = page * 45;
        int end = Math.min(start + 45, titles.size());

        for (int i = start; i < end; i++) {
            Title t = titles.get(i);
            boolean isUnlocked = unlocked.contains(t.id());
            boolean isActive = t.id().equals(active);

            Material mat = isActive ? Material.ENCHANTED_BOOK : (isUnlocked ? Material.BOOK : Material.BARRIER);
            String status = isActive ? "<green>ACTIVE" : (isUnlocked ? "<yellow>Click to equip" : "<red>Locked");

            inventory.setItem(i - start, GuiUtil.createItem(
                    mat,
                    t.display(),
                    "<gray>" + t.description(),
                    "",
                    status
            ));
        }

        // Pagination row
        for (int i = 45; i < 54; i++) inventory.setItem(i, GuiUtil.filler());
        if (page > 0) inventory.setItem(45, GuiUtil.createItem(Material.ARROW, "<yellow>Previous Page"));
        if (end < titles.size()) inventory.setItem(53, GuiUtil.createItem(Material.ARROW, "<yellow>Next Page"));
        
        inventory.setItem(49, GuiUtil.createItem(Material.RED_DYE, "<red>Clear Title", "<gray>Click to unequip your title."));
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == 45 && page > 0) {
            page--;
            build();
        } else if (slot == 53 && (page + 1) * 45 < titles.size()) {
            page++;
            build();
        } else if (slot == 49) {
            plugin.getTitleManager().clearActiveTitle(player);
            player.closeInventory();
        } else if (slot < 45) {
            int index = page * 45 + slot;
            if (index < titles.size()) {
                Title t = titles.get(index);
                if (plugin.getTitleManager().setActiveTitle(player, t.id())) {
                    player.closeInventory();
                }
            }
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
