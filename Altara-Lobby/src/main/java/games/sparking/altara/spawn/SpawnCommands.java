package games.sparking.altara.spawn;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.configuration.defaults.LocationConfig;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public class SpawnCommands {

    @Command(names = "spawn", playerOnly = true)
    public boolean spawnCommand(Player player) {
        if (AltaraLobby.getLobbyInstance().getLobbyConfig().getSpawnLocation() == null) {
            player.sendMessage("<red>The spawn location is not set, " +
                    "please contact a server administrator.");
            return true;
        }

        player.teleport(AltaraLobby.getLobbyInstance().getLobbyConfig().getSpawnLocation().getLocation());
        return true;
    }

    @Command(names = "setspawn", playerOnly = true,
            permission = "osmium.command.setspawn",
            async = true)
    public boolean setSpawnCommand(Player player) {
        AltaraLobby.getLobbyInstance().getLobbyConfig().setSpawnLocation(new LocationConfig(player.getLocation()));
        AltaraLobby.getLobbyInstance().getLobbyConfig().saveConfig();

        player.sendMessage(ChatColor.GREEN + "You have set the spawn location.");
        return true;
    }
}
