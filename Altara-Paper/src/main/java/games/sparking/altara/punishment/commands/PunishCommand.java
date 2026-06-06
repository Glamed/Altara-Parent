package games.sparking.altara.punishment.commands;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.punishment.menu.PunishMenu;
import games.sparking.altara.punishment.menu.PunishmentModifyMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PunishCommand {

    @Command(names = {"punish", "p"}, permission = "altara.punish", playerOnly = true, description = "Open the punishment menu for a player")
    public void punish(Player sender, @Param(name = "player") String targetName) {
        new PunishMenu(Bukkit.getOfflinePlayer(targetName)).openMenu(sender);
    }

    @Command(names = {"punishmodify", "pmod"}, permission = "altara.punish", playerOnly = true, description = "Open the punishment modify menu for a player")
    public void punishModify(Player sender, @Param(name = "player") String targetName) {
        new PunishmentModifyMenu(Bukkit.getOfflinePlayer(targetName)).openMenu(sender);
    }

}
