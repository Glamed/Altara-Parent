package games.sparking.altara.hologram.clickhandler;

import games.sparking.altara.hologram.Hologram;
import org.bukkit.entity.Player;

public interface HologramClickHandler {

    void click(Player player, Hologram hologram, ClickType clickType);

    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }

}
