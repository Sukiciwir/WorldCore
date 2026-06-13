package dev.worldcore.title.hooks;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * LuckPerms hook — sets a prefix node on the player's user object.
 *
 * <p><strong>Priority 1000</strong> is used so WorldCore titles sit above
 * group prefixes but below operator-set nodes.
 */
public final class LuckPermsHook {

    private static final int PREFIX_PRIORITY = 1000;

    private final Logger log;
    private LuckPerms api;
    private boolean ready = false;

    public LuckPermsHook(@NotNull Logger log) {
        this.log = log;
        try {
            api   = LuckPermsProvider.get();
            ready = true;
            log.info("[Titles/LuckPerms] Hook active.");
        } catch (Throwable e) {
            log.fine("[Titles/LuckPerms] Not available.");
        }
    }

    public boolean isReady() { return ready; }

    public void setPrefix(@NotNull Player player, @NotNull String prefix) {
        if (!ready) return;
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return;

        // Remove any previous WorldCore prefix nodes at our priority
        user.data().clear(NodeType.PREFIX.predicate(p -> p.getPriority() == PREFIX_PRIORITY));

        if (!prefix.isEmpty()) {
            user.data().add(PrefixNode.builder(prefix, PREFIX_PRIORITY).build());
        }
        api.getUserManager().saveUser(user);
    }
}
