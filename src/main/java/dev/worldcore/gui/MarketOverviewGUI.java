package dev.worldcore.gui;

import dev.worldcore.WorldCorePlugin;
import dev.worldcore.gui.util.GUIListener;
import dev.worldcore.gui.util.GuiUtil;
import dev.worldcore.market.MarketConfig;
import dev.worldcore.market.SupplyDemandTracker;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public final class MarketOverviewGUI implements GUIListener.WorldCoreGUI {

    private final Inventory inventory;
    private final Player player;
    private final WorldCorePlugin plugin;

    public MarketOverviewGUI(@NotNull Player player, @NotNull WorldCorePlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, MiniMessage.miniMessage().deserialize("<gold>Market Overview"));
        build();
    }

    private void build() {
        for (int i = 0; i < 27; i++) inventory.setItem(i, GuiUtil.filler());

        MarketConfig.MarketTrendDef trend = plugin.getMarketManager().getTrendManager().getActiveTrend();
        String trendName = trend != null ? trend.id() : "Stable";

        inventory.setItem(11, GuiUtil.createItem(
                Material.EMERALD,
                "<green>Global Market Trend",
                "<gray>Current Trend: <white>" + trendName,
                "",
                "<yellow>Trends affect all prices globally."
        ));

        inventory.setItem(13, GuiUtil.createItem(
                Material.DIAMOND,
                "<aqua>Top Traded Items",
                "<gray>Click to view the most traded items",
                "<gray>on the server."
        ));

        // Just an example item to show its live multiplier
        double diamondMult = plugin.getMarketManager().getMultiplier(Material.DIAMOND);
        SupplyDemandTracker sdt = plugin.getMarketManager().getSupplyDemandTracker();
        
        inventory.setItem(15, GuiUtil.createItem(
                Material.DIAMOND_ORE,
                "<b_aqua>Diamond Market",
                "<gray>Live Price Multiplier: <white>" + String.format("%.2fx", diamondMult),
                "<gray>Total Sold: <white>" + sdt.getTotalSold(Material.DIAMOND),
                "<gray>Total Bought: <white>" + sdt.getTotalBought(Material.DIAMOND)
        ));
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        // Handle clicks if needed
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
