package games.sparking.altara.scoreboard;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraLobby;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.configuration.StaffConfig;
import games.sparking.altara.configuration.defaults.ServerConfig;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.reboot.RebootService;
import games.sparking.altara.reboot.RebootTask;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.stringanimation.StringAnimation;
import games.sparking.altara.stringanimation.impl.BlinkAnimation;
import games.sparking.altara.stringanimation.impl.FadeAnimation;
import games.sparking.altara.stringanimation.impl.StaticAnimation;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scoreboard for Altara's Staff-1 server.
 *
 * <p>Deliberately identical to the Lobby scoreboard ({@code HubBoardAdapter}) rather than
 * a team-by-team operations dashboard: nothing here should show workload numbers, ticket
 * counts or other internal figures on a screen anyone on the server can glance at (or
 * screenshot). Staff-specific tooling belongs in dedicated commands/menus (see
 * {@code /review}), not the sidebar.
 */
public class StaffBoardAdapter implements ScoreboardAdapter {

    private static final String TITLE_BASE = "<dark_aqua><bold>";
    private static final String TITLE_HIGHLIGHT = "<aqua><bold>";
    /** Ticks between swapping the queue and reboot blocks when both apply. */
    private static final long ROTATE_INTERVAL = 100L;

    private final AtomicReference<Component> title = new AtomicReference<>(Component.empty());
    private int rotateTick = 0;

    public StaffBoardAdapter() {
        String text = config().getScoreboardTitle();

        StringAnimation animation = new StringAnimation();
        animation.add(new StaticAnimation(TITLE_BASE + text, 10));
        animation.add(new FadeAnimation(text, TITLE_BASE, TITLE_HIGHLIGHT, false));
        animation.add(new BlinkAnimation(text, TITLE_BASE, TITLE_HIGHLIGHT, 3, 2));
        animation.add(new StaticAnimation(TITLE_BASE + text, 10));
        animation.add(new FadeAnimation(text, TITLE_BASE, TITLE_HIGHLIGHT, true));
        animation.add(new BlinkAnimation(text, TITLE_BASE, TITLE_HIGHLIGHT, 3, 2));
        animation.whenTicked(s -> title.set(CC.format(s)));
        animation.start(4L);

        AltaraPaper.getPlugin().getServer().getScheduler().runTaskTimer(
                AltaraPaper.getPlugin(), () -> rotateTick++, ROTATE_INTERVAL, ROTATE_INTERVAL);
    }

    private static StaffConfig config() {
        return AltaraLobby.getLobbyInstance().getStaffConfig();
    }

    @Override
    public Component getTitle(Player player) {
        return title.get();
    }

    @Override
    public List<Component> getLines(Player player) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        if (profile == null) return List.of();

        StaffConfig config = config();
        ServerConfig server = config.getServerConfig();
        QueueService queueService = AltaraPaper.getPaperInstance().getQueueService();
        String primaryQueue = queueService.getPrimaryQueue(player.getUniqueId());
        int online = ServerInfo.getGlobalPlayerCount();

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.parsed("rank", rankDisplay(profile)),
                Placeholder.unparsed("onlinecount", String.valueOf(online)),
                Placeholder.unparsed("maxcount", String.valueOf(online + 1)),
                Placeholder.unparsed("connection_address", nullToEmpty(server.getIp())),
                Placeholder.unparsed("store_address", nullToEmpty(server.getStore())),
                Placeholder.unparsed("web_address", nullToEmpty(server.getWebsite())));

        List<Component> lines = new ArrayList<>();
        for (String line : config.getScoreBoardLines()) {
            switch (line) {
                case "<rotate>" -> {
                    boolean hasQueue = primaryQueue != null && !config.getScoreBoardQueueLines().isEmpty();
                    boolean hasReboot = RebootService.getRebootTask() != null && !config.getScoreBoardRebootLines().isEmpty();
                    if (hasQueue && (!hasReboot || rotateTick % 2 == 0)) {
                        lines.addAll(queueLines(player, queueService, primaryQueue));
                    } else if (hasReboot) {
                        lines.addAll(rebootLines());
                    }
                }
                case "<queue>" -> lines.addAll(queueLines(player, queueService, primaryQueue));
                case "<reboot>" -> lines.addAll(rebootLines());
                default -> lines.add(CC.format(line, placeholders));
            }
        }
        return lines;
    }

    /** Rank display names are staff-authored MiniMessage, so they're parsed; disguised staff also see their real rank. */
    private static String rankDisplay(Profile profile) {
        String shown = rankName(profile.getCurrentGrant());
        if (!profile.isDisguised()) return shown;
        return shown + " <gray>(" + rankName(profile.getRealCurrentGrant()) + "<gray>)";
    }

    private static String rankName(Grant grant) {
        return grant != null && grant.asRank() != null ? grant.asRank().getDisplayName() : "<gray>None";
    }

    private static List<Component> queueLines(Player player, QueueService queueService, String queue) {
        if (queue == null) return List.of();
        int position = queueService.getPosition(player.getUniqueId(), queue);
        if (position < 0) return List.of();

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("queue_name", queue),
                Placeholder.unparsed("queue_position", String.valueOf(position + 1)),
                Placeholder.unparsed("queue_total", String.valueOf(queueService.getQueueing(queue).size())));
        return format(config().getScoreBoardQueueLines(), placeholders);
    }

    private static List<Component> rebootLines() {
        RebootTask task = RebootService.getRebootTask();
        if (task == null) return List.of();

        TagResolver placeholders = TagResolver.resolver(Placeholder.unparsed("time_remaining",
                Time.formatHHMMSS(task.getSecondsRemaining(), TimeUnit.SECONDS)));
        return format(config().getScoreBoardRebootLines(), placeholders);
    }

    /** Older configs used {@code %name%} placeholders; accept both spellings. */
    private static List<Component> format(List<String> templates, TagResolver placeholders) {
        List<Component> lines = new ArrayList<>(templates.size());
        for (String template : templates) {
            lines.add(CC.format(template.replaceAll("%(queue_name|queue_position|queue_total|time_remaining)%", "<$1>"),
                    placeholders));
        }
        return lines;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
