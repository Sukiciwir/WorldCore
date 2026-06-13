package dev.worldcore.market.transactions;

import dev.worldcore.market.MarketManager;
import dev.worldcore.market.SupplyDemandTracker;
import dev.worldcore.statistics.StatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * QuickShop-Hikari transaction provider.
 * Intercepts ShopPurchaseEvent for Supply/Demand.
 * Intercepts PriceCalcEvent for dynamic pricing.
 */
public final class QuickShopProvider implements TransactionProvider {

    private final Plugin plugin;
    private final Logger log;
    private final MarketManager marketManager;
    private final SupplyDemandTracker tracker;
    private final StatisticsManager stats;
    private boolean ready = false;

    public QuickShopProvider(@NotNull Plugin plugin, @NotNull Logger log,
                             @NotNull MarketManager marketManager,
                             @NotNull SupplyDemandTracker tracker,
                             @NotNull StatisticsManager stats) {
        this.plugin = plugin;
        this.log = log;
        this.marketManager = marketManager;
        this.tracker = tracker;
        this.stats = stats;
        init();
    }

    private void init() {
        Plugin qs = Bukkit.getPluginManager().getPlugin("QuickShop-Hikari");
        if (qs == null || !qs.isEnabled()) return;

        try {
            org.bukkit.event.Listener listener = new QuickShopListener();
            Bukkit.getPluginManager().registerEvents(listener, plugin);
            ready = true;
            log.info("[Market/QuickShop] Provider hooked for dynamic pricing and transaction tracking.");
        } catch (Throwable e) {
            log.warning("[Market/QuickShop] Failed to hook: " + e.getMessage());
        }
    }

    @Override public boolean isReady() { return ready; }
    @Override @NotNull public String getName() { return "QuickShop-Hikari"; }

    private final class QuickShopListener implements org.bukkit.event.Listener {

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
        public void onPriceCalc(org.bukkit.event.Event event) {
            String cn = event.getClass().getName();
            if (!cn.contains("quickshop") || !cn.contains("PriceCalc")) return;

            try {
                // Get the ItemStack being bought/sold
                Method getItem = event.getClass().getMethod("getItem");
                Object item = getItem.invoke(event);
                if (!(item instanceof org.bukkit.inventory.ItemStack is)) return;
                Material mat = is.getType();

                // Dynamic calculation
                double mult = marketManager.getMultiplier(mat);
                if (mult == 1.0) return;

                Method getPrice = event.getClass().getMethod("getPrice");
                double price = (double) getPrice.invoke(event);
                Method setPrice = event.getClass().getMethod("setPrice", double.class);
                setPrice.invoke(event, price * mult);

            } catch (Throwable ignored) { }
        }

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
        public void onPurchase(org.bukkit.event.Event event) {
            String cn = event.getClass().getName();
            if (!cn.contains("quickshop") || !cn.contains("ShopPurchaseEvent")) return;

            try {
                // Determine item and amount
                Method getShop = event.getClass().getMethod("getShop");
                Object shop = getShop.invoke(event);
                Method getItem = shop.getClass().getMethod("getItem");
                Object item = getItem.invoke(shop);
                if (!(item instanceof org.bukkit.inventory.ItemStack is)) return;
                Material mat = is.getType();

                Method getAmount = event.getClass().getMethod("getAmount");
                int amount = (int) getAmount.invoke(event);

                Method isSelling = shop.getClass().getMethod("isSelling");
                boolean shopIsSelling = (boolean) isSelling.invoke(shop);
                // If shop is selling, player is buying (demand goes up, supply goes down)
                // If shop is buying, player is selling (supply goes up, demand goes down)
                boolean playerIsSelling = !shopIsSelling;

                // Track supply/demand
                tracker.recordTransaction(mat, amount, playerIsSelling);

                // Update merchant stats
                Method getPlayer = event.getClass().getMethod("getPurchaser");
                java.util.UUID uuid = (java.util.UUID) getPlayer.invoke(event);
                
                // Get money exchanged
                Method getTotal = event.getClass().getMethod("getTotal");
                double total = (double) getTotal.invoke(event);

                // Update stats cache
                // Note: We need a way to pass money/items sold to the stats manager.
                // For now we'll just implement the methods in StatisticsManager.
                stats.addTransactionStats(uuid, (long) total, playerIsSelling ? amount : 0, playerIsSelling ? 0 : amount);

            } catch (Throwable ignored) { }
        }
    }
}
