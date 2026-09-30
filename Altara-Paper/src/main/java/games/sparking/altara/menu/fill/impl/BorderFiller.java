package games.sparking.altara.menu.fill.impl;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.IMenuFiller;
import org.bukkit.entity.Player;

import java.util.Map;

public class BorderFiller implements IMenuFiller {

    @Override
    public void fill(Menu menu, Player player, Map<Integer, Button> buttons, int size) {
        Button placeholder = Button.createPlaceholder(menu.getPlaceholderItem(player));
        for (int slot = 0; slot < size; slot++) {
            if (isBorder(slot, size)) buttons.putIfAbsent(slot, placeholder);
        }
    }

    /** True for slots on the outer edge of a chest inventory. */
    public static boolean isBorder(int slot, int size) {
        int column = slot % 9;
        return slot < 9 || slot >= size - 9 || column == 0 || column == 8;
    }
}
