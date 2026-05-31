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
            sender.sendMessage(CC.RED + "You cannot queue for " + CC.YELLOW + server.getName() + CC.RED
                    + " while the server is offline.");
            return false;
        }

        if (server.isProxy()) {
            sender.sendMessage(CC.RED + "Cannot queue for a proxy.");
            return false;
        }

        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.RED + "You cannot queue for " + CC.YELLOW + server.getName() + CC.RED + ".");
            return false;
        }

        if (sender.hasPermission("invictus.queue.bypass")) {
            new QueueSendPlayerPacket(server.getName(), sender.getUniqueId()).publish();
            return true;
        }

        if (AltaraPaper.getPaperInstance().getQueueService().isQueueingFor(sender.getUniqueId(), server.getName())) {
            sender.sendMessage(CC.RED + "You are already queueing for " + CC.YELLOW + server.getName() + CC.RED + ".");
            return false;
        }

        if (!sender.hasPermission("invictus.server." + server.getGroup())) {
            sender.sendMessage(CC.RED + "You do not have permission to queue for " + CC.YELLOW + server.getName() + CC.RED + ".");
            return false;
        }

        new QueueJoinPacket(server.getName(), sender.getUniqueId()).publish();
        sender.sendMessage(CC.GREEN + "You have been added to the " + CC.YELLOW + server.getName() + CC.GREEN + " " +
                "queue.");
        return true;
    }

    @Command(names = {"leavequeue", "lq"},
             description = "Leave a Queue",
             playerOnly = true,
             async = true)
    public boolean leaveQueue(Player sender, @Param(name = "server") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.YELLOW + server.getName() + CC.RED + " does not have queue enabled.");
            return false;
        }

        if (!AltaraPaper.getPaperInstance().getQueueService().isQueueingFor(sender.getUniqueId(), server.getName())) {
            sender.sendMessage(CC.RED + "You are not queueing for " + CC.YELLOW + server.getName() + CC.RED + ".");
            return false;
        }

        new QueueLeavePacket(server.getName(), sender.getUniqueId()).publish();
        sender.sendMessage(CC.GREEN + "You have been removed from the " + CC.YELLOW + server.getName() + CC.GREEN +
                " queue.");
        return true;
    }

    @Command(names = {"queue pause"},
             permission = "queue.command.argument.pause",
             description = "Pause a Queue",
             async = true)
    public boolean queuePause(CommandSender sender, @Param(name = "server") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.YELLOW + server.getName() + CC.RED + " does not have queue enabled.");
            return false;
        }

        if (!server.isOnline()) {
            sender.sendMessage(CC.YELLOW + server.getName() + CC.RED + " is offline.");
            return false;
        }

        boolean paused = !server.isQueuePaused();
        new QueuePausePacket(server.getName(), paused).publish();
        sender.sendMessage(CC.GREEN + "Queue of " + CC.YELLOW + server.getName() + CC.GREEN + " has been "
                + CC.colorBoolean(!paused, "unpaused", "paused"));
        return true;
    }

    @Command(names = {"queue rate"},
             permission = "queue.command.argument.rate",
             description = "Change the rate at which players are being sent to the server (x per second)",
             async = true)
    public boolean queueRate(CommandSender sender, @Param(name = "server") ServerInfo server,
                             @Param(name = "rate") int rate) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.YELLOW + server.getName() + CC.RED + " does not have queue enabled.");
            return false;
        }

        if (!server.isOnline()) {
            sender.sendMessage(CC.YELLOW + server.getName() + CC.RED + " is offline.");
            return false;
        }

        new QueueRatePacket(server.getName(), rate).publish();
        sender.sendMessage(CC.format("&aRate of &e%s &awas set to &e%d&a.", server.getName(), rate));
        return true;
    }

    @Command(names = {"queue info"},
             permission = "queue.command.argument.info",
             description = "View information about a queue",
             async = true)
    public boolean queueInfo(CommandSender sender, @Param(name = "server") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.YELLOW + server.getName() + CC.RED + " does not have queue enabled.");
            return false;
        }

        sender.sendMessage(CC.SMALL_CHAT_BAR);
        sender.sendMessage(CC.RED + CC.BOLD + "Queue Info");
        sender.sendMessage(CC.format(" &eState: %s",
                CC.colorBoolean(!server.isQueuePaused(), "Unpaused", "Paused")));
        sender.sendMessage(CC.format(" &eRate: &c%d per second", server.getQueueRate()));
        sender.sendMessage(CC.format(" &eQueued: &c%d",
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
        sender.sendMessage(CC.format("&9Currently queueing for &e%d &9servers.", queues.size()));
        queues.forEach(queue ->
                sender.sendMessage(CC.format(
                        "&9%s: &e%d&9/&e%d",
                        queue,
                        AltaraPaper.getPaperInstance().getQueueService().getPosition(sender.getUniqueId(), queue) + 1,
                        AltaraPaper.getPaperInstance().getQueueService().getQueueing(queue).size()
                )));
        sender.sendMessage(CC.format("&9Primary: &e%s",
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
                        "&9%s: &e%d",
                        UUIDCache.getName(uuid),
                        AltaraPaper.getPaperInstance().getQueueService().getPosition(uuid, server.getName()) + 1)));
        return true;
    }

}
