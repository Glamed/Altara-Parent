package games.sparking.altara.selector;

import games.sparking.altara.command.annotation.Command;
import org.bukkit.entity.Player;

public class SelectorCommands {

    @Command(names = {"selector", "realms"}, description = "Open the realm selector")
    public boolean selector(Player sender) {
        new ServerSelectorMenu().openMenu(sender);
        return true;
    }
}
