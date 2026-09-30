package games.sparking.altara.staffmode.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.staffmode.StaffMode;
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

/** Paged picker of every locally online player, used to spectate (vanished) or visit (unvanished). */
public class SpectatorMenu extends PagedMenu {

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Staff Mode", "Spectate"};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != player)
                .sorted(Comparator.comparing(Player::getName))
                .toList();

        int index = 0;
        for (Player target : online) {
            buttons.put(index++, new SpectatorButton(target));
        }
        return buttons;
    }

    /** Puts {@code staff} into staff mode + vanish (if not already) and sends them to {@code target}. */
    public static void spectate(Player staff, Player target) {
        if (target == null || !target.isOnline()) return;

        staff.closeInventory();
        if (!StaffMode.isEnabled(staff)) StaffMode.get(staff).toggleEnabled(true);
        if (!StaffMode.isVanished(staff)) StaffMode.get(staff).toggleVanish(true);

        staff.teleportAsync(target.getLocation());
    }

    @RequiredArgsConstructor
    private class SpectatorButton extends Button {

        private final Player target;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(target)
                    .setDisplayName(Component.text(target.getName(), Theme.TEXT_STRONG))
                    .setLore(Gui.lore()
                            .text("Left click to spectate (vanished).")
                            .text("Right click to visit (unvanished).")
                            .build())
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            if (clickType.isLeftClick()) {
                spectate(whoClicked, target);
                return;
            }

            new ConfirmationMenu(
                    new String[]{"Staff Mode", "Spectate"},
                    "Teleport to " + target.getName() + " unvanished?",
                    List.of(),
                    "Teleport",
                    "Cancel",
                    confirmed -> {
                        if (!confirmed) return;
                        whoClicked.teleportAsync(target.getLocation());
                    }
            ).openMenu(whoClicked);
        }
    }
}
