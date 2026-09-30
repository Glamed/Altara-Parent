package games.sparking.altara.report.menu.close;

import games.sparking.altara.Altara;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.report.Report;
import games.sparking.altara.report.ReportCategory;
import games.sparking.altara.report.ReportStatus;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code /reportclose}: accept (probable cause found) or reject the report currently being handled. */
public class ReportCloseMenu extends Menu {

    private final Report report;

    public ReportCloseMenu(Report report) {
        this.report = report;
    }

    @Override
    public Component getTitle(Player player) {
        return Gui.title("Report", "Close");
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
        buttons.put(12, new AcceptButton());
        buttons.put(14, new RejectButton());
        return buttons;
    }

    private String dominantCategory() {
        return report.getReasons().stream()
                .max((a, b) -> {
                    ReportCategory ca = safeCategory(a.getCategory());
                    ReportCategory cb = safeCategory(b.getCategory());
                    int pa = ca != null ? ca.getPriority() : 0;
                    int pb = cb != null ? cb.getPriority() : 0;
                    return Integer.compare(pa, pb);
                })
                .map(reason -> reason.getCategory())
                .orElse(null);
    }

    private static ReportCategory safeCategory(String name) {
        try {
            return ReportCategory.valueOf(name);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    private class AcceptButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return Gui.acceptItem("Accept Report", Gui.lore()
                    .text("You have probable cause to believe", "a rule has been violated.")
                    .cta("accept").build());
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            new ConfirmationMenu(
                    new String[]{"Report", "Accept"},
                    "Accept this report?",
                    List.of("Remember to issue a punishment with /punish if you haven't already."),
                    "Accept", "Cancel",
                    confirmed -> {
                        if (!confirmed) return;
                        whoClicked.closeInventory();
                        resolveWith(whoClicked, report, ReportStatus.ACCEPTED, dominantCategory());
                    }
            ).openMenu(whoClicked);
        }
    }

    private class RejectButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return Gui.declineItem("Reject Report", Gui.lore()
                    .text("You do not have probable cause to", "believe a rule has been violated.")
                    .cta("choose a reason").build());
        }

        @Override
        public void click(Player whoClicked, int slot, ClickType clickType, int hotbarButton) {
            new ReportRejectMenu(report).openMenu(whoClicked);
        }
    }

    /** Also used by {@link ReportRejectMenu} to resolve with a specific rejection reason. */
    public static void resolveWith(Player staff, Report report, ReportStatus status, String statusReason) {
        Tasks.runAsync(() -> {
            boolean success = Altara.getSharedInstance().getReportService()
                    .resolve(report.getId(), staff.getUniqueId(), status, statusReason);
            Tasks.run(() -> {
                if (success) {
                    staff.sendMessage(CC.success("Report resolved.", "Marked as " + status.name() + "."));
                } else {
                    staff.sendMessage(CC.error("Failed to resolve report.", "Please try again."));
                }
            });
        });
    }
}
