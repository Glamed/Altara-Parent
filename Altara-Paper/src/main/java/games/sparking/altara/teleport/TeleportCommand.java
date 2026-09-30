package games.sparking.altara.teleport;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.CommandCooldown;
import games.sparking.altara.command.annotation.Flag;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.teleport.packet.TeleportRequestPacket;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

public class TeleportCommand {

    @Command(names = {"teleport", "tp", "goto", "visit"},
            description = "Join a player",
            playerOnly = true,
            async = true
    )
    @CommandCooldown(time = 5)
    public boolean teleport(Player sender, @Param(name = "player") Profile target, @Flag(names = {"v", "-v", "vanish", "-vanish"}, description = "Vanished?", defaultValue = false) boolean vanished) {
        if (sender.getUniqueId().equals(target.getUuid())) {
            sender.sendMessage(CC.error(
                    "Unable to teleport.",
                    "You cannot teleport to yourself."
            ));
            return false;
        }

        new TeleportRequestPacket(
                sender.getUniqueId(),
                target.getUuid(),
                vanished
        ).publish();

        return true;
    }
}