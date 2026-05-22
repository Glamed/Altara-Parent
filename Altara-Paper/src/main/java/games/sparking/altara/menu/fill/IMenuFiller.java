package games.sparking.altara.menu.fill;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import org.bukkit.entity.Player;

import java.util.Map;

public interface IMenuFiller {

    void fill(Menu menu, Player player, Map<Integer, Button> buttons, int size);

}
