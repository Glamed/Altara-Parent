package games.sparking.altara.queue.commands;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.CommandCooldown;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.queue.QueueService;
import games.sparking.altara.queue.packet.QueueJoinPacket;
import games.sparking.altara.queue.packet.QueueLeavePacket;
import games.sparking.altara.queue.packet.QueueSendPlayerPacket;
import games.sparking.altara.queue.packet.update.QueuePausePacket;
import games.sparking.altara.queue.packet.update.QueueRatePacket;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@Header(value = "Queue", panel = Panel.STAFF)
public class QueueCommands {

    public static final String BYPASS_PERMISSION = "altara.queue.bypass";

    /** Permission needed to join realms of a server group. */
    public static String accessPermission(ServerInfo server) {
        return "altara.server." + server.getGroup().toLowerCase();
    }

    private static QueueService service() {
        return AltaraPaper.getPaperInstance().getQueueService();
    }

    @Command(names = {"joinqueue", "play", "realm", "jq"}, description = "Join a realm's queue",
            playerOnly = true, async = true)
    @CommandCooldown(time = 5)
    public boolean joinQueue(Player sender, @Param(name = "realm", completionFlags = {"accessible"}) ServerInfo server) {
        if (server.getName().equalsIgnoreCase(AltaraPaper.getSharedInstance().getLocalServerName())) {
            sender.sendMessage(CC.error("Already connected.", "You're already on *" + server.getName() + "*."));
            return false;
        }
        if (!server.isOnline()) {
            sender.sendMessage(CC.error(Messages.REALM_OFFLINE, server.getName()));
            return false;
        }
        if (!server.isQueueEnabled() || server.isProxy()) {
            sender.sendMessage(CC.error(Messages.REALM_UNAVAILABLE, server.getName()));
            return false;
        }
        if (!sender.hasPermission(accessPermission(server))) {
            sender.sendMessage(CC.error("No access.", "You can't join *" + server.getName() + "*."));
            return false;
        }

        if (sender.hasPermission(BYPASS_PERMISSION)) {
            new QueueSendPlayerPacket(server.getName(), sender.getUniqueId()).publish();
            return true;
        }

        if (service().isQueueingFor(sender.getUniqueId(), server.getName())) {
            sender.sendMessage(CC.error("Already queued.", "You're already in the *" + server.getName() + "* queue."));
            return false;
        }

        new QueueJoinPacket(server.getName(), sender.getUniqueId()).publish();
        sender.sendMessage(CC.success("Queue joined.", "You're now in the *" + server.getName() + "* queue."));
        return true;
    }

    @Command(names = {"leavequeue", "lq"}, description = "Leave a realm's queue", playerOnly = true, async = true)
    public boolean leaveQueue(Player sender, @Param(name = "realm") ServerInfo server) {
        if (!service().isQueueingFor(sender.getUniqueId(), server.getName())) {
            sender.sendMessage(CC.error("Not queued.", "You aren't in the *" + server.getName() + "* queue."));
            return false;
        }

        new QueueLeavePacket(server.getName(), sender.getUniqueId()).publish();
        sender.sendMessage(CC.success("Queue left.", "You've left the *" + server.getName() + "* queue."));
        return true;
    }

    @Command(names = {"queue pause"}, permission = "queue.command.argument.pause",
            description = "Pause or resume a queue", async = true)
    public boolean queuePause(CommandSender sender, @Param(name = "realm") ServerInfo server) {
        if (!checkManageable(sender, server)) return false;

        boolean paused = !server.isQueuePaused();
        new QueuePausePacket(server.getName(), paused).publish();
        sender.sendMessage(CC.success("Queue updated.", "The *" + server.getName() + "* queue is now *" + (paused ? "paused" : "running") + "*."));
        return true;
    }

    @Command(names = {"queue rate"}, permission = "queue.command.argument.rate",
            description = "Set how many players are sent per second", async = true)
    public boolean queueRate(CommandSender sender, @Param(name = "realm") ServerInfo server, @Param(name = "rate") int rate) {
        if (!checkManageable(sender, server)) return false;
        if (rate < 1 || rate > 100) {
            sender.sendMessage(CC.error("Invalid rate.", "Choose a rate between *1* and *100*."));
            return false;
        }

        new QueueRatePacket(server.getName(), rate).publish();
        sender.sendMessage(CC.success("Queue updated.", "The *" + server.getName() + "* queue now sends *" + rate + "* per second."));
        return true;
    }

    @Command(names = {"queue info"}, permission = "queue.command.argument.info",
            description = "View a queue's status", async = true)
    public boolean queueInfo(CommandSender sender, @Param(name = "realm") ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.error(Messages.REALM_UNAVAILABLE, server.getName()));
            return false;
        }

        List<UUID> queued = service().getQueueing(server.getName());
        sender.sendMessage(CC.header(Panel.STAFF, "Queue", server.getName()));
        sender.sendMessage(CC.item("State", CC.state(!server.isQueuePaused(), "Running", "Paused")));
        sender.sendMessage(CC.item("Rate", server.getQueueRate() + " per second"));
        sender.sendMessage(CC.item("Queued", String.valueOf(queued.size())));
        for (int i = 0; i < Math.min(10, queued.size()); i++) {
            String name = UUIDCache.getName(queued.get(i));
            sender.sendMessage(CC.nested("#" + (i + 1) + " " + (name != null ? name : queued.get(i).toString())));
        }
        sender.sendMessage(CC.footer(Panel.STAFF));
        return true;
    }

    @Command(names = {"queue debugme"}, permission = "queue.command.argument.debugme",
            description = "View the queues you're in", async = true, playerOnly = true)
    public boolean queueDebugMe(Player sender) {
        List<String> queues = service().getQueues(sender.getUniqueId());

        sender.sendMessage(CC.header(Panel.STAFF, "Your Queues"));
        if (queues.isEmpty()) sender.sendMessage(CC.empty("You aren't in any queues."));
        for (String queue : queues) {
            sender.sendMessage(CC.item(queue, "#" + (service().getPosition(sender.getUniqueId(), queue) + 1)
                    + " of " + service().getQueueing(queue).size()));
        }
        sender.sendMessage(CC.footer(Panel.STAFF));
        return true;
    }

    private static boolean checkManageable(CommandSender sender, ServerInfo server) {
        if (!server.isQueueEnabled()) {
            sender.sendMessage(CC.error(Messages.REALM_UNAVAILABLE, server.getName()));
            return false;
        }
        if (!server.isOnline()) {
            sender.sendMessage(CC.error(Messages.REALM_OFFLINE, server.getName()));
            return false;
        }
        return true;
    }
}
