package games.sparking.altara.server.parameter;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.stream.Collectors;

public class AllServersParameter implements ParameterType<ServerInfo> {

    @Override
    public ServerInfo parse(CommandSender sender, String source) {
        ServerInfo parsed = ServerInfo.getServerInfo(source);
        if (parsed == null) {
            sender.sendMessage(CC.RED + "Server " + CC.YELLOW + source + CC.RED + " not found.");
        }
        return parsed;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return ServerInfo.getServers().stream()
                .map(ServerInfo::getName)
                .filter(name -> !flags.contains("accessible") || sender.hasPermission("invictus.server." + name))
                .collect(Collectors.toList());
    }
}
