package games.sparking.altara.punishment.commands;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.punishment.menu.PunishMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PunishCommand {

    @Command(
            names       = {"punish"},
            permission  = "altara.punish",
            playerOnly  = true,
            description = "Open the punishment builder for a player"
    )
    public void punish(Player sender, @Param(name = "player") String targetName) {
        new PunishMenu(Bukkit.getOfflinePlayer(targetName)).openMenu(sender);
    }
}

