package games.sparking.altara.scoreboard;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ScoreboardListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);

        Scoreboard scoreboard = new Scoreboard(user, "sidebar", "sidebar", true);
        scoreboard.create();
        scoreboard.display();

        ScoreboardService.scoreboards.put(event.getPlayer().getUniqueId(), scoreboard);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ScoreboardService.scoreboards.remove(event.getPlayer().getUniqueId());
    }
}
