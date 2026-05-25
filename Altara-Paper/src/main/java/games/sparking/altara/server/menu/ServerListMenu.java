package games.sparking.altara.server.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.queue.packet.QueueSendPlayerPacket;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public class ServerListMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "Servers";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<ServerInfo> servers = new ArrayList<>(ServerInfo.getServers());
        servers.sort(Comparator.comparing(ServerInfo::isProxy).reversed().thenComparing(ServerInfo::getName));
        servers.forEach(server -> buttons.put(buttons.size(), new ServerButton(server)));
        return buttons;
    }

    @AllArgsConstructor
    public class ServerButton extends Button {

        private final ServerInfo server;

        @Override
        public ItemStack getItem(Player player) {
            List<String> lore = new ArrayList<>();
            int players = server.isOnline() ? server.getOnlinePlayers() : 0;

            lore.add(CC.MENU_BAR);
            lore.add(CC.YELLOW + "Grant Scope: " + CC.RED + server.getGroup());
            lore.add(CC.YELLOW + "Players: " + CC.RED + players + CC.YELLOW + "/" + CC.RED + server.getMaxPlayers());
            if (server.isQueueEnabled())
                lore.add(CC.format("&ePlayers Queued: &c%d%s",
                        server.getPlayersInQueue(), server.isQueuePaused() ? CC.GRAY + " (Paused)" : ""));
            if (server.isOnline()) {
                if (!server.isProxy()) {
                    lore.add(CC.YELLOW + "TPS: " + CC.RED + formatTps(server.getTps()));
                    lore.add(CC.YELLOW + "Full tick: " + CC.RED + (Math.round(server.getFullTick() * 10.0D) / 10.0D) + "ms");
                }
                lore.add(CC.YELLOW + "Memory: " + CC.RED + server.getUsedMemory() + "mb" + CC.YELLOW + "/"
                        + CC.RED + server.getAllocatedMemory() + "mb");
            }
            lore.add(" ");
            lore.add(CC.YELLOW + "State: " + server.getState().getInternalName());
            if (server.getState() == ServerState.HEARTBEAT_TIMEOUT) {
                lore.add(CC.YELLOW + "Last Heartbeat: " + CC.RED + Time.formatTimeAgo(server.getLastHeartbeat()));
            } else if (server.getState() == ServerState.OFFLINE) {
                lore.add(CC.YELLOW + "Last Online: " + CC.RED + Time.formatTimeAgo(server.getLastHeartbeat()));
            }
            if (!server.isProxy()) {
                lore.add(" ");
                lore.add(CC.GRAY + CC.ITALIC + "Click to connect.");
            }
            lore.add(CC.MENU_BAR);

            return new ItemBuilder(server.isOnline() ? Material.PAPER : Material.MAP)
                    .setDisplayName(CC.YELLOW + server.getName())
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!server.isProxy())
                new QueueSendPlayerPacket(server.getName(), player.getUniqueId()).publish();
        }
    }

    private String formatTps(double tps) {
        return String.valueOf(Math.min(Math.round(tps * 10.0) / 10.0, 20.0));
    }
}
