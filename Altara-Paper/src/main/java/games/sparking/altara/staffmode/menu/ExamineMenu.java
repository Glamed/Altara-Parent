package games.sparking.altara.staffmode.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Read-only view of a player's inventory, armor, health and active effects, opened by the Inspect hotbar item. */
public class ExamineMenu extends Menu {

    private final Player target;

    public ExamineMenu(Player target) {
        this.target = target;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Examine", target.getName());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        if (!target.isOnline()) {
            player.closeInventory();
            return buttons;
        }

        ItemStack[] contents = target.getInventory().getContents();
        for (int i = 0; i < contents.length && i < 36; i++) {
            putIfPresent(buttons, i, contents[i]);
        }

        putIfPresent(buttons, 36, target.getInventory().getHelmet());
        putIfPresent(buttons, 37, target.getInventory().getChestplate());
        putIfPresent(buttons, 38, target.getInventory().getLeggings());
        putIfPresent(buttons, 39, target.getInventory().getBoots());
        putIfPresent(buttons, 40, target.getInventory().getItemInOffHand());

        buttons.put(42, Button.createPlaceholder(healthItem()));
        buttons.put(43, Button.createPlaceholder(effectsItem()));
        buttons.put(44, clearButton());
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());

        return buttons;
    }

    private void putIfPresent(Map<Integer, Button> buttons, int slot, ItemStack item) {
        if (item != null && !item.getType().isAir()) buttons.put(slot, Button.createPlaceholder(item));
    }

    private ItemStack healthItem() {
        return new ItemBuilder(Material.REDSTONE)
                .setDisplayName(Component.text("Health", Theme.ERROR))
                .setLore(Gui.lore().value("Current", String.format("%.1f", target.getHealth())).build())
                .setAmount((int) Math.max(1, Math.min(64, Math.ceil(target.getHealth()))))
                .build();
    }

    private ItemStack effectsItem() {
        Gui.Lore lore = Gui.lore();
        List<PotionEffect> effects = List.copyOf(target.getActivePotionEffects());
        if (effects.isEmpty()) {
            lore.text("No active effects.");
        } else {
            for (PotionEffect effect : effects) {
                String name = effect.getType().getName().replace("_", " ");
                lore.entry(name + " " + (effect.getAmplifier() + 1) + " (" + Time.formatHHMMSS(effect.getDuration() / 20, TimeUnit.SECONDS) + ")");
            }
        }
        return new ItemBuilder(Material.BREWING_STAND)
                .setDisplayName(Component.text("Active Effects", Theme.PRIMARY))
                .setLore(lore.build())
                .build();
    }

    private Button clearButton() {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(Material.WRITABLE_BOOK)
                        .setDisplayName(Component.text("Clear Inventory", Theme.ERROR))
                        .setLore(Gui.lore().cta("clear this player's inventory").build())
                        .build();
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                Bukkit.dispatchCommand(player, "clear " + target.getName());
            }
        };
    }
}
