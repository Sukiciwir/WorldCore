package dev.worldcore.hooks;

import dev.worldcore.state.WorldState;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * ODailyQuests hook.
 *
 * <p>ODailyQuests generally doesn't have a robust API for hot-swapping quests.
 * We rely on configuring all quests in ODailyQuests config, and potentially
 * using commands or API to give/remove quest categories if possible.
 *
 * <p>Since WorldCore acts as the engine, this hook can be expanded if ODailyQuests
 * provides a Java API, but for now we might dispatch console commands or just
 * log state changes.
 */
public final class ODailyQuestsHook {

    private final Logger log;
    private boolean ready = false;

    public ODailyQuestsHook(@NotNull Logger log) {
        this.log = log;
        init();
    }

    private void init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ODailyQuests");
        if (plugin == null || !plugin.isEnabled()) return;
        ready = true;
        log.info("[Hooks/ODailyQuests] Hook active.");
    }

    public boolean isReady() { return ready; }

    public void onStateChange(@NotNull WorldState state) {
        if (!ready) return;
        // Future integration: enable/disable quest groups based on state.getId()
        log.fine("[Hooks/ODailyQuests] Notified of state change: " + state.getId());
    }
}
