package games.sparking.altara.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Colour tier of a chat panel ({@code ----[Title > Sub]----} section).  The line alternates
 * dark gray with {@link #lineAccent}; the title is {@link #title} bold, and command help lines
 * use {@link #command}.
 *
 * <ul>
 *   <li>{@link #PLAYER} — everyday player features (friends, chat, queues you're in…)</li>
 *   <li>{@link #STAFF} — moderation and administration (ranks, punishments, servers…)</li>
 *   <li>{@link #DEV} — developer / build tooling (holograms, NPCs, debug…)</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum Panel {

    PLAYER(NamedTextColor.DARK_AQUA, NamedTextColor.AQUA, NamedTextColor.GREEN),
    STAFF(NamedTextColor.DARK_RED, NamedTextColor.RED, NamedTextColor.RED),
    DEV(NamedTextColor.DARK_PURPLE, NamedTextColor.LIGHT_PURPLE, NamedTextColor.LIGHT_PURPLE);

    /** Alternates with dark gray in the header and footer lines. */
    private final TextColor lineAccent;
    /** The bold heading inside the brackets. */
    private final TextColor title;
    /** The {@code /command} part of help lines. */
    private final TextColor command;
}
