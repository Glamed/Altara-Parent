package games.sparking.altara.selector;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.menu.ServerListMenu;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Messages;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** {@code /selector}: the realms configured in {@code serverSelector}; click one to queue. */
public class ServerSelectorMenu extends Menu {

    /** Same node as {@code /servers}. */
    private static final String SERVER_LIST_PERMISSION = "servermanager.command.argument.list";

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Realms");
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int size = getSize();
        AltaraLobby.getLobbyInstance().getStaffConfig().getServerSelector().forEach(entry -> {
            if (entry.getSlot() >= 0 && entry.getSlot() < size) buttons.put(entry.getSlot(), new SelectorItem(entry));
        });

        buttons.putIfAbsent(Gui.CLOSE_SLOT, Gui.closeButton());
        if (player.hasPermission(SERVER_LIST_PERMISSION)) {
            buttons.putIfAbsent(Gui.backSlot(size), new AllServersButton());
        }
        return buttons;
    }

    @Override
    public int getSize() {
        int size = AltaraLobby.getLobbyInstance().getStaffConfig().getSelectorSize();
        return Math.clamp((size + 8) / 9 * 9, 27, 54);
    }

    @Override
    public FillTemplate getFillTemplate() {
        try {
            return FillTemplate.valueOf(AltaraLobby.getLobbyInstance().getStaffConfig().getSelectorFiller().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return FillTemplate.ALTARA;
        }
    }

    @RequiredArgsConstructor
    private static class SelectorItem extends Button {

        private final ServerSelectorEntry entry;

        @Override
        public ItemStack getItem(Player player) {
            return entry.toItem(player);
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            ServerInfo server = entry.getServer();
            if (server == null || !server.isOnline()) {
                player.sendMessage(CC.error(Messages.REALM_OFFLINE, entry.getServerName()));
                return;
            }
            player.closeInventory();
            Bukkit.dispatchCommand(player, "joinqueue " + server.getName());
        }
    }

    private static class AllServersButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.COMPASS)
                    .setDisplayName(Gui.name("All servers"))
                    .setLore(Gui.lore().text("Every server on the network,", "with live stats.").cta("view them").build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            new ServerListMenu().openMenu(player);
        }
    }
}
