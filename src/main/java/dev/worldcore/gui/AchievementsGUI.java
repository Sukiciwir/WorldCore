package dev.worldcore.gui;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.achievement.Achievement;
import dev.worldcore.achievement.AchievementManager;
import dev.worldcore.achievement.AchievementProgress;
import dev.worldcore.gui.util.GUIListener;
import dev.worldcore.gui.util.GuiUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AchievementsGUI implements GUIListener.WorldCoreGUI {

    private final Inventory inventory;
    private final Player player;
    private final WorldCorePlugin plugin;
    private int page = 0;
    private final List<Achievement> achievements = new ArrayList<>();

    public AchievementsGUI(@NotNull Player player, @NotNull WorldCorePlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, MiniMessage.miniMessage().deserialize(plugin.getMainConfig().getGuiAchievementsTitle()));
        this.achievements.addAll(plugin.getAchievementManager().getDefinitions());
        build();
    }

    private void build() {
        inventory.clear();
        AchievementManager am = plugin.getAchievementManager();
        Map<String, AchievementProgress> progress = am.getProgress(player.getUniqueId());

        int start = page * 45;
        int end = Math.min(start + 45, achievements.size());

        for (int i = start; i < end; i++) {
            Achievement a = achievements.get(i);
            AchievementProgress ap = progress.get(a.id());
            boolean unlocked = ap != null && ap.isUnlocked();
            long current = ap != null ? Math.min(ap.getProgress(), a.target()) : 0;

            String status = unlocked ? "<green>Unlocked!" : "<gray>Progress: <white>" + current + " / " + a.target();
            String reward = a.titleReward() != null ? "<light_purple>Reward: Title '" + a.titleReward() + "'" : "";

            inventory.setItem(i - start, GuiUtil.createItem(
                    a.icon(),
                    a.name(),
                    "<gray>" + a.description(),
                    "",
                    status,
                    reward
            ));
        }

        // Pagination row
        for (int i = 45; i < 54; i++) inventory.setItem(i, GuiUtil.filler());
        if (page > 0) inventory.setItem(45, GuiUtil.createItem(org.bukkit.Material.ARROW, "<yellow>Previous Page"));
        if (end < achievements.size()) inventory.setItem(53, GuiUtil.createItem(org.bukkit.Material.ARROW, "<yellow>Next Page"));
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
        } else if (slot == 53 && (page + 1) * 45 < achievements.size()) {
            page++;
            build();
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
