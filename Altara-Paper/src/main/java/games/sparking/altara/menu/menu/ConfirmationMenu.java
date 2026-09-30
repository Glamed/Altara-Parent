package games.sparking.altara.menu.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Three-row accept/decline menu.
 *
 * <pre>
 * slot 4  — summary of what is being confirmed
 * slot 12 — ✔ accept          slot 14 — ✕ decline
 * </pre>
 *
 * The callback receives {@code true} on accept and {@code false} on decline.
 * Closing the menu any other way does nothing.
 */
public class ConfirmationMenu extends Menu {

    private final String[] breadcrumb;
    private final String question;
    private final List<String> details;
    private final String acceptLabel;
    private final String declineLabel;
    private final Consumer<Boolean> callback;

    /**
     * @param breadcrumb pages after "Altara", e.g. {@code {"Punish", "Confirm"}}
     * @param question   short summary, e.g. "Revoke this punishment?"
     * @param details    gray lore lines explaining the consequence (may be empty)
     */
    public ConfirmationMenu(String[] breadcrumb, String question, List<String> details,
                            String acceptLabel, String declineLabel, Consumer<Boolean> callback) {
        this.breadcrumb = breadcrumb;
        this.question = question;
        this.details = details;
        this.acceptLabel = acceptLabel;
        this.declineLabel = declineLabel;
        this.callback = callback;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title(breadcrumb);
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        Gui.Lore info = Gui.lore();
        details.forEach(info::text);
        buttons.put(4, Button.createPlaceholder(new ItemBuilder(Material.PAPER)
                .setDisplayName(Component.text(question, Theme.PRIMARY))
                .setLore(info.build())
                .build()));

        buttons.put(12, choice(true, Gui.acceptItem(acceptLabel, Gui.lore().cta(acceptLabel.toLowerCase()).build())));
        buttons.put(14, choice(false, Gui.declineItem(declineLabel, Gui.lore().cta(declineLabel.toLowerCase()).build())));
        return buttons;
    }

    private Button choice(boolean accept, ItemStack item) {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return item;
            }

            @Override
            public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
                player.closeInventory();
                callback.accept(accept);
            }
        };
    }
}
