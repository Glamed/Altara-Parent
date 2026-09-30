package games.sparking.altara.server.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.server.menu.ServerListMenu;
import games.sparking.altara.server.packet.ExecuteCommandPacket;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Header(value = "Server Monitor", panel = Panel.STAFF)
public class ServerMonitorCommands {

    /** Whether this server publishes its heartbeat (debug switch). */
    public static volatile boolean SEND_PACKET = true;

    @Command(names = {"servermanager list", "sm list", "servers"}, permission = "servermanager.command.argument.list",
            description = "View every server", playerOnly = true)
    public boolean smList(Player sender) {
        new ServerListMenu().openMenu(sender);
        return true;
    }

    @Command(names = {"servermanager toggleupdate", "sm toggleupdate"}, permission = "op", hidden = true,
            description = "Toggle this server's heartbeat (debug only)")
    public boolean smToggleUpdate(CommandSender sender) {
        SEND_PACKET = !SEND_PACKET;
        sender.sendMessage(CC.notice("Heartbeat " + (SEND_PACKET ? "enabled." : "disabled."),
                SEND_PACKET ? "Other servers can see this one again." : "Other servers will mark this one as timed out."));
        return true;
    }

    @Command(names = {"servermanager sendtogroup", "sm sendtogroup"}, permission = "owner",
            description = "Run a console command on a server group")
    public boolean smSendToGroup(CommandSender sender, @Param(name = "group") String scope,
                                 @Param(name = "command", wildcard = true) String command) {
        new ExecuteCommandPacket(sender.getName(), null, scope, command).publish();
        sender.sendMessage(CC.success("Command sent.", "Running */" + command + "* on every *" + scope + "* server."));
        return true;
    }

    @Command(names = {"servermanager sendto", "sm sendto"}, permission = "owner",
            description = "Run a console command on one server")
    public boolean smSendToServer(CommandSender sender, @Param(name = "server") String server,
                                  @Param(name = "command", wildcard = true) String command) {
        new ExecuteCommandPacket(sender.getName(), server, null, command).publish();
        sender.sendMessage(CC.success("Command sent.", "Running */" + command + "* on *" + server + "*."));
        return true;
    }

    @Command(names = {"servermanager sendtoall", "sm sendtoall"}, permission = "owner",
            description = "Run a console command on every server")
    public boolean smSendToAll(CommandSender sender, @Param(name = "command", wildcard = true) String command) {
        new ExecuteCommandPacket(sender.getName(), null, null, command).publish();
        sender.sendMessage(CC.success("Command sent.", "Running */" + command + "* on every server."));
        return true;
    }
}
