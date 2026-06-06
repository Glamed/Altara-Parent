package games.sparking.altara.menu.buttons;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public class BackButton extends Button {

    private final Menu menu;

    public BackButton(Menu menu) {
        this.menu = menu;
    }

    @Override
    public ItemStack getItem(Player player) {
        /*List<String> lore = new ArrayList<>();
        if (menu instanceof PagedMenu) {
            lore.add(NamedTextColor.GRAY + "To: " + ((PagedMenu) menu).getRawTitle(player));
        } else {
            lore.add(NamedTextColor.GRAY + "To: " + menu.getTitle(player));
        }*/
        return new ItemBuilder(Material.RED_BED).setDisplayName("<red><b>Go Back").build();
    }

    @Override
    public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
        menu.openMenu(player);
    }
}
