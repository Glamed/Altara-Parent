package games.sparking.altara.menu.page;

import games.sparking.altara.menu.Button;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class PageButton extends Button {

    private final int mod;
    private final PagedMenu menu;

    public PageButton(int mod, PagedMenu menu) {
        this.mod = mod;
        this.menu = menu;
    }

    @Override
    public ItemStack getItem(Player player) {
        String label = mod > 0 ? "Next Page" : "Previous Page";

        if (this.hasNext(player)) {
            return new ItemBuilder(Material.LIME_CARPET)
                    .setDisplayName(CC.text(label, Style.GREEN.getColor(), Style.BOLD.getDecoration()))
                    .build();
        } else {
            return new ItemBuilder(Material.GRAY_CARPET)
                    .setDisplayName(CC.text(label, Style.GRAY.getColor(), Style.BOLD.getDecoration()))
                    .build();
        }
    }

    @Override
    public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
        if (clickType.isShiftClick()) {
            if (hasNext(player)) {
                this.menu.modPage(player, this.mod > 0 ?
                        this.menu.getPages(player) - this.menu.getPage() :
                        1 - this.menu.getPage());
            }
        } else {
            if (hasNext(player)) {
                this.menu.modPage(player, mod);
            }
        }
    }

    private boolean hasNext(Player player) {
        int pg = this.menu.getPage() + this.mod;
        return pg > 0 && this.menu.getPages(player) >= pg;
    }
}