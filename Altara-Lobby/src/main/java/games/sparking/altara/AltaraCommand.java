package games.sparking.altara;

import com.github.retrooper.packetevents.protocol.world.Location;
import games.sparking.altara.command.annotation.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class AltaraCommand {

    @Command(names = "altara", description = "Altara debug / test command", permission = "altara.admin")
    public void altaraCommand(Player player) {
        org.bukkit.Location loc = player.getLocation().add(0, 2.2, 0);
        Location peLoc = new Location(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

        

        player.sendMessage(Component.text("Spawned hologram above you.", NamedTextColor.GREEN));
    }
}

