package games.sparking.altara.hologram;

import games.sparking.altara.server.ServerInfo;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

/**
 * Resolves hologram text placeholders for a specific viewer.
 *
 * <ul>
 *   <li>{@code %player%}        — the viewer's username</li>
 *   <li>{@code %displayname%}   — the viewer's display name (rank colour)</li>
 *   <li>{@code %global_online%} — players online across the network</li>
 *   <li>{@code %global_max%}    — the network "max" shown alongside it</li>
 * </ul>
 */
public final class PlaceholderResolver {

    private PlaceholderResolver() {}

    public static boolean hasPlaceholders(String text) {
        return text != null && text.contains("%");
    }

    /** Plain {@code replace} — placeholder values may contain {@code $} or other regex characters. */
    public static String resolve(String text, Player player) {
        if (!hasPlaceholders(text)) return text;

        int online = ServerInfo.getGlobalPlayerCount();
        return text
                .replace("%player%", player.getName())
                .replace("%displayname%", MiniMessage.miniMessage().serialize(player.displayName()))
                .replace("%global_online%", String.valueOf(online))
                .replace("%global_max%", String.valueOf(online + 1));
    }
}
