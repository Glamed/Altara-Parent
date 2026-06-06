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
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public class ServerListMenu extends Menu {

    @Override
    public Component getTitle(Player player) {
        return CC.format("Servers");
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
            List<Component> lore = new ArrayList<>();
            int players = server.isOnline() ? server.getOnlinePlayers() : 0;

            lore.add(CC.MENU_BAR);
            lore.add(CC.format("<yellow>Grant Scope: <red>%s", server.getGroup()));
            lore.add(CC.format("<yellow>Players: <red>%d<yellow>/<red>%d", players, server.getMaxPlayers()));
            if (server.isQueueEnabled())
                lore.add(CC.format("<yellow>Players Queued: <red>%d%s",
                        server.getPlayersInQueue(), server.isQueuePaused() ? " <gray>(Paused)" : ""));
            if (server.isOnline()) {
                if (!server.isProxy()) {
                    lore.add(CC.format("<yellow>TPS: <red>%s", formatTps(server.getTps())));
                    lore.add(CC.format("<yellow>Full tick: <red>%sms", Math.round(server.getFullTick() * 10.0D) / 10.0D));
                }
                lore.add(CC.format("<yellow>Memory: <red>%dmb<yellow>/<red>%dmb",
                        server.getUsedMemory(), server.getAllocatedMemory()));
            }
            lore.add(CC.format(" "));
            lore.add(CC.format("<yellow>State: %s", server.getState().getInternalName()));
            if (server.getState() == ServerState.HEARTBEAT_TIMEOUT) {
                lore.add(CC.format("<yellow>Last Heartbeat: <red>%s", Time.formatTimeAgo(server.getLastHeartbeat())));
            } else if (server.getState() == ServerState.OFFLINE) {
                lore.add(CC.format("<yellow>Last Online: <red>%s", Time.formatTimeAgo(server.getLastHeartbeat())));
            }
            if (!server.isProxy()) {
                lore.add(CC.format(" "));
                lore.add(CC.format("<gray><italic>Click to connect."));
            }
            lore.add(CC.MENU_BAR);

            return new ItemBuilder(server.isOnline() ? Material.PAPER : Material.MAP)
                    .setDisplayName(CC.format("<yellow>%s", server.getName()))
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
