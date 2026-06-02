package games.sparking.altara.hologram.updating;

import org.bukkit.entity.Player;

import java.util.List;

/**
 * Supplies the hologram lines for a specific player.
 *
 * <p>Implement this interface to create per-player hologram content €" for
 * example, a hologram above an NPC that shows the player's rank or a server's
 * live player count.
 *
 * <p>Register with an {@link UpdatingHologram} via
 * {@link UpdatingHologramBuilder#getProvider()}.
 */
public interface HologramProvider {

    /**
     * Returns the list of text lines to display for {@code player}.
     * Each string may contain legacy colour codes ({@code &}) and any
     * custom placeholders that should be resolved server-side.
     *
     * @param player the player who is viewing the hologram
     * @return ordered list of line strings (top to bottom)
     */
    List<String> getLines(Player player);
}

