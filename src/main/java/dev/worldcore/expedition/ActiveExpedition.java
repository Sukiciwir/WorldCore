package dev.worldcore.expedition;

import org.jetbrains.annotations.NotNull;

/**
 * A running expedition instance for a specific player.
 *
 * @param expeditionId  ID of the expedition definition.
 * @param startedAt     Unix timestamp (ms) when started.
 * @param endsAt        Unix timestamp (ms) when it completes.
 */
public record ActiveExpedition(
        @NotNull String expeditionId,
        long startedAt,
        long endsAt
) {

    /** @return true if this expedition is ready to claim. */
    public boolean isReady() {
        return System.currentTimeMillis() >= endsAt;
    }

    /** @return Time remaining in seconds (0 if ready). */
    public long secondsRemaining() {
        return Math.max(0L, (endsAt - System.currentTimeMillis()) / 1000L);
    }

    /** @return Human-readable time remaining (e.g. "2h 30m"). */
    @NotNull
    public String formatRemaining() {
        long secs = secondsRemaining();
        if (secs == 0) return "Ready!";
        long hours   = secs / 3600;
        long minutes = (secs % 3600) / 60;
        long seconds = secs % 60;
        if (hours > 0)   return hours   + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
