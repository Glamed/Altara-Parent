package games.sparking.altara.playersetting.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.playersetting.menu.SettingsMenu;
import org.bukkit.entity.Player;

public class SettingsCommands {

    @Command(names = {"settings", "options", "preferences", "prefs"},
            description = "Open the settings menu")
    public boolean settings(Player sender) {
        new SettingsMenu().openMenu(sender);
        return true;
    }

}
