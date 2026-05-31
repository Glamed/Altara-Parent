package games.sparking.altara.hologram.clickhandler;

import games.sparking.altara.hologram.Hologram;
import org.bukkit.entity.Player;

public interface HologramClickHandler {

    /**
     * @param player    the player who clicked
     * @param hologram  the hologram that was clicked
     * @param lineIndex 0-based index of the line that was clicked (-1 if unknown)
     * @param clickType LEFT_CLICK or RIGHT_CLICK
     */
    void click(Player player, Hologram hologram, int lineIndex, ClickType clickType);

    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }

}
