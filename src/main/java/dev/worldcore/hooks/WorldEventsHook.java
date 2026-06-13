package dev.worldcore.hooks;

import com.worldevents.api.WorldEventsAPI;
import com.worldevents.events.WorldEventEndEvent;
import com.worldevents.events.WorldEventStartEvent;
import com.worldevents.managers.EventManager;
import com.worldevents.models.WorldEvent;
import dev.worldcore.state.WorldState;
import dev.worldcore.state.WorldStateManager;
import dev.worldcore.state.WorldStateType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Logger;

/**
 * WorldEvents hook.
 *
 * <p>Listens to {@link WorldEventStartEvent} and {@link WorldEventEndEvent} to
 * register/unregister active event states with the {@link WorldStateManager}.
 */
public final class WorldEventsHook implements Listener {

    private final WorldStateManager stateManager;
    private final Logger            log;
    private final Plugin            plugin;

    private WorldEventsAPI weApi;
    private EventManager   weEventManager;

    public WorldEventsHook(@NotNull Plugin plugin, @NotNull WorldStateManager stateManager,
                           @NotNull Logger log) {
        this.plugin       = plugin;
        this.stateManager = stateManager;
        this.log          = log;
    }

    public boolean load() {
        Plugin p = Bukkit.getPluginManager().getPlugin("WorldEvents");
        if (p == null || !p.isEnabled()) return false;

        if (p instanceof WorldEventsAPI api) {
            this.weApi = api;
        } else {
            // Some plugins implement API in the main class but don't expose it properly
            // Let's try to get EventManager via reflection if needed, but normally
            // the plugin main class implements the API.
            try {
                if (WorldEventsAPI.class.isAssignableFrom(p.getClass())) {
                    this.weApi = (WorldEventsAPI) p;
                }
            } catch (NoClassDefFoundError ignored) {}
        }

        // The EventManager is usually accessible via a getter or cast.
        // The API signature we decompiled shows EventManager is instantiated.
        // Let's use reflection to find the getEventManager method if needed,
        // or just access it directly if we can.
        try {
            Method getManager = p.getClass().getMethod("getEventManager");
            this.weEventManager = (EventManager) getManager.invoke(p);
        } catch (Throwable e) {
            log.warning("[Hook/WorldEvents] Could not get EventManager: " + e.getMessage());
        }

        if (weEventManager != null) {
            // Sync current active events
            try {
                List<WorldEvent> active = weEventManager.getActiveEvents();
                if (active != null) {
                    for (WorldEvent we : active) {
                        String id = generateEventId(we);
                        stateManager.registerProvider(new ActiveEventState(id, we.getDisplayName()));
                    }
                }
            } catch (Throwable t) {
                log.warning("[Hook/WorldEvents] Failed to sync initial events: " + t.getMessage());
            }
        }

        Bukkit.getPluginManager().registerEvents(this, plugin);
        log.info("[Hook/WorldEvents] Hooked into WorldEvents.");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEventStart(WorldEventStartEvent event) {
        String id = event.getEventId();
        if (id == null) id = generateEventId(event.getEvent());
        String name = event.getEvent().getDisplayName();

        ActiveEventState state = new ActiveEventState(id, name);
        stateManager.registerProvider(state);
        log.fine("[Hook/WorldEvents] Event started: " + id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEventEnd(WorldEventEndEvent event) {
        String id = event.getEventId();
        if (id == null) id = generateEventId(event.getEvent());

        // Find and remove matching provider
        for (WorldState p : stateManager.getProviders()) {
            if (p instanceof ActiveEventState aes && aes.getId().equals(id)) {
                stateManager.removeProvider(p);
                log.fine("[Hook/WorldEvents] Event ended: " + id);
                break;
            }
        }
    }

    private String generateEventId(WorldEvent we) {
        // Fallback if eventId isn't easily accessible
        return we.getName().toLowerCase().replace(" ", "_");
    }

    // ─── Inner WorldState Provider ────────────────────────────────────────────

    private static final class ActiveEventState implements WorldState {
        private final String id;
        private final String displayName;

        ActiveEventState(@NotNull String id, @NotNull String displayName) {
            this.id          = id;
            this.displayName = displayName;
        }

        @Override @NotNull public String getId()          { return id; }
        @Override @NotNull public String getDisplayName() { return displayName; }
        @Override          public int    getPriority()    { return 100; } // Events override seasons
        @Override          public boolean isActive()      { return true; }
        @Override @NotNull public WorldStateType getType() { return WorldStateType.EVENT; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ActiveEventState that = (ActiveEventState) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
}
