package dev.worldcore.title;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A cosmetic title definition loaded from {@code titles.yml}.
 *
 * @param id          Unique key.
 * @param display     MiniMessage display name.
 * @param prefix      MiniMessage chat/TAB prefix.
 * @param description How to unlock this title.
 * @param seasonal    Whether this title expires when the season ends.
 */
public record Title(
        @NotNull  String id,
        @NotNull  String display,
        @NotNull  String prefix,
        @NotNull  String description,
                  boolean seasonal
) {}
