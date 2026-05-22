package games.sparking.altara.menu.fill.impl;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.IMenuFiller;
import games.sparking.altara.menu.page.PagedMenu;
import org.bukkit.entity.Player;

import java.util.Map;

public class BorderFiller implements IMenuFiller {

    @Override
    public void fill(Menu menu, Player player, Map<Integer, Button> buttons, int size) {
        int startIndex = menu instanceof PagedMenu ? 8 : 0;
        for (int i = startIndex; i < size; i++) {
            if (i < startIndex + 9) {
                buttons.putIfAbsent(i, Button.createPlaceholder(menu.getPlaceholderItem(player)));
                buttons.putIfAbsent(i + (size - 9), Button.createPlaceholder(menu.getPlaceholderItem(player)));
            }

            if (i % 9 == 0) {
                buttons.putIfAbsent(i, Button.createPlaceholder(menu.getPlaceholderItem(player)));
                buttons.putIfAbsent(i + 8, Button.createPlaceholder(menu.getPlaceholderItem(player)));
            }
        }
    }

}
