package games.sparking.altara.selector;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraLobby;
import games.sparking.altara.configuration.StaticConfiguration;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor
@Data
public class ServerSelectorEntry implements StaticConfiguration {

    private int slot = 22;
    private Material material = Material.COMMAND_BLOCK_MINECART;
    private short subId = 0;
    private String name = "&a&lDev";
    private String serverName = "Dev";
    private List<String> description = Arrays.asList(
            "&7&m------------------------",
            "&7Description",
            "&fStatus: %status%",
            "&fPlayers: &a%online%&f/&a%max%",
            "&fLives: &a%lives%",
            "&fDeathban: &a%deathban_remaining%",
            "&7&m------------------------"
    );

    private LocationConfig npcLocation = new LocationConfig("world", 63, 68, 88);
    private String npcSkin = "Notch";
    private List<String> npcLines = Arrays.asList(
            "&5&lDev",
            "&7&m------------------------",
            "&fPlayers: &d%online%&f/&d%max%",
            "&fIn Queue: &d%in_queue%",
            "&fYour Rank: %rank_on_scope%",
            "&7&m------------------------"
    );

    public ItemStack toItem(Player player) {
        ServerInfo server = getServer();

        final int onlineCount = (server != null) ? server.getOnlinePlayers() : 0;
        final int maxCount = (server != null) ? server.getMaxPlayers() : 0;
        final String state = (server != null) ? getStateName(player, server) : "Offline";
        final String inQueueDisplay = (server != null) ? getQueueStatus(player, server) : "";
        final String rankOnScope = (server != null)
                ? Altara.getSharedInstance()
                .getProfileService()
                .getProfile(player)
                .getCurrentGrantOn(server.getName())
                .asRank()
                .getDisplayName()
                : "N/A";

        final Material displayMaterial = (server != null) ? material : Material.REDSTONE_BLOCK;
        final String displayName = name + ((server == null) ? " <red>(Offline)" : "");

        return new ItemBuilder(displayMaterial)
                .setDisplayName(displayName)
                .setLore(description.stream()
                        .map(s -> CC.translate(s
                                .replace("%status%", state)
                                .replace("%online%", String.valueOf(onlineCount))
                                .replace("%max%", String.valueOf(maxCount))
                                .replace("%rank_on_scope%", rankOnScope)
                                .replace("%in_queue%", inQueueDisplay)))
                        .collect(Collectors.toList()))
                .build();
    }


    private static String getQueueStatus(Player player, ServerInfo server) {
        QueueService queueService = AltaraLobby.getLobbyInstance().getQueueService();
        boolean isQueueing = queueService.isQueueingFor(player.getUniqueId(), server.getName());
        String inQueueDisplay;

        if (isQueueing) {
            int position = queueService.getPosition(player.getUniqueId(), server.getName());
            int total = queueService.getQueueing(server.getName()).size();
            inQueueDisplay = String.format("Position %d of %d", position + 1, total);
        } else {
            inQueueDisplay = queueService.getQueueing(server.getName()).size() + " in queue";
        }
        return inQueueDisplay;
    }


    public ServerInfo getServer() {
        return ServerInfo.getServerInfo(serverName);
    }

    public static String getStateName(Player player, ServerInfo server) {
        switch (server.getState()) {
            case UNKNOWN:
                return "<red>Offline<gray>" + (player.isOp() ? " (UN)" : "");
            case HEARTBEAT_TIMEOUT:
                return "<red>Offline<gray>" + (player.isOp() ? " (HB)" : "");
            case OFFLINE:
                return "<red>Offline";
            case WHITELISTED:
                return "<yellow>Whitelisted";
            case ONLINE:
                return "<green>Online";
        }
        return "<red>Offline";
    }

}
