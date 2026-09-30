package games.sparking.altara.report;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.report.menu.close.ReportCloseMenu;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

/** {@code /reportclose}: accept or reject the report you are handling; {@code /reportrelease}: hand it back. */
public class ReportCloseCommand {

    @Command(names = {"reportclose", "rc"}, permission = "altara.staff", playerOnly = true, async = true,
            description = "Close the report you are currently handling")
    public void reportClose(Player sender) {
        var handling = Altara.getSharedInstance().getReportService().getHandling(sender.getUniqueId());

        Tasks.run(() -> {
            if (handling.isEmpty()) {
                sender.sendMessage(CC.error("You aren't handling a report.", "Run /reporthandle to pick one up."));
                return;
            }
            new ReportCloseMenu(handling.get()).openMenu(sender);
        });
    }

    @Command(names = {"reportrelease", "rr"}, permission = "altara.staff", playerOnly = true, async = true,
            description = "Put the report you are handling back in the queue")
    public void reportRelease(Player sender) {
        var handling = Altara.getSharedInstance().getReportService().getHandling(sender.getUniqueId());
        if (handling.isEmpty()) {
            Tasks.run(() -> sender.sendMessage(CC.error("You aren't handling a report.", "Run /reporthandle to pick one up.")));
            return;
        }

        Altara.getSharedInstance().getReportService().release(handling.get().getId(), sender.getUniqueId());
        Tasks.run(() -> sender.sendMessage(CC.success("Report released.", "It's back in the queue for someone else.")));
    }
}
