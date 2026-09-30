package games.sparking.altara.staffmode.commands;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.staffmode.StaffMode;
import org.bukkit.entity.Player;

/** {@code /staffmode}: toggles the creative/vanish staff toolkit. */
public class StaffModeCommand {

    @Command(names = {"staffmode", "sm", "modmode", "mm"}, permission = StaffMode.PERMISSION,
            playerOnly = true, description = "Toggle staff mode")
    public void staffMode(Player sender) {
        StaffMode.get(sender).toggleEnabled(false);
    }
}
