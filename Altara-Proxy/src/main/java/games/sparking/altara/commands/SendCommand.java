package games.sparking.altara.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import games.sparking.altara.AltaraProxy;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;

public class SendCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length < 2) {
            invocation.source().sendMessage(
                    Component.text("Usage: /send <player> <server>")
            );
            return;
        }

        String targetName = args[0];
        String serverName = args[1];

        Optional<Player> targetOpt = AltaraProxy.getProxyInstance().getPlayer(targetName);
        if (targetOpt.isEmpty()) {
            invocation.source().sendMessage(
                    Component.text("Player not found.")
            );
            return;
        }

        Optional<RegisteredServer> serverOpt = AltaraProxy.getProxyInstance().getServer(serverName);
        if (serverOpt.isEmpty()) {
            invocation.source().sendMessage(
                    Component.text("Server not found.")
            );
            return;
        }

        Player target = targetOpt.get();
        RegisteredServer targetServer = serverOpt.get();

        // CONNECT WITH RESULT CHECKING
        target.createConnectionRequest(targetServer).connect()
                .thenAccept(result -> {

                    switch (result.getStatus()) {

                        case SUCCESS -> {
                            invocation.source().sendMessage(
                                    Component.text("Sent " + target.getUsername() + " to " + serverName)
                            );

                            target.sendMessage(
                                    Component.text("You were sent to " + serverName)
                            );
                        }

                        case ALREADY_CONNECTED -> invocation.source().sendMessage(
                                Component.text(target.getUsername() + " is already on that server.")
                        );

                        case CONNECTION_IN_PROGRESS -> invocation.source().sendMessage(
                                Component.text("Connection already in progress for that player.")
                        );

                        case CONNECTION_CANCELLED -> invocation.source().sendMessage(
                                Component.text("Connection was cancelled by a plugin.")
                        );

                        case SERVER_DISCONNECTED -> {
                            invocation.source().sendMessage(
                                    Component.text("Failed: target server is offline.")
                            );
                        }

                        default -> invocation.source().sendMessage(
                                Component.text("Failed to send player (unknown reason).")
                        );
                    }
                });
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 1) {
            return AltaraProxy.getProxyInstance().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .toList();
        }

        if (args.length == 2) {
            return AltaraProxy.getProxyInstance().getAllServers().stream()
                    .map(s -> s.getServerInfo().getName())
                    .toList();
        }

        return List.of();
    }
}