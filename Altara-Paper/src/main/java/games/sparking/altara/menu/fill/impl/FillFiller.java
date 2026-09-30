package games.sparking.altara.menu.fill.impl;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.IMenuFiller;
import org.bukkit.entity.Player;

import java.util.Map;

public class FillFiller implements IMenuFiller {

    @Override
    public void fill(Menu menu, Player player, Map<Integer, Button> buttons, int size) {
        Button placeholder = Button.createPlaceholder(menu.getPlaceholderItem(player));
        for (int slot = 0; slot < size; slot++) {
            buttons.putIfAbsent(slot, placeholder);
        }
    }
}
