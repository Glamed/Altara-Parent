package games.sparking.altara.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import games.sparking.altara.AltaraProxy;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;

import java.util.Comparator;
import java.util.Optional;

/** {@code /lobby}: sends the player to the emptiest server whose name starts with "lobby". */
public class LobbyCommand implements SimpleCommand {

    private static final String LOBBY_PREFIX = "lobby";

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(CC.error(Messages.PLAYERS_ONLY));
            return;
        }

        boolean inLobby = player.getCurrentServer()
                .map(connection -> isLobby(connection.getServer()))
                .orElse(false);
        if (inLobby) {
            player.sendMessage(CC.error("Already connected.", "You're already in a lobby."));
            return;
        }

        Optional<RegisteredServer> lobby = AltaraProxy.getProxyInstance().getAllServers().stream()
                .filter(LobbyCommand::isLobby)
                .min(Comparator.comparingInt(server -> server.getPlayersConnected().size()));
        if (lobby.isEmpty()) {
            player.sendMessage(CC.error("No lobby available.", "Please try again in a moment."));
            return;
        }

        player.sendMessage(CC.info("Sending you to *" + lobby.get().getServerInfo().getName() + "*..."));
        player.createConnectionRequest(lobby.get()).connect().thenAccept(result -> {
            if (!result.isSuccessful()) {
                player.sendMessage(CC.error("Couldn't connect.", "The lobby didn't accept the connection. Please try again."));
            }
        });
    }

    private static boolean isLobby(RegisteredServer server) {
        return server.getServerInfo().getName().toLowerCase().startsWith(LOBBY_PREFIX);
    }
}
