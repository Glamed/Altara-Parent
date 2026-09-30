package games.sparking.altara.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import games.sparking.altara.AltaraProxy;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;

import java.util.List;
import java.util.Optional;

/** {@code /send <player> <server>}: moves a player to another backend server. */
public class SendCommand implements SimpleCommand {

    public static final String PERMISSION = "altara.command.send";

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length != 2) {
            source.sendMessage(CC.error("Invalid syntax.", "Try */send <player> <server>*."));
            return;
        }

        Optional<Player> targetOpt = AltaraProxy.getProxyInstance().getPlayer(args[0]);
        if (targetOpt.isEmpty()) {
            source.sendMessage(CC.error("Player not found.", "*" + args[0] + "* isn't online."));
            return;
        }

        Optional<RegisteredServer> serverOpt = AltaraProxy.getProxyInstance().getServer(args[1]);
        if (serverOpt.isEmpty()) {
            source.sendMessage(CC.error("Server not found.", "No server called *" + args[1] + "* is registered."));
            return;
        }

        Player target = targetOpt.get();
        String serverName = serverOpt.get().getServerInfo().getName();
        target.createConnectionRequest(serverOpt.get()).connect().thenAccept(result -> {
            switch (result.getStatus()) {
                case SUCCESS -> {
                    source.sendMessage(CC.success("Player sent.", "*" + target.getUsername() + "* is now on *" + serverName + "*."));
                    if (source != target) target.sendMessage(CC.notice("You were moved.", "A staff member sent you to *" + serverName + "*."));
                }
                case ALREADY_CONNECTED -> source.sendMessage(CC.error("Already there.",
                        "*" + target.getUsername() + "* is already on *" + serverName + "*."));
                case CONNECTION_IN_PROGRESS -> source.sendMessage(CC.error("Already connecting.",
                        "*" + target.getUsername() + "* is already being moved."));
                case CONNECTION_CANCELLED -> source.sendMessage(CC.error("Connection cancelled.",
                        "A plugin cancelled the move."));
                case SERVER_DISCONNECTED -> source.sendMessage(CC.error(Messages.REALM_OFFLINE, serverName));
            }
        }).exceptionally(throwable -> {
            source.sendMessage(CC.error("Couldn't send player.", "*" + serverName + "* didn't respond."));
            return null;
        });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase();

        if (args.length <= 1) {
            return AltaraProxy.getProxyInstance().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            return AltaraProxy.getProxyInstance().getAllServers().stream()
                    .map(server -> server.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
