package games.sparking.altara.review;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.task.Tasks;
import org.bukkit.entity.Player;

import java.util.List;

/** Staff-1 only: opens the automated-punishment review queue. */
public class ReviewCommand {

    @Command(names = {"review", "pendingreview"}, permission = "altara.review", playerOnly = true, async = true,
            description = "Review automated punishments awaiting staff review")
    public void review(Player sender) {
        List<Punishment> pending = Altara.getSharedInstance().getPunishmentService().getPendingReview();
        Tasks.run(() -> {
            if (sender.isOnline()) new ReviewQueueMenu(pending).openMenu(sender);
        });
    }
}
