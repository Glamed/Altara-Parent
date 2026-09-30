package games.sparking.altara.selector;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraLobby;
import games.sparking.altara.configuration.StaticConfiguration;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One realm in the lobby selector (and its NPC).  {@link #description} and {@link #npcLines}
 * are MiniMessage and may use {@code %status%}, {@code %online%}, {@code %max%},
 * {@code %in_queue%} and {@code %rank_on_scope%}.
 */
@NoArgsConstructor
@Data
public class ServerSelectorEntry implements StaticConfiguration {

    private int slot = 22;
    private Material material = Material.COMMAND_BLOCK_MINECART;
    private String name = "<aqua><bold>Dev";
    private String serverName = "Dev";
    private List<String> description = Arrays.asList(
            "<gray>A development realm.",
            "",
            "<gray>- Status: %status%",
            "<gray>- Players: <white>%online%<gray>/<white>%max%",
            "<gray>- Queue: <white>%in_queue%",
            "<gray>- Your rank: %rank_on_scope%"
    );

    private LocationConfig npcLocation = new LocationConfig("world", 63, 68, 88);
    private String npcSkin = "Notch";
    private List<String> npcLines = Arrays.asList(
            "<aqua><bold>Dev",
            "<gray>Players: <white>%online%<gray>/<white>%max%",
            "<gray>Queue: <white>%in_queue%",
            "<gray>Your rank: %rank_on_scope%"
    );

    public ServerInfo getServer() {
        return ServerInfo.getServerInfo(serverName);
    }

    public ItemStack toItem(Player player) {
        ServerInfo server = getServer();
        boolean online = server != null && server.isOnline();

        List<Component> lore = new ArrayList<>(CC.format(resolve(description, player)));
        if (online) {
            lore.add(Component.empty());
            lore.add(Gui.cta("join the queue"));
        }

        Component displayName = CC.format(name);
        if (!online) {
            displayName = displayName.append(Component.space())
                    .append(CC.bracketed(Component.text("Offline", Theme.ERROR)));
        }

        return new ItemBuilder(online ? material : Material.GRAY_DYE)
                .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                .setDisplayName(displayName)
                .setLore(lore)
                .build();
    }

    /** Fills the {@code %placeholders%} of the given MiniMessage lines for {@code player}. */
    public List<String> resolve(List<String> lines, Player player) {
        ServerInfo server = getServer();
        String online = String.valueOf(server != null ? server.getOnlinePlayers() : 0);
        String max = String.valueOf(server != null ? server.getMaxPlayers() : 0);
        String status = statusOf(player, server);
        String queue = CC.escape(queueStatus(player, server));
        String rank = rankOnScope(player, server);

        List<String> resolved = new ArrayList<>(lines.size());
        for (String line : lines) {
            resolved.add(line
                    .replace("%status%", status)
                    .replace("%online%", online)
                    .replace("%max%", max)
                    .replace("%in_queue%", queue)
                    .replace("%rank_on_scope%", rank));
        }
        return resolved;
    }

    public static String statusOf(Player player, ServerInfo server) {
        if (server == null) return "<red>Offline";
        return switch (server.getState()) {
            case ONLINE -> "<green>Online";
            case WHITELISTED -> "<yellow>Whitelisted";
            case OFFLINE -> "<red>Offline";
            case HEARTBEAT_TIMEOUT -> "<red>Offline" + (player.isOp() ? " <dark_gray>(no heartbeat)" : "");
            case UNKNOWN -> "<red>Offline" + (player.isOp() ? " <dark_gray>(unknown)" : "");
        };
    }

    public static String queueStatus(Player player, ServerInfo server) {
        if (server == null) return "None";
        QueueService queueService = AltaraLobby.getLobbyInstance().getQueueService();
        int total = queueService.getQueueing(server.getName()).size();
        int position = queueService.getPosition(player.getUniqueId(), server.getName());
        return position >= 0 ? "#" + (position + 1) + " of " + total : total + " waiting";
    }

    /** The viewer's rank on the realm's scope (staff-authored MiniMessage). */
    public static String rankOnScope(Player player, ServerInfo server) {
        if (server == null) return "<gray>N/A";
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        Grant grant = profile == null ? null : profile.getCurrentGrantOn(server.getName());
        return grant != null && grant.asRank() != null ? grant.asRank().getDisplayName() : "<gray>N/A";
    }
}
