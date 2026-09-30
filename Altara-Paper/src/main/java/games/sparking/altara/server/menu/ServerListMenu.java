package games.sparking.altara.server.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.queue.packet.QueueSendPlayerPacket;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** {@code /servers}: every known server with live stats; click to connect. */
public class ServerListMenu extends PagedMenu {

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Servers"};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<ServerInfo> servers = new ArrayList<>(ServerInfo.getServers());
        servers.sort(Comparator.comparing(ServerInfo::isProxy).reversed().thenComparing(ServerInfo::getName));
        servers.forEach(server -> buttons.put(buttons.size(), new ServerButton(server)));
        return buttons;
    }

    private static String formatTps(double tps) {
        return String.valueOf(Math.min(Math.round(tps * 10.0) / 10.0, 20.0));
    }

    private static class ServerButton extends Button {

        private final ServerInfo server;

        ServerButton(ServerInfo server) {
            this.server = server;
        }

        @Override
        public ItemStack getItem(Player player) {
            boolean online = server.isOnline();
            Gui.Lore lore = Gui.lore()
                    .value("Group", server.getGroup())
                    .value("Players", server.getOnlinePlayers() + "/" + server.getMaxPlayers());

            if (server.isQueueEnabled()) {
                lore.value("Queued", server.getPlayersInQueue() + (server.isQueuePaused() ? " (paused)" : ""));
            }
            if (online) {
                if (!server.isProxy()) {
                    lore.value("TPS", formatTps(server.getTps()));
                    lore.value("Tick", Math.round(server.getFullTick() * 10.0D) / 10.0D + "ms");
                }
                lore.value("Memory", server.getUsedMemory() + "/" + server.getAllocatedMemory() + " MB");
            } else if (server.getState() == ServerState.HEARTBEAT_TIMEOUT || server.getState() == ServerState.OFFLINE) {
                lore.value("Last seen", Time.formatTimeAgo(server.getLastHeartbeat()));
            }

            lore.value("State", CC.format(server.getState().getInternalName()));
            if (online && !server.isProxy()) lore.cta("connect");

            return new ItemBuilder(server.isProxy() ? Material.ENDER_EYE : online ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setDisplayName(Component.text()
                            .append(CC.statusDot(online))
                            .append(Component.text(" " + server.getName(), Theme.TEXT_STRONG))
                            .build())
                    .setLore(lore.build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!server.isProxy() && server.isOnline()) {
                new QueueSendPlayerPacket(server.getName(), player.getUniqueId()).publish();
            }
        }
    }
}
