package games.sparking.altara.spawn;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.configuration.LobbyConfig;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SpawnCommands {

    public static final String SET_SPAWN_PERMISSION = "altara.command.setspawn";

    /** The configured spawn, or {@code null} when unset or its world isn't loaded. */
    public static Location spawnLocation() {
        LocationConfig config = AltaraLobby.getLobbyInstance().getLobbyConfig().getSpawnLocation();
        if (config == null) return null;
        Location location = config.getLocation();
        return location.getWorld() == null ? null : location;
    }

    @Command(names = "spawn", description = "Return to spawn")
    public boolean spawn(Player player) {
        Location spawn = spawnLocation();
        if (spawn == null) {
            player.sendMessage(CC.error("No spawn set.", "Ask an administrator to set one with */setspawn*."));
            return false;
        }

        player.teleport(spawn);
        player.sendMessage(CC.info("Teleported to spawn."));
        return true;
    }

    @Command(names = "setspawn", permission = SET_SPAWN_PERMISSION, description = "Set the spawn to your location")
    public boolean setSpawn(Player player) {
        LobbyConfig config = AltaraLobby.getLobbyInstance().getLobbyConfig();
        config.setSpawnLocation(new LocationConfig(player.getLocation()));
        Tasks.runAsync(config::saveConfig);

        player.sendMessage(CC.success("Spawn set.", "Players now spawn at your location."));
        return true;
    }
}
