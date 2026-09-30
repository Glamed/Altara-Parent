package games.sparking.altara.staffmode.commands;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.staffmode.StaffMode;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

/** {@code /vanish}: toggles vanish independently of the full staff toolkit. */
public class VanishCommand {

    @Command(names = {"vanish", "v"}, permission = StaffMode.PERMISSION,
            playerOnly = true, description = "Toggle vanish")
    public void vanish(Player sender) {
        StaffMode.get(sender).toggleVanish(false);
    }
}
