package games.sparking.altara.staffmode.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.staffmode.StaffMode;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Lists locally online staff so a staff member can jump straight to a colleague. */
public class OnlineStaffMenu extends PagedMenu {

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Staff Mode", "Online Staff"};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<Player> staff = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(StaffMode.PERMISSION))
                .sorted(Comparator.comparing(Player::getName))
                .toList();

        int index = 0;
        for (Player online : staff) {
            buttons.put(index++, new StaffButton(online));
        }
        return buttons;
    }

    @RequiredArgsConstructor
    private class StaffButton extends Button {

        private final Player staffMember;

        @Override
        public ItemStack getItem(Player player) {
            boolean enabled = StaffMode.isEnabled(staffMember);
            boolean vanished = StaffMode.isVanished(staffMember);

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(staffMember)
                    .setDisplayName(Component.text(staffMember.getName(), Theme.TEXT_STRONG))
                    .setLore(Gui.lore()
                            .value("Staff Mode", CC.state(enabled))
                            .value("Vanished", CC.state(vanished, "Yes", "No"))
                            .cta("teleport to " + staffMember.getName())
                            .build())
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            whoClicked.closeInventory();
            whoClicked.teleportAsync(staffMember.getLocation()).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) {
                    whoClicked.sendMessage(CC.success("Teleported to *" + staffMember.getName() + "*."));
                } else {
                    whoClicked.sendMessage(CC.error("Teleport failed.", "Unable to teleport to " + staffMember.getName() + "."));
                }
            });
        }
    }
}
