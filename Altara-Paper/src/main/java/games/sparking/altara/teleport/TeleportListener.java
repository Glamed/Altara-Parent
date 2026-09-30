package games.sparking.altara.teleport;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class TeleportListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawnLocation(PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();

        TeleportService service = AltaraPaper.getPaperInstance().getTeleportService();
        TeleportService.PendingTeleport pending = service.get(player.getUniqueId());

        if (pending == null) return;

        Player target = player.getServer().getPlayer(pending.getTargetUuid());
        if (target == null) return;

        event.setSpawnLocation(target.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        TeleportService.PendingTeleport pending = AltaraPaper.getPaperInstance()
                .getTeleportService()
                .consume(player.getUniqueId());

        if (pending == null) return;

        Player target = player.getServer().getPlayer(pending.getTargetUuid());
        if (target == null) return;

        player.sendMessage(CC.success(
                "Teleported to *" + target.getName() + "*."
        ));

        if (!pending.isVanish()) {
            target.sendMessage(CC.info(
                    "*" + player.getName() + "* teleported to you."
            ));
        }
    }
}