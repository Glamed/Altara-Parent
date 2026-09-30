package games.sparking.altara.report.menu;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.report.ReportCategory;
import games.sparking.altara.report.ReportSubmitCommand;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** {@code /report [player]}: choose the violation. If a target was already given, skips straight here. */
public class ReportMenu extends Menu {

    private final UUID targetUuid;
    private final String targetName;

    public ReportMenu(UUID targetUuid, String targetName) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    public Component getTitle(Player player) {
        return targetName != null ? Gui.title("Report", targetName) : Gui.title("Report");
    }

    @Override
    public int getSize() {
        return targetName != null ? 36 : 27;
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
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());

        int startRow = 1;
        if (targetName != null) {
            buttons.put(4, new HeadButton());
            startRow = 2;
        }

        ReportCategory[] categories = ReportCategory.values();
        int count = categories.length;
        int row = startRow;
        int index = 0;
        while (index < count) {
            int thisRowCount = Math.min(7, count - index);
            int start = row * 9 + (9 - thisRowCount) / 2;
            for (int j = 0; j < thisRowCount; j++, index++) {
                buttons.put(start + j, new CategoryButton(categories[index]));
            }
            row++;
        }
        return buttons;
    }

    private class HeadButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(Bukkit.getOfflinePlayer(targetUuid))
                    .setDisplayName(Component.text(targetName, Theme.TEXT_STRONG))
                    .setLore(Gui.lore().text("Choose the violation below.").build())
                    .build();
        }
    }

    private class CategoryButton extends Button {

        private final ReportCategory category;

        CategoryButton(ReportCategory category) {
            this.category = category;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(category.getMaterial())
                    .setDisplayName(Gui.name(category.getDisplayName()))
                    .setLore(Gui.lore().text(category.getDescription()).cta("report this").build())
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            if (targetUuid == null) {
                new ReportTargetMenu(category).openMenu(whoClicked);
                return;
            }
            ReportSubmitCommand.confirmAndSubmit(whoClicked, targetUuid, targetName, category);
        }
    }
}
