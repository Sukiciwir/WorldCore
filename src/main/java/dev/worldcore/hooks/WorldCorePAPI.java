package dev.worldcore.hooks;

import dev.worldcore.WorldCorePlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion for WorldCore.
 */
public final class WorldCorePAPI extends PlaceholderExpansion {

    private final WorldCorePlugin plugin;

    public WorldCorePAPI(@NotNull WorldCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override @NotNull public String getIdentifier() { return "worldcore"; }
    @Override @NotNull public String getAuthor()     { return plugin.getDescription().getAuthors().get(0); }
    @Override @NotNull public String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("active_state")) {
            return plugin.getStateManager().getActiveState().getDisplayName();
        }
        if (params.equalsIgnoreCase("active_state_id")) {
            return plugin.getStateManager().getActiveState().getId();
        }

        if (player == null) return "";

        if (params.equalsIgnoreCase("active_title")) {
            String titleId = plugin.getTitleManager().getActiveTitle(player.getUniqueId());
            if (titleId == null) return "None";
            var t = plugin.getTitleManager().getDefinition(titleId);
            return t != null ? t.display() : titleId;
        }

        if (params.equalsIgnoreCase("expeditions_available_slots")) {
            int max = plugin.getExpeditionManager().getMaxSlots(player);
            int used = plugin.getExpeditionManager().getUsedSlots(player.getUniqueId());
            return String.valueOf(Math.max(0, max - used));
        }

        if (params.equalsIgnoreCase("achievements_unlocked")) {
            return String.valueOf(plugin.getAchievementManager().getUnlockedCount(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("market_trend")) {
            var trend = plugin.getMarketManager().getTrendManager().getActiveTrend();
            return trend != null ? trend.id() : "Stable";
        }

        if (params.startsWith("item_price_")) {
            String matName = params.substring(11).toUpperCase();
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(matName);
            if (mat != null) {
                double mult = plugin.getMarketManager().getMultiplier(mat);
                return String.format("%.2fx", mult);
            }
            return "N/A";
        }

        return null;
    }
}
