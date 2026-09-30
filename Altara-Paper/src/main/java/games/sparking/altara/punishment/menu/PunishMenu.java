package games.sparking.altara.punishment.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishTarget;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code /punish <player>}: choose the violation. */
public class PunishMenu extends Menu {

    private final PunishTarget target;
    private final String message;

    public PunishMenu(PunishTarget target) {
        this(target, null);
    }

    /** @param message the chat message being punished, if any (shown to the player afterwards) */
    public PunishMenu(PunishTarget target, String message) {
        this.target = target;
        this.message = message;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Punish", target.name());
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public boolean isAutoUpdate() {
        return false;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        buttons.put(4, new HeadButton());
        buttons.put(49, new HistoryButton());
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());

        // Rows of up to 7, each row centred inside the frame.
        InfractionType[] visible = InfractionType.visibleValues();
        int row = 2;
        for (int i = 0; i < visible.length; row++) {
            int count = Math.min(7, visible.length - i);
            int start = row * 9 + (9 - count) / 2;
            for (int j = 0; j < count; j++, i++) {
                buttons.put(start + j, new TypeButton(visible[i]));
            }
        }
        return buttons;
    }

    /** Wraps text into lines of roughly {@code width} characters. */
    static Gui.Lore wrap(Gui.Lore lore, String text, int width) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + word.length() + 1 > width) {
                lore.text(line.toString());
                line = new StringBuilder();
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lore.text(line.toString());
        return lore;
    }

    private class HeadButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(Bukkit.getOfflinePlayer(target.uuid()))
                    .setDisplayName(Component.text(target.name(), Theme.TEXT_STRONG))
                    .setLore(Gui.lore()
                            .value("Status", Bukkit.getPlayer(target.uuid()) != null
                                    ? Component.text("Online", Theme.SUCCESS) : Component.text("Offline", Theme.ERROR))
                            .text("", "Choose the violation below.")
                            .build())
                    .build();
        }
    }

    private class TypeButton extends Button {

        private final InfractionType type;

        TypeButton(InfractionType type) {
            this.type = type;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(type.getMaterial())
                    .addFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
                    .setDisplayName(Gui.name(type.getDisplayName()))
                    .setLore(wrap(Gui.lore(), type.getDescription(), 32).cta("choose this violation").build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            new PunishActionMenu(target, type, message, PunishMenu.this).openMenu(player);
        }
    }

    private class HistoryButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.WRITABLE_BOOK)
                    .setDisplayName(Gui.name("Punishment", "History"))
                    .setLore(Gui.lore()
                            .text("View past punishments and", "revoke active ones.")
                            .cta("view history")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            Tasks.runAsync(() -> {
                List<Punishment> history = Altara.getSharedInstance().getPunishmentService().loadPunishments(target.uuid());
                Tasks.run(() -> new PunishmentModifyMenu(target, history, PunishMenu.this).openMenu(player));
            });
        }
    }
}
