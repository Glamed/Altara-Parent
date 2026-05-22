package games.sparking.altara.chat;


import org.bukkit.entity.Player;

public interface DefaultChannelProvider {

    DefaultChannelProvider DEFAULT = (_) -> ChatService.getDefaultChannel();

    ChatChannel getDefaultChannel(Player player);

}
