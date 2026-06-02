package games.sparking.altara.hologram;

import games.sparking.altara.server.ServerInfo;

/**
 * Resolves hologram text placeholders for a specific viewer.
 *
 * <p>Supported placeholders:
 * <ul>
 *   <li>{@code %player%}       — the player's username</li>
 *   <li>{@code %displayname%}  — the player's display name (includes rank prefix if set)</li>
 * </ul>
 *
 * <p>Add new entries to {@link #resolve} to extend the system without touching
 * any hologram rendering code.
 */
public final class PlaceholderResolver {

    private PlaceholderResolver() {}

    /** Returns {@code true} if {@code text} contains at least one resolvable placeholder. */
    public static boolean hasPlaceholders(String text) {
        return text != null && text.contains("%");
    }

    /**
     * Resolves all known placeholders in {@code text} for the given viewer.
     *
     * @param text   raw hologram line text (may be {@code null})
     * @param player the player whose data should be substituted
     * @return the resolved string, or {@code null} if {@code text} was {@code null}
     */
    public static String resolve(String text, org.bukkit.entity.Player player) {
        if (text == null || !hasPlaceholders(text)) return text;
        return text
                .replaceAll("%player%",      player.getName())
                .replaceAll("%displayname%", player.getDisplayName())
                .replaceAll("%global_online%", String.valueOf(ServerInfo.getGlobalPlayerCount()))
                .replaceAll("%global_max%", String.valueOf(ServerInfo.getGlobalPlayerCount() + 1));
    }
}

