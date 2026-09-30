package games.sparking.altara.report.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.report.ReportCategory;
import games.sparking.altara.report.ReportSubmitCommand;
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

/** Lets the reporter pick a locally-online player once a category has already been chosen. */
public class ReportTargetMenu extends PagedMenu {

    private final ReportCategory category;

    public ReportTargetMenu(ReportCategory category) {
        this.category = category;
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Report", category.getDisplayName()};
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
            buttons.put(index++, new TargetButton(target));
        }
        return buttons;
    }

    @RequiredArgsConstructor
    private class TargetButton extends Button {

        private final Player target;

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(target)
                    .setDisplayName(Component.text(target.getName(), Theme.TEXT_STRONG))
                    .setLore(Gui.lore().cta("report this player").build())
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            ReportSubmitCommand.confirmAndSubmit(whoClicked, target.getUniqueId(), target.getName(), category);
        }
    }
}
