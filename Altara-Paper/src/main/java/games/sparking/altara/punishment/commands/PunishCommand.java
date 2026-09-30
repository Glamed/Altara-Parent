package games.sparking.altara.punishment.commands;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.profile.UnloadedProfile;
import games.sparking.altara.punishment.PunishTarget;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.menu.PunishMenu;
import games.sparking.altara.punishment.menu.PunishmentModifyMenu;
import games.sparking.altara.task.Tasks;
import org.bukkit.entity.Player;

import java.util.List;

public class PunishCommand {

    @Command(names = {"punish", "p"}, permission = "altara.punish", playerOnly = true, async = true,
            description = "Punish a player")
    public void punish(Player sender, @Param(name = "player") UnloadedProfile target) {
        // Load history first so recommendations reflect the player's standing.
        Altara.getSharedInstance().getPunishmentService().loadPunishments(target.getUuid());
        Tasks.run(() -> new PunishMenu(new PunishTarget(target.getUuid(), target.getName())).openMenu(sender));
    }

    @Command(names = {"punishmodify", "pmod"}, permission = "altara.punish", playerOnly = true, async = true,
            description = "View and revoke a player's punishments")
    public void punishModify(Player sender, @Param(name = "player") UnloadedProfile target) {
        PunishTarget punishTarget = new PunishTarget(target.getUuid(), target.getName());
        List<Punishment> history = Altara.getSharedInstance().getPunishmentService().loadPunishments(target.getUuid());
        Tasks.run(() -> new PunishmentModifyMenu(punishTarget, history, new PunishMenu(punishTarget)).openMenu(sender));
    }
}
