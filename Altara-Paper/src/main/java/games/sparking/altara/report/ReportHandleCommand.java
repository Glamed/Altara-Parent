package games.sparking.altara.report;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.uuid.UUIDCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code /reporthandle}: claims (or resumes) a report and follows the suspect. */
public class ReportHandleCommand {

    @Command(names = {"reporthandle", "rh"}, permission = "altara.staff", playerOnly = true, async = true,
            description = "Handle the next open report")
    public void reportHandle(Player sender) {
        UUID staffUuid = sender.getUniqueId();

        Optional<Report> existing = Altara.getSharedInstance().getReportService().getHandling(staffUuid);
        Report report = existing.orElseGet(() -> Altara.getSharedInstance().getReportService()
                .claimNext(staffUuid, Altara.getSharedInstance().getLocalServerName())
                .orElse(null));

        if (report == null) {
            Tasks.run(() -> sender.sendMessage(CC.success("No open reports.", "Nice work staying on top of the queue.")));
            return;
        }

        // Staff mode + report panel + follow the suspect — identical to a web-assigned report.
        ReportEngagement.engage(staffUuid, report, false);
    }

    public static void display(Player staff, Report report, String suspectName) {
        staff.sendMessage(CC.header(Panel.STAFF, "Report", suspectName));
        staff.sendMessage(CC.line(CC.item("Category", report.getGroup().name())));

        List<ReportReason> reasons = report.getReasons();
        staff.sendMessage(CC.line(Component.text(reasons.size() + " " + CC.plural(reasons.size(), "report") + ":", Theme.TEXT)));
        for (ReportReason reason : reasons) {
            String reporterName = reason.getReporterUuid() != null
                    ? UUIDCache.getName(UUID.fromString(reason.getReporterUuid())) : "Automated";
            staff.sendMessage(CC.nested((reporterName != null ? reporterName : "Unknown")
                    + ": " + safeCategoryName(reason.getCategory())));
        }

        TextComponent.Builder actions = Component.text()
                .append(CC.accept("Close this report (accept/reject)", ClickEvent.runCommand("/reportclose")));

        if (report.getGroup() == ReportGroup.CHAT && !report.getMessages().isEmpty()) {
            actions.append(Component.space())
                    .append(CC.action("View chat history", ClickEvent.runCommand("/viewchatreport " + report.getId())));
        }

        staff.sendMessage(CC.line(actions.build()));
        staff.sendMessage(CC.footer(Panel.STAFF));
    }

    private static String safeCategoryName(String category) {
        try {
            return ReportCategory.valueOf(category).getDisplayName();
        } catch (IllegalArgumentException e) {
            return category;
        }
    }
}
