package dev.worldcore.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Typed wrapper around {@code messages.yml}.
 */
public final class MessageConfig {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private String prefix                    = "<dark_gray>[<gradient:#6a11cb:#2575fc>WorldCore</gradient><dark_gray>]</dark_gray> ";
    private String noPermission              = "<red>You don't have permission to do that.";
    private String unknownCommand            = "<red>Unknown sub-command. Use <yellow>/worldcore help</yellow>.";
    private String playerOnly               = "<red>This command can only be used by players.";
    private String reloadSuccess             = "<green>WorldCore configuration reloaded successfully.";
    private String stateChanged              = "<aqua>🌍 World State changed to: <white>{state}";
    private String stateCurrent              = "<aqua>Current World State: <white>{state}";
    private String expeditionStarted         = "<aqua>⛵ Expedition <white>{expedition} <aqua>started! Returns in <white>{duration}<aqua>.";
    private String expeditionClaimed         = "<green>✅ Expedition <white>{expedition} <green>completed! Rewards delivered.";
    private String expeditionNoActive        = "<red>You have no active expeditions to claim.";
    private String expeditionNotReady        = "<yellow>⏱ Your expedition <white>{expedition} <yellow>returns in <white>{remaining}<yellow>.";
    private String expeditionNoSlots         = "<red>You have no free expedition slots.";
    private String expeditionNotFound        = "<red>That expedition does not exist or is not available.";
    private String expeditionNotAvailable    = "<red>That expedition is not available in the current world state.";
    private String expeditionInsufficientMoney = "<red>You need <yellow>${amount} <red>to start this expedition.";
    private String expeditionInsufficientItems = "<red>You need <yellow>{item} x{amount} <red>to start this expedition.";
    private String achievementUnlocked       = "<gold>🏆 Achievement Unlocked: <yellow>{achievement}";
    private String achievementAlreadyUnlocked = "<yellow>You have already unlocked this achievement.";
    private String titleAwarded              = "<light_purple>👑 Title Awarded: <white>{title}";
    private String titleSet                  = "<green>Active title set to: <white>{title}";
    private String titleCleared              = "<gray>Active title cleared.";
    private String titleNotOwned            = "<red>You don't own that title.";
    private String titleNotFound            = "<red>That title does not exist.";
    private String broadcastAchievement      = "{prefix}<gold>🏆 <white>{player} <gray>unlocked <yellow>{achievement}<gray>!";
    private String broadcastExpedition       = "{prefix}<aqua>⛵ <white>{player} <gray>completed <aqua>{expedition}<gray>!";

    public void reload(@NotNull FileConfiguration cfg) {
        prefix                    = cfg.getString("prefix", prefix);
        noPermission              = cfg.getString("no-permission", noPermission);
        unknownCommand            = cfg.getString("unknown-command", unknownCommand);
        playerOnly               = cfg.getString("player-only", playerOnly);
        reloadSuccess             = cfg.getString("reload-success", reloadSuccess);
        stateChanged              = cfg.getString("state-changed", stateChanged);
        stateCurrent              = cfg.getString("state-current", stateCurrent);
        expeditionStarted         = cfg.getString("expedition-started", expeditionStarted);
        expeditionClaimed         = cfg.getString("expedition-claimed", expeditionClaimed);
        expeditionNoActive        = cfg.getString("expedition-no-active", expeditionNoActive);
        expeditionNotReady        = cfg.getString("expedition-not-ready", expeditionNotReady);
        expeditionNoSlots         = cfg.getString("expedition-no-slots", expeditionNoSlots);
        expeditionNotFound        = cfg.getString("expedition-not-found", expeditionNotFound);
        expeditionNotAvailable    = cfg.getString("expedition-not-available", expeditionNotAvailable);
        expeditionInsufficientMoney = cfg.getString("expedition-cost-insufficient-money", expeditionInsufficientMoney);
        expeditionInsufficientItems = cfg.getString("expedition-cost-insufficient-items", expeditionInsufficientItems);
        achievementUnlocked       = cfg.getString("achievement-unlocked", achievementUnlocked);
        achievementAlreadyUnlocked = cfg.getString("achievement-already-unlocked", achievementAlreadyUnlocked);
        titleAwarded              = cfg.getString("title-awarded", titleAwarded);
        titleSet                  = cfg.getString("title-set", titleSet);
        titleCleared              = cfg.getString("title-cleared", titleCleared);
        titleNotOwned            = cfg.getString("title-not-owned", titleNotOwned);
        titleNotFound            = cfg.getString("title-not-found", titleNotFound);
        broadcastAchievement      = cfg.getString("broadcast-achievement", broadcastAchievement);
        broadcastExpedition       = cfg.getString("broadcast-expedition-complete", broadcastExpedition);
    }

    // ─── Component builders ───────────────────────────────────────────────────

    private @NotNull Component parse(@NotNull String raw, @NotNull Map<String, String> placeholders) {
        String result = raw.replace("{prefix}", prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return MM.deserialize(result);
    }

    public @NotNull Component prefixed(@NotNull String raw, @NotNull Map<String, String> ph) {
        return parse(prefix + raw, ph);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public @NotNull Component noPermission()   { return MM.deserialize(noPermission); }
    public @NotNull Component unknownCommand() { return MM.deserialize(unknownCommand); }
    public @NotNull Component playerOnly()     { return MM.deserialize(playerOnly); }
    public @NotNull Component reloadSuccess()  { return parse(prefix + reloadSuccess, Map.of()); }

    public @NotNull Component stateChanged(@NotNull String state) {
        return parse(prefix + stateChanged, Map.of("{state}", state));
    }
    public @NotNull Component stateCurrent(@NotNull String state) {
        return parse(prefix + stateCurrent, Map.of("{state}", state));
    }

    public @NotNull Component expeditionStarted(@NotNull String name, @NotNull String dur) {
        return parse(prefix + expeditionStarted, Map.of("{expedition}", name, "{duration}", dur));
    }
    public @NotNull Component expeditionClaimed(@NotNull String name) {
        return parse(prefix + expeditionClaimed, Map.of("{expedition}", name));
    }
    public @NotNull Component expeditionNoActive()     { return parse(prefix + expeditionNoActive, Map.of()); }
    public @NotNull Component expeditionNoSlots()      { return parse(prefix + expeditionNoSlots, Map.of()); }
    public @NotNull Component expeditionNotFound()     { return parse(prefix + expeditionNotFound, Map.of()); }
    public @NotNull Component expeditionNotAvailable() { return parse(prefix + expeditionNotAvailable, Map.of()); }
    public @NotNull Component expeditionNotReady(@NotNull String name, @NotNull String rem) {
        return parse(prefix + expeditionNotReady, Map.of("{expedition}", name, "{remaining}", rem));
    }
    public @NotNull Component expeditionInsufficientMoney(double amount) {
        return parse(prefix + expeditionInsufficientMoney, Map.of("{amount}", String.format("%.0f", amount)));
    }
    public @NotNull Component expeditionInsufficientItems(@NotNull String item, int amount) {
        return parse(prefix + expeditionInsufficientItems, Map.of("{item}", item, "{amount}", String.valueOf(amount)));
    }

    public @NotNull Component achievementUnlocked(@NotNull String name) {
        return parse(prefix + achievementUnlocked, Map.of("{achievement}", name));
    }
    public @NotNull Component achievementAlreadyUnlocked() {
        return parse(prefix + achievementAlreadyUnlocked, Map.of());
    }

    public @NotNull Component titleAwarded(@NotNull String name) {
        return parse(prefix + titleAwarded, Map.of("{title}", name));
    }
    public @NotNull Component titleSet(@NotNull String name) {
        return parse(prefix + titleSet, Map.of("{title}", name));
    }
    public @NotNull Component titleCleared()    { return parse(prefix + titleCleared, Map.of()); }
    public @NotNull Component titleNotOwned()   { return parse(prefix + titleNotOwned, Map.of()); }
    public @NotNull Component titleNotFound()   { return parse(prefix + titleNotFound, Map.of()); }

    public @NotNull Component broadcastAchievement(@NotNull String player, @NotNull String achievement) {
        return parse(broadcastAchievement, Map.of("{prefix}", prefix, "{player}", player, "{achievement}", achievement));
    }
    public @NotNull Component broadcastExpedition(@NotNull String player, @NotNull String expedition) {
        return parse(broadcastExpedition, Map.of("{prefix}", prefix, "{player}", player, "{expedition}", expedition));
    }

    @NotNull public String getRawPrefix() { return prefix; }
}
