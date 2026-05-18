package games.sparking.altara.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import games.sparking.altara.AltaraProxy;
import net.kyori.adventure.text.Component;

public class LobbyCommand implements SimpleCommand {


    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command."));
            return;
        }

        AltaraProxy.getProxyInstance().getServer("Lobby-1").ifPresentOrElse(
                registeredServer -> player.createConnectionRequest(registeredServer).fireAndForget(),
                () -> player.sendMessage(Component.text("Lobby server is offline or not registered."))
        );
    }
}
