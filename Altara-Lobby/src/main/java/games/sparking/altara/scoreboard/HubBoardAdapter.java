package games.sparking.altara.scoreboard;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.AltaraPaper;
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
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class HubBoardAdapter implements ScoreboardAdapter {

    private static final AtomicReference<Component> SCOREBOARD_TITLE = new AtomicReference<>(Component.empty());
    private static int rotateTick = 0; // controls rotation timing

    static {
        StringAnimation animation = new StringAnimation();

        animation.add(new StaticAnimation(
                "<dark_red><bold>" + AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreboardTitle(),
                10
        ));
        animation.add(new FadeAnimation(
                AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreboardTitle(),
                "<dark_red><bold>",
                "<red><bold>",
                false
        ));
        animation.add(new BlinkAnimation(
                AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreboardTitle(),
                "<dark_red><bold>",
                "<red><bold>",
                3,
                2
        ));
        animation.add(new StaticAnimation(
                "<dark_red><bold>" + AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreboardTitle(),
                10
        ));
        animation.add(new FadeAnimation(
                AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreboardTitle(),
                "<dark_red><bold>",
                "<red><bold>",
                true
        ));
        animation.add(new BlinkAnimation(
                AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreboardTitle(),
                "<dark_red><bold>",
                "<red><bold>",
                3,
                2
        ));

        animation.whenTicked(s -> SCOREBOARD_TITLE.set(CC.translate(s)));
        animation.start(4L);

        // Rotation updater (every 3 seconds swap)
        AltaraPaper.getPlugin().getServer().getScheduler().runTaskTimer(
                AltaraPaper.getPlugin(),
                () -> rotateTick++,
                0L,
                200L // 60 ticks = 3 seconds
        );
    }

    @Override
    public Component getTitle(Player player) {
        return SCOREBOARD_TITLE.get();
    }

    @Override
    public List<Component> getLines(Player player) {
//        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        Profile profile = new Profile(player.getUniqueId(), "GLamify");

        QueueService queueService = AltaraPaper.getPaperInstance().getQueueService();
        String primaryQueue = queueService.getPrimaryQueue(player.getUniqueId());

        TagResolver globalPlaceholders = TagResolver.builder()
                .resolver(Placeholder.parsed("rank",
                        profile.getCurrentGrant().asRank().getDisplayName() + (profile.isDisguised()
                                ? " &7(" + profile.getRealCurrentGrant().asRank().getDisplayName() + "&7)" : "")))
                .resolver(Placeholder.unparsed("onlinecount",
                        String.valueOf(ServerInfo.getGlobalPlayerCount())))
                .resolver(Placeholder.unparsed("maxcount",
                        String.valueOf(ServerInfo.getGlobalPlayerCount() + 1)))
                .resolver(Placeholder.unparsed("connection_address",
                        AltaraLobby.getLobbyInstance().getLobbyConfig().getServerConfig().getIp()))
                .resolver(Placeholder.unparsed("store_address",
                        AltaraLobby.getLobbyInstance().getLobbyConfig().getServerConfig().getStore()))
                .resolver(Placeholder.unparsed("web_address",
                        AltaraLobby.getLobbyInstance().getLobbyConfig().getServerConfig().getWebsite()))
                .build();

        List<Component> lines = new ArrayList<>();

        for (String s : AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardLines()) {
            switch (s) {
                case "%rotate%": {
                    boolean hasQueue = primaryQueue != null && !AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardQueueLines().isEmpty();
                    boolean hasReboot = RebootService.getRebootTask() != null && !AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardRebootLines().isEmpty();

                    if (hasQueue && hasReboot) {
                        if (rotateTick % 2 == 0) {
                            lines.addAll(getQueueLines(player, queueService, primaryQueue));
                        } else {
                            lines.addAll(getRebootLines());
                        }
                    } else if (hasQueue) {
                        lines.addAll(getQueueLines(player, queueService, primaryQueue));
                    } else if (hasReboot) {
                        lines.addAll(getRebootLines());
                    }
                    break;
                }
                case "%queue%":
                    lines.addAll(getQueueLines(player, queueService, primaryQueue));
                    break;
                case "%reboot%":
                    lines.addAll(getRebootLines());
                    break;
                default:
                    lines.add(CC.format(s, globalPlaceholders));
                    break;
            }
        }

        return lines;
    }

    private List<Component> getQueueLines(Player player, QueueService queueService, String primaryQueue) {
        List<Component> queueLines = new ArrayList<>();
        if (primaryQueue == null) return queueLines;

        TagResolver queuePlaceholders = TagResolver.builder()
                .resolver(Placeholder.unparsed("queue_position",
                        String.valueOf(queueService.getPosition(player.getUniqueId(), primaryQueue) + 1)))
                .resolver(Placeholder.unparsed("queue_total",
                        String.valueOf(queueService.getQueueing(primaryQueue).size())))
                .resolver(Placeholder.unparsed("queue_name", primaryQueue))
                .build();

        for (String queueLine : AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardQueueLines()) {
            queueLines.add(CC.format(queueLine, queuePlaceholders));
        }
        return queueLines;
    }

    private List<Component> getRebootLines() {
        List<Component> rebootLines = new ArrayList<>();
        RebootTask rebootTask = RebootService.getRebootTask();
        if (rebootTask == null) return rebootLines;

        TagResolver rebootPlaceholders = TagResolver.builder()
                .resolver(Placeholder.unparsed("time_remaining",
                        Time.formatHHMMSS(rebootTask.getSecondsRemaining(), TimeUnit.SECONDS)))
                .build();

        for (String rebootLine : AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardRebootLines()) {
            rebootLines.add(CC.format(rebootLine, rebootPlaceholders));
        }
        return rebootLines;
    }
}