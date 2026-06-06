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
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private static int rotateTick = 0;

    static {
        StringAnimation animation = new StringAnimation();

        String title = AltaraLobby.getLobbyInstance()
                .getLobbyConfig()
                .getScoreboardTitle();

        animation.add(new StaticAnimation("<dark_red><bold>" + title, 10));
        animation.add(new FadeAnimation(title, "<dark_red><bold>", "<red><bold>", false));
        animation.add(new BlinkAnimation(title, "<dark_red><bold>", "<red><bold>", 3, 2));
        animation.add(new StaticAnimation("<dark_red><bold>" + title, 10));
        animation.add(new FadeAnimation(title, "<dark_red><bold>", "<red><bold>", true));
        animation.add(new BlinkAnimation(title, "<dark_red><bold>", "<red><bold>", 3, 2));

        animation.whenTicked(s -> SCOREBOARD_TITLE.set(CC.translate(s)));
        animation.start(4L);

        AltaraPaper.getPlugin().getServer().getScheduler().runTaskTimer(
                AltaraPaper.getPlugin(),
                () -> rotateTick++,
                0L,
                60L
        );
    }

    @Override
    public Component getTitle(Player player) {
        return SCOREBOARD_TITLE.get();
    }

    @Override
    public List<Component> getLines(Player player) {

        Profile profile = new Profile(player.getUniqueId(), "GLamify");

        QueueService queueService = AltaraPaper.getPaperInstance().getQueueService();
        String primaryQueue = queueService.getPrimaryQueue(player.getUniqueId());

        TagResolver globalPlaceholders = TagResolver.builder()
                .resolver(Placeholder.parsed("rank",
                        profile.getCurrentGrant().asRank().getDisplayName()
                                + (profile.isDisguised()
                                ? " &7(" + profile.getRealCurrentGrant().asRank().getDisplayName() + "&7)"
                                : "")))
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

        TagResolver internalResolvers = TagResolver.resolver(
                Placeholder.component("queue", buildQueueComponent(player, queueService, primaryQueue)),
                Placeholder.component("reboot", buildRebootComponent()),
                Placeholder.component("rotate", buildRotateComponent(player, queueService, primaryQueue))
        );

        List<Component> lines = new ArrayList<>();
        MiniMessage mm = MiniMessage.miniMessage();

        for (String line : AltaraLobby.getLobbyInstance()
                .getLobbyConfig()
                .getScoreBoardLines()) {

            lines.add(mm.deserialize(line, globalPlaceholders, internalResolvers));
        }

        return lines;
    }

    private Component buildRotateComponent(Player player, QueueService queueService, String primaryQueue) {

        boolean hasQueue = primaryQueue != null
                && !AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardQueueLines().isEmpty();

        boolean hasReboot = RebootService.getRebootTask() != null
                && !AltaraLobby.getLobbyInstance().getLobbyConfig().getScoreBoardRebootLines().isEmpty();

        if (hasQueue && hasReboot) {
            if (rotateTick % 2 == 0) {
                return buildQueueComponent(player, queueService, primaryQueue);
            } else {
                return buildRebootComponent();
            }
        }

        if (hasQueue) return buildQueueComponent(player, queueService, primaryQueue);
        if (hasReboot) return buildRebootComponent();

        return Component.empty();
    }

    private Component buildQueueComponent(Player player, QueueService queueService, String primaryQueue) {

        if (primaryQueue == null) return Component.empty();

        TagResolver queuePlaceholders = TagResolver.builder()
                .resolver(Placeholder.unparsed("queue_position",
                        String.valueOf(queueService.getPosition(player.getUniqueId(), primaryQueue) + 1)))
                .resolver(Placeholder.unparsed("queue_total",
                        String.valueOf(queueService.getQueueing(primaryQueue).size())))
                .resolver(Placeholder.unparsed("queue_name", primaryQueue))
                .build();

        List<Component> lines = new ArrayList<>();
        MiniMessage mm = MiniMessage.miniMessage();

        for (String line : AltaraLobby.getLobbyInstance()
                .getLobbyConfig()
                .getScoreBoardQueueLines()) {

            lines.add(mm.deserialize(line, queuePlaceholders));
        }

        return Component.join(net.kyori.adventure.text.JoinConfiguration.noSeparators(), lines);
    }

    private Component buildRebootComponent() {

        RebootTask rebootTask = RebootService.getRebootTask();
        if (rebootTask == null) return Component.empty();

        TagResolver rebootPlaceholders = TagResolver.builder()
                .resolver(Placeholder.unparsed("time_remaining",
                        Time.formatHHMMSS(rebootTask.getSecondsRemaining(), TimeUnit.SECONDS)))
                .build();

        List<Component> lines = new ArrayList<>();
        MiniMessage mm = MiniMessage.miniMessage();

        for (String line : AltaraLobby.getLobbyInstance()
                .getLobbyConfig()
                .getScoreBoardRebootLines()) {

            lines.add(mm.deserialize(line, rebootPlaceholders));
        }

        return Component.join(net.kyori.adventure.text.JoinConfiguration.noSeparators(), lines);
    }
}