package games.sparking.altara.menu.page;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

/** Previous / next page arrow. Shift-click jumps to the first / last page. */
public class PageButton extends Button {

    private final int mod;
    private final PagedMenu menu;

    public PageButton(int mod, PagedMenu menu) {
        this.mod = mod;
        this.menu = menu;
    }

    @Override
    public ItemStack getItem(Player player) {
        boolean next = mod > 0;
        int target = menu.getPage() + mod;

        return new ItemBuilder(Material.ARROW)
                .setDisplayName(next
                        ? Component.text("Next page", Theme.TEXT_STRONG).append(Component.text(" ❱", Theme.TEXT))
                        : Component.text(Theme.BACK + " ", Theme.TEXT).append(Component.text("Previous page", Theme.TEXT_STRONG)))
                .setLore(Gui.lore()
                        .value("Page", target + "/" + menu.getPages(player))
                        .cta(next ? "view the next page" : "view the previous page")
                        .build())
                .build();
    }

    @Override
    public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
        if (clickType.isShiftClick()) {
            menu.modPage(player, mod > 0 ? menu.getPages(player) : -menu.getPage());
        } else {
            menu.modPage(player, mod);
        }
    }
}
