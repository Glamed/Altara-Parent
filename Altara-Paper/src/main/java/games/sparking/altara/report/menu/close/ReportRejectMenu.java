package games.sparking.altara.report.menu.close;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.report.RejectionType;
import games.sparking.altara.report.Report;
import games.sparking.altara.report.ReportStatus;
import games.sparking.altara.utils.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Picks a {@link RejectionType} to close out a report with no action taken. */
public class ReportRejectMenu extends Menu {

    private final Report report;

    public ReportRejectMenu(Report report) {
        this.report = report;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Report", "Reject");
    }

    @Override
    public int getSize() {
        return 27;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int index = 11;
        for (RejectionType type : RejectionType.values()) {
            buttons.put(index++, new TypeButton(type));
        }
        return buttons;
    }

    private class TypeButton extends Button {

        private final RejectionType type;

        TypeButton(RejectionType type) {
            this.type = type;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(type.getMaterial())
                    .setDisplayName(Gui.name(type.getDisplayName()))
                    .setLore(Gui.lore().text(type.getDescription()).cta("choose this reason").build())
                    .build();
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            ReportStatus status = type.isAbusive() ? ReportStatus.ABUSIVE : ReportStatus.REJECTED;
            new ConfirmationMenu(
                    new String[]{"Report", "Reject"},
                    "Reject as " + type.getDisplayName() + "?",
                    List.of(type.getDescription()),
                    "Reject", "Cancel",
                    confirmed -> {
                        if (!confirmed) return;
                        whoClicked.closeInventory();
                        ReportCloseMenu.resolveWith(whoClicked, report, status, type.name());
                    }
            ).openMenu(whoClicked);
        }
    }
}
