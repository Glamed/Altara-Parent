package games.sparking.altara.report;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.menu.menu.ConfirmationMenu;
import games.sparking.altara.presence.PresenceService;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.report.menu.ReportMenu;
import games.sparking.altara.report.menu.ReportTargetMenu;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/** {@code /report}, {@code /chatreport}, {@code /hackerreport}: file a report on a player. */
public class ReportSubmitCommand {

    @Command(names = {"report"}, playerOnly = true, async = true, description = "Report a player")
    public void report(Player sender, @Param(name = "player", defaultValue = "") String targetName) {
        handle(null, sender, targetName);
    }

    @Command(names = {"chatreport", "cr"}, playerOnly = true, async = true, description = "Report a player for chat abuse")
    public void chatReport(Player sender, @Param(name = "player", defaultValue = "") String targetName) {
        handle(ReportCategory.CHAT_ABUSE, sender, targetName);
    }

    @Command(names = {"hackerreport", "hr", "cheatreport"}, playerOnly = true, async = true, description = "Report a player for cheating")
    public void hackerReport(Player sender, @Param(name = "player", defaultValue = "") String targetName) {
        handle(ReportCategory.CHEATING, sender, targetName);
    }

    /** Runs off the main thread (marked {@code async = true}) — {@link PresenceService} does a blocking Redis lookup. */
    private void handle(ReportCategory category, Player sender, String targetName) {
        if (targetName.isEmpty()) {
            Tasks.run(() -> {
                if (category == null) {
                    new ReportMenu(null, null).openMenu(sender);
                } else {
                    new ReportTargetMenu(category).openMenu(sender);
                }
            });
            return;
        }

        UUID targetUuid = UUIDUtils.isUUID(targetName) ? UUID.fromString(targetName) : UUIDCache.getUuid(targetName);
        if (targetUuid == null || !PresenceService.isOnline(targetUuid)) {
            Tasks.run(() -> sender.sendMessage(CC.error("Player not found.", "*" + targetName + "* isn't online right now.")));
            return;
        }

        if (targetUuid.equals(sender.getUniqueId())) {
            Tasks.run(() -> sender.sendMessage(CC.error("Unable to report.", "You cannot report yourself.")));
            return;
        }

        String resolvedName = UUIDCache.getName(targetUuid);

        if (category == null) {
            Tasks.run(() -> new ReportMenu(targetUuid, resolvedName).openMenu(sender));
            return;
        }

        if (category == ReportCategory.CHAT_ABUSE && !hasReceivedMessageFrom(sender, targetUuid)) {
            Tasks.run(() -> sender.sendMessage(CC.error("Invalid report.", "You haven't received any messages from *" + resolvedName + "*.")));
            return;
        }

        Tasks.run(() -> confirmAndSubmit(sender, targetUuid, resolvedName, category));
    }

    private static boolean hasReceivedMessageFrom(Player reporter, UUID targetUuid) {
        return ReportChatCache.recentlySeenBy(reporter.getUniqueId().toString()).stream()
                .anyMatch(message -> targetUuid.toString().equals(message.getSenderUuid()));
    }

    public static void confirmAndSubmit(Player sender, UUID targetUuid, String targetName, ReportCategory category) {
        new ConfirmationMenu(
                new String[]{"Report", targetName},
                "Report " + targetName + " for " + category.getDisplayName() + "?",
                List.of(category.getDescription()),
                "Report",
                "Cancel",
                confirmed -> {
                    if (!confirmed) return;
                    submit(sender, targetUuid, targetName, category);
                }
        ).openMenu(sender);
    }

    public static void submit(Player sender, UUID targetUuid, String targetName, ReportCategory category) {
        List<ReportMessage> chatMessages = category.getGroup() == ReportGroup.CHAT
                ? ReportChatCache.recentlySeenBy(sender.getUniqueId().toString())
                : List.of();

        Tasks.runAsync(() -> {
            var saved = Altara.getSharedInstance().getReportService().create(
                    sender.getUniqueId(),
                    Altara.getSharedInstance().getLocalServerName(),
                    targetUuid,
                    category,
                    chatMessages
            );

            Tasks.run(() -> {
                if (saved.isPresent()) {
                    sender.sendMessage(CC.success("Report submitted.", "*" + targetName + "* has been reported for " + category.getDisplayName() + "."));
                } else {
                    sender.sendMessage(CC.error("Report failed.", "Please try again in a moment."));
                }
            });
        });
    }
}
