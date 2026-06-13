package dev.worldcore.gui.util;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Prevents item modification in WorldCore GUIs.
 */
public final class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof WorldCoreGUI) {
            event.setCancelled(true);
            ((WorldCoreGUI) holder).onClick(event);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof WorldCoreGUI) {
            event.setCancelled(true);
        }
    }
    
    /** Interface for all WorldCore GUI holders. */
    public interface WorldCoreGUI extends InventoryHolder {
        void onClick(InventoryClickEvent event);
    }
}
