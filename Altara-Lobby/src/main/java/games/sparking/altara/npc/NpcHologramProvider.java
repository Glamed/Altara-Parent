package games.sparking.altara.npc;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraLobby;
import games.sparking.altara.hologram.updating.HologramProvider;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.selector.ServerSelectorEntry;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Supplies per-player hologram lines for an NPC that represents a game server.
 *
 * <p>Resolves the placeholders declared in {@link ServerSelectorEntry#getNpcLines()}:
 * <ul>
 *   <li>{@code %online%}        – current player count on the target server</li>
 *   <li>{@code %max%}           – max player count on the target server</li>
 *   <li>{@code %in_queue%}      – how many players are queued / the player's queue position</li>
 *   <li>{@code %rank_on_scope%} – the viewing player's rank on that server's scope</li>
 * </ul>
 *
 * <p>Integrated the same way as in ilib: registered via
 * {@link games.sparking.altara.hologram.updating.UpdatingHologramBuilder#provider(HologramProvider)}.
 */
@RequiredArgsConstructor
public class NpcHologramProvider implements HologramProvider {

    private final ServerSelectorEntry entry;

    @Override
    public List<String> getLines(Player player) {
        ServerInfo server = entry.getServer();

        int  onlineCount = (server != null) ? server.getOnlinePlayers() : 0;
        int  maxCount    = (server != null) ? server.getMaxPlayers()    : 0;
        String inQueue   = buildQueueDisplay(player, server);
        String rankScope = buildRankOnScope(player, server);

        List<String> resolved = new ArrayList<>();
        for (String line : entry.getNpcLines()) {
            resolved.add(line
                    .replace("%online%",        String.valueOf(onlineCount))
                    .replace("%max%",           String.valueOf(maxCount))
                    .replace("%in_queue%",      inQueue)
                    .replace("%rank_on_scope%", rankScope));
        }
        return resolved;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static String buildQueueDisplay(Player player, ServerInfo server) {
        if (server == null) return "0 in queue";
        QueueService queueService = AltaraLobby.getLobbyInstance().getQueueService();
        String serverName = server.getName();
        boolean queueing  = queueService.isQueueingFor(player.getUniqueId(), serverName);
        if (queueing) {
            int pos   = queueService.getPosition(player.getUniqueId(), serverName);
            int total = queueService.getQueueing(serverName).size();
            return String.format("Position %d of %d", pos + 1, total);
        }
        return queueService.getQueueing(serverName).size() + " in queue";
    }

    private static String buildRankOnScope(Player player, ServerInfo server) {
        if (server == null) return "N/A";
        try {
            return Altara.getSharedInstance()
                    .getProfileService()
                    .getProfile(player)
                    .getCurrentGrantOn(server.getName())
                    .asRank()
                    .getDisplayName();
        } catch (Exception e) {
            return "N/A";
        }
    }
}

