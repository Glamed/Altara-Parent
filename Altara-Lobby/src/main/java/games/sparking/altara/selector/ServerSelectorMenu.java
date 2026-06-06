package games.sparking.altara.selector;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ServerSelectorMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return "Server Selector";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        AltaraLobby.getLobbyInstance().getLobbyConfig().getServerSelector().forEach(entry ->
                buttons.put(entry.getSlot(), new SelectorItem(entry)));

        if (player.hasPermission("servermanager.command.argument.list")) {
            int slot = getSize() - 1;
            while (buttons.containsKey(slot))
                slot--;

            buttons.put(slot, new AllServersButton());
        }

        return buttons;
    }

    @Override
    public int getSize() {
        return AltaraLobby.getLobbyInstance().getLobbyConfig().getSelectorSize();
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.valueOf(AltaraLobby.getLobbyInstance().getLobbyConfig().getSelectorFiller());
    }

    @RequiredArgsConstructor
    public static class SelectorItem extends Button {

        private final ServerSelectorEntry entry;

        @Override
        public ItemStack getItem(Player player) {
            ServerInfo server = entry.getServer();

            ItemStack itemStack;
            if (server == null || !server.isOnline()) {
                // Offline server: redstone block with "(Offline)"
                itemStack = new ItemBuilder(Material.REDSTONE_BLOCK)
                        .setDisplayName(ChatColor.RED + entry.getServerName() + " (Offline)")
                        .build();
            } else {
                // Online server: use entry's normal item
                itemStack = entry.toItem(player).clone();
            }

            return itemStack;
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            ServerInfo server = entry.getServer();
            if (server != null && server.isOnline()) {
                Bukkit.dispatchCommand(player, "joinqueue " + entry.getServerName());
            } else {
                player.sendMessage(ChatColor.RED + "That server is currently unavailable.");
            }
        }
    }

    public static class AllServersButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.COMMAND_BLOCK)
                    .setDisplayName("<yellow><bold>View all servers")
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            Bukkit.dispatchCommand(player, "servers");
        }
    }
}
