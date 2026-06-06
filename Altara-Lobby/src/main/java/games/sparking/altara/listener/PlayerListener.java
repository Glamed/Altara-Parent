package games.sparking.altara.listener;

import games.sparking.altara.AltaraLobby;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location spawnLocation = AltaraLobby.getLobbyInstance().getLobbyConfig().getSpawnLocation().getLocation();

        event.joinMessage(null);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);

        if (spawnLocation != null)
            player.teleport(spawnLocation);


//        AltaraLobby.giveItems(player);
    }

}
