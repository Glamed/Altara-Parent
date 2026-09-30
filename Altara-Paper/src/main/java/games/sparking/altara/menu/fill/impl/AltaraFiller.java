package games.sparking.altara.menu.fill.impl;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.IMenuFiller;
import org.bukkit.entity.Player;

import java.util.Map;

/** Light-blue frame around the perimeter and light-gray background everywhere else. */
public class AltaraFiller implements IMenuFiller {

    @Override
    public void fill(Menu menu, Player player, Map<Integer, Button> buttons, int size) {
        Button frame = Button.createPlaceholder(Gui.framePane());
        Button background = Button.createPlaceholder(Gui.backgroundPane());

        for (int slot = 0; slot < size; slot++) {
            buttons.putIfAbsent(slot, BorderFiller.isBorder(slot, size) ? frame : background);
        }
    }
}
