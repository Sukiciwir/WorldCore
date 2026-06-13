package dev.worldcore.gui;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.expedition.ActiveExpedition;
import dev.worldcore.expedition.Expedition;
import dev.worldcore.expedition.ExpeditionManager;
import dev.worldcore.gui.util.GUIListener;
import dev.worldcore.gui.util.GuiUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ExpeditionsGUI implements GUIListener.WorldCoreGUI {

    private final Inventory inventory;
    private final Player player;
    private final WorldCorePlugin plugin;
    private final List<Expedition> available = new ArrayList<>();

    public ExpeditionsGUI(@NotNull Player player, @NotNull WorldCorePlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, MiniMessage.miniMessage().deserialize(plugin.getMainConfig().getGuiExpeditionsTitle()));
        build();
    }

    private void build() {
        ExpeditionManager em = plugin.getExpeditionManager();
        List<ActiveExpedition> active = em.getActiveExpeditions(player.getUniqueId());

        // Top row: Active expeditions
        for (int i = 0; i < 9; i++) {
            if (i < active.size()) {
                ActiveExpedition ae = active.get(i);
                Expedition def = em.getDefinition(ae.expeditionId());
                String name = def != null ? def.name() : ae.expeditionId();
                if (ae.isReady()) {
                    inventory.setItem(i, GuiUtil.createItem(Material.LIME_STAINED_GLASS, 
                            name, "<green>Ready to claim!", "", "<yellow>Click to claim."));
                } else {
                    inventory.setItem(i, GuiUtil.createItem(Material.ORANGE_STAINED_GLASS, 
                            name, "<gray>Time remaining: <white>" + ae.formatRemaining()));
                }
            } else if (i < em.getMaxSlots(player)) {
                inventory.setItem(i, GuiUtil.createItem(Material.WHITE_STAINED_GLASS_PANE, "<gray>Empty Slot"));
            } else {
                inventory.setItem(i, GuiUtil.createItem(Material.BARRIER, "<red>Locked Slot", "<gray>Upgrade to unlock."));
            }
        }

        for (int i = 9; i < 18; i++) inventory.setItem(i, GuiUtil.filler());

        // Available expeditions (filtering by current state)
        String stateId = plugin.getStateManager().getActiveState().getId();
        for (Expedition e : em.getDefinitions()) {
            if (e.isAvailable(stateId)) available.add(e);
        }

        for (int i = 0; i < available.size() && i < 36; i++) {
            Expedition e = available.get(i);
            inventory.setItem(18 + i, GuiUtil.createItem(Material.MAP, 
                    e.name(), 
                    "<gray>" + e.description(),
                    "",
                    "<yellow>Click to start (<white>" + (e.durationSeconds() / 60) + "m<yellow>)"
            ));
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ExpeditionManager em = plugin.getExpeditionManager();

        if (slot < 9) {
            // Clicked active slot (to claim)
            if (em.claimAll(player) > 0) {
                player.closeInventory();
            }
        } else if (slot >= 18 && slot < 18 + available.size()) {
            Expedition clicked = available.get(slot - 18);
            ExpeditionManager.StartResult res = em.startExpedition(player, clicked.id());
            if (res == ExpeditionManager.StartResult.SUCCESS) {
                player.closeInventory();
            } else if (res == ExpeditionManager.StartResult.NO_SLOTS) {
                player.sendMessage(plugin.getMessageConfig().expeditionNoSlots());
            } else if (res == ExpeditionManager.StartResult.INSUFFICIENT_COST) {
                player.sendMessage(plugin.getMessageConfig().prefixed("<red>You cannot afford this.", java.util.Map.of()));
            }
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
