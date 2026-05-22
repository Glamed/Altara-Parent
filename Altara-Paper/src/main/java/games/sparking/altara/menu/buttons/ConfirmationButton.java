package games.sparking.altara.menu.buttons;

import games.sparking.altara.menu.Button;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Style;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class ConfirmationButton extends Button {

    private final boolean bool;
    private final Consumer<Boolean> callable;
    private String name;

    public ConfirmationButton(boolean bool, Consumer<Boolean> callable) {
        this.bool = bool;
        this.callable = callable;
    }

    public ConfirmationButton(boolean bool, String name, Consumer<Boolean> callable) {
        this.bool = bool;
        this.callable = callable;
        this.name = name;
    }

    @Override
    public ItemStack getItem(Player player) {
        return new ItemBuilder(bool ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .setDisplayName(CC.text(
                        name,
                        bool ? Style.GREEN.getColor() : Style.RED.getColor(),
                        Style.BOLD.getDecoration()
                ))
                .build();
    }

    @Override
    public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
        player.closeInventory();
        callable.accept(bool);
    }
}