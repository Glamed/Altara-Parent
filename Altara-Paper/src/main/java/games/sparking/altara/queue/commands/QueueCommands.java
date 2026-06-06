package games.sparking.altara.queue.commands;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.CommandCooldown;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.queue.packet.QueueJoinPacket;
import games.sparking.altara.queue.packet.QueueLeavePacket;
import games.sparking.altara.queue.packet.QueueSendPlayerPacket;
import games.sparking.altara.queue.packet.update.QueuePausePacket;
import games.sparking.altara.queue.packet.update.QueueRatePacket;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.uuid.UUIDCache;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@RequiredArgsConstructor
public class QueueCommands {

    @Command(names = {"joinqueue", "play", "realm", "jq"},
             description = "Join a Queue",
             playerOnly = true,
             async = true)
    @CommandCooldown(time = 5)
    public boolean joinQueue(Player sender, @Param(name = "server", completionFlags = {"accessible"}) ServerInfo server) {
        if (!server.isOnline()) {
            sender.sendMessage(CC.errorMsg(Messages.PLAYER_OFFLINE, server.getName()));
            return false;
        }

        if (!server.isQueueEnabled() || server.isProxy()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_UNAVAILABLE, server.getName()));
            return false;
        }

        if (sender.hasPermission("invictus.queue.bypass")) {
            new QueueSendPlayerPacket(server.getName(), sender.getUniqueId()).publish();
            return true;
        }

        if (AltaraPaper.getPaperInstance().getQueueService().isQueueingFor(sender.getUniqueId(), server.getName())) {
            sender.sendMessage(CC.errorMsg("Invalid Server.", "You are already queued for *" + server.getName() +  "*."));
            return false;
        }

        if (!sender.hasPermission("altara.server." + server.getGroup())) {
            sender.sendMessage(CC.errorMsg("Invalid Server.", "You do not have access to *" + server.getName() +  "*."));
            return false;
        }

        new QueueJoinPacket(server.getName(), sender.getUniqueId()).publish();
        sender.sendMessage(CC.successMsg("","You have joined the queue for *" + server.getName() + "*."));
        return true;
    }

    @Command(names = {"leavequeue", "lq"},
             description = "Leave a Queue",
             playerOnly = true,
             async = true)
    public boolean leaveQueue(Player sender, @Param(name = "server") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_UNAVAILABLE, server.getName()));
            return false;
        }

        if (!AltaraPaper.getPaperInstance().getQueueService().isQueueingFor(sender.getUniqueId(), server.getName())) {
            sender.sendMessage(CC.errorMsg("Invalid Server.", "You are not queueing for *" + server.getName() + "*."));
            return false;
        }

        new QueueLeavePacket(server.getName(), sender.getUniqueId()).publish();
        sender.sendMessage(CC.successMsg("", "You have been removed from the *" + server.getName() + "* queue."));
        return true;
    }

    @Command(names = {"queue pause"},
             permission = "queue.command.argument.pause",
             description = "Pause a Queue",
             async = true)
    public boolean queuePause(CommandSender sender, @Param(name = "server") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_UNAVAILABLE, server.getName()));
            return false;
        }

        if (!server.isOnline()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_OFFLINE, server.getName()));
            return false;
        }

        boolean paused = !server.isQueuePaused();
        new QueuePausePacket(server.getName(), paused).publish();
        sender.sendMessage(CC.noticeMsg("Queue Updated.", "Queue of " + server.getName() + " has been " + CC.colorBoolean(!paused, "unpaused", "paused")));
        return true;
    }

    @Command(names = {"queue rate"},
             permission = "queue.command.argument.rate",
             description = "Change the rate at which players are being sent to the server (x per second)",
             async = true)
    public boolean queueRate(CommandSender sender, @Param(name = "server") ServerInfo server,
                             @Param(name = "rate") int rate) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_UNAVAILABLE, server.getName()));
            return false;
        }

        if (!server.isOnline()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_OFFLINE, server.getName()));
            return false;
        }

        new QueueRatePacket(server.getName(), rate).publish();
        sender.sendMessage(CC.noticeMsg("Queue Updated.", server.getName() + " queue rate has been set to *" + rate + "*."));
        return true;
    }

    @Command(names = {"queue info"},
             permission = "queue.command.argument.info",
             description = "View information about a queue",
             async = true)
    public boolean queueInfo(CommandSender sender, @Param(name = "server") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.errorMsg(Messages.SERVER_UNAVAILABLE, server.getName()));
            return false;
        }

        sender.sendMessage(CC.SMALL_CHAT_BAR);
        sender.sendMessage(CC.format("<red><b>Queue Info"));
        sender.sendMessage(CC.format("<yellow> State: %s",
                CC.strip(CC.colorBoolean(!server.isQueuePaused(), "Unpaused", "Paused"))));
        sender.sendMessage(CC.format("<yellow> Rate: <red>%d per second", server.getQueueRate()));
        sender.sendMessage(CC.format("<yellow> Queued: <red>%d",
                AltaraPaper.getPaperInstance().getQueueService().getQueueing(server.getName()).size()));
        sender.sendMessage(CC.SMALL_CHAT_BAR);
        return true;
    }

    @Command(names = {"queue debugme"},
             permission = "queue.command.argument.debugme",
             description = "View debug info about yourself",
             async = true,
             playerOnly = true)
    public boolean queueDebugMe(Player sender) {
        List<String> queues = AltaraPaper.getPaperInstance().getQueueService().getQueues(sender.getUniqueId());
        sender.sendMessage(CC.format("<blue>Currently queueing for <yellow>%d <blue>servers.", queues.size()));
        queues.forEach(queue ->
                sender.sendMessage(CC.format(
                        "<blue>%s: <yellow>%d<blue>/<yellow>%d",
                        queue,
                        AltaraPaper.getPaperInstance().getQueueService().getPosition(sender.getUniqueId(), queue) + 1,
                        AltaraPaper.getPaperInstance().getQueueService().getQueueing(queue).size()
                )));
        sender.sendMessage(CC.format("<blue>Primary: <yellow>%s",
                AltaraPaper.getPaperInstance().getQueueService().getPrimaryQueue(sender.getUniqueId())));
        return true;
    }

    @Command(names = {"queue debug"},
             permission = "queue.command.argument.debugme",
             description = "View debug info about a server",
             async = true)
    public boolean queueDebug(CommandSender sender, @Param(name = "server") ServerInfo server) {
        AltaraPaper.getPaperInstance().getQueueService().getQueueing(server.getName()).forEach(uuid ->
                sender.sendMessage(CC.format(
                        "<blue>%s: <yellow>%d",
                        UUIDCache.getName(uuid),
                        AltaraPaper.getPaperInstance().getQueueService().getPosition(uuid, server.getName()) + 1)));
        return true;
    }

}
