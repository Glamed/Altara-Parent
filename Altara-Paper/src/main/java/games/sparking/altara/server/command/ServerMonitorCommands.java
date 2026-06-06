package games.sparking.altara.server.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.server.menu.ServerListMenu;
import games.sparking.altara.server.packet.ExecuteCommandPacket;
import games.sparking.altara.utils.CC;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
@Header(
        primaryColor = "dark_red",
        secondaryColor = "dark_gray",
        tertiaryColor = "red",
        header = "Server Monitor"
)
public class ServerMonitorCommands {

    public static boolean SEND_PACKET = true;

    @Command(names = {"servermanager list", "sm list"},
             permission = "servermanager.command.argument.list",
             description = "List all available servers",
             playerOnly = true)
    public boolean smList(Player sender) {
        new ServerListMenu().openMenu(sender);
        return true;
    }

    @Command(names = {"servers"},
             permission = "servermanager.command.argument.list",
             description = "List all available servers",
             playerOnly = true)
    public boolean servers(Player sender) {
        return smList(sender);
    }

    @Command(names = {"servermanager toggleupdate", "sm toggleupdate"},
             permission = "op",
             hidden = true,
             description = "Toggle if the update packet of this server gets send (DEBUG ONLY!)")
    public boolean smToggleUpdate(CommandSender sender) {
        ServerMonitorCommands.SEND_PACKET = !ServerMonitorCommands.SEND_PACKET;
        sender.sendMessage(CC.YELLOW + "You have " + CC.colorBoolean(ServerMonitorCommands.SEND_PACKET)
                + CC.YELLOW + " the sending of the update packet for this server.");
        return true;
    }

    @Command(names = {"servermanager sendtogroup", "sm sendtogroup"}, permission = "owner")
    public boolean smSendToGroup(CommandSender sender,
                                 @Param(name = "scope") String scope,
                                 @Param(name = "command", wildcard = true) String command) {
       new ExecuteCommandPacket(sender.getName(), null, scope, command).publish();
        sender.sendMessage(CC.format(
                "<green>Executing <yellow>%s <green>on all <yellow>%s <green>servers.",
                command,
                scope
        ));
        return true;
    }

    @Command(names = {"servermanager sendto", "sm sendto"}, permission = "owner")
    public boolean smSendToServer(CommandSender sender,
                                  @Param(name = "server") String server,
                                  @Param(name = "command", wildcard = true) String command) {
        new ExecuteCommandPacket(sender.getName(), server, null, command).publish();
        sender.sendMessage(CC.format(
                "<green>Executing <yellow>%s <green>on <yellow>%s<green>.",
                command,
                server
        ));
        return true;
    }

    @Command(names = {"servermanager sendtoall", "sm sendtoall"}, permission = "owner")
    public boolean smSendToAll(CommandSender sender, @Param(name = "command", wildcard = true) String command) {
        new ExecuteCommandPacket(sender.getName(), null, null, command).publish();
        sender.sendMessage(CC.format(
                "<green>Executing <yellow>%s <green>on <green>all servers.",
                command
        ));
        return true;
    }

}
