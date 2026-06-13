package dev.worldcore.gui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for building GUI items.
 */
public final class GuiUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    @NotNull
    public static ItemStack createItem(@NotNull Material mat, @NotNull String name, @NotNull String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MM.deserialize(name));
            if (lore.length > 0) {
                List<Component> loreComp = new ArrayList<>();
                for (String l : lore) loreComp.add(MM.deserialize(l));
                meta.lore(loreComp);
            }
            // Hide attributes, enchants, etc. for cleaner GUI
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
            item.setItemMeta(meta);
        }
        return item;
    }

    @NotNull
    public static ItemStack createItem(@NotNull Material mat, @NotNull String name, @NotNull List<String> lore) {
        return createItem(mat, name, lore.toArray(new String[0]));
    }
    
    @NotNull
    public static ItemStack filler() {
        return createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
    }
}
