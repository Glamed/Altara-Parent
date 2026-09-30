package games.sparking.altara.staffmode.commands;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.staffmode.StaffMode;
import games.sparking.altara.staffmode.menu.SpectatorMenu;
import games.sparking.altara.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** {@code /spectate [player]}: jumps straight to a locally online player, or opens the picker. */
public class SpectatorCommand {

    @Command(names = {"spectate", "spec", "spectator"}, permission = StaffMode.PERMISSION,
            playerOnly = true, description = "Spectate a player or open the spectator menu")
    public void spectate(Player sender, @Param(name = "player", defaultValue = "") String targetName) {
        if (targetName.isEmpty()) {
            new SpectatorMenu().openMenu(sender);
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(CC.error("Player not found.", "*" + targetName + "* isn't online on this server."));
            return;
        }

        SpectatorMenu.spectate(sender, target);
    }
}
