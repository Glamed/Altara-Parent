package games.sparking.altara.server.parameter;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.queue.commands.QueueCommands;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AllServersParameter implements ParameterType<ServerInfo> {

    @Override
    public ServerInfo parse(CommandSender sender, String source) {
        ServerInfo parsed = ServerInfo.getServerInfo(source);
        if (parsed == null) {
            sender.sendMessage(CC.error("Invalid realm.", "*" + source + "* doesn't exist."));
        }
        return parsed;
    }

    /** With the {@code accessible} flag, only realms the sender may join are suggested. */
    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return ServerInfo.getServers().stream()
                .filter(server -> !flags.contains("accessible")
                        || (!server.isProxy() && sender.hasPermission(QueueCommands.accessPermission(server))))
                .map(ServerInfo::getName)
                .toList();
    }
}
