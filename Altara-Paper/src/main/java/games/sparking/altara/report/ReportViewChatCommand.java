package games.sparking.altara.report;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.report.menu.ChatReportMenu;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

/** {@code /viewchatreport <id>}: shows the chat history attached to a report. */
public class ReportViewChatCommand {

    @Command(names = {"viewchatreport", "crv"}, permission = "altara.staff", playerOnly = true, async = true,
            description = "View the chat history of a report")
    public void viewChatReport(Player sender, @Param(name = "reportId") String reportId) {
        var report = Altara.getSharedInstance().getReportService().getById(reportId);

        Tasks.run(() -> {
            if (report.isEmpty()) {
                sender.sendMessage(CC.error("Report not found.", "*" + reportId + "* does not exist."));
                return;
            }
            if (report.get().getMessages().isEmpty()) {
                sender.sendMessage(CC.error("No chat history.", "This report has no attached chat messages."));
                return;
            }
            new ChatReportMenu(reportId, report.get().getMessages()).openMenu(sender);
        });
    }
}
