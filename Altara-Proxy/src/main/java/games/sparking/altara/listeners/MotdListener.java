package games.sparking.altara.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MotdListener {

    @Subscribe
    public void onPing(ProxyPingEvent event) {

        MiniMessage mm = MiniMessage.miniMessage();

        String motd =
                "<dark_gray>><dark_aqua>><dark_gray>> " +
                        "<aqua><bold>McFriends</bold> Network " +
                        "<dark_gray><italic>[<yellow>1.21<dark_gray>] " +
                        "<dark_gray><<dark_aqua><<dark_gray>< " +
                        "<dark_gray>mcfriends.us\n" +

                        "<gray>Releasing SOON more at <light_purple>discord.mcfriends.us";

        var newPing = event.getPing().asBuilder()
                .description(mm.deserialize(motd))
                .build();

        event.setPing(newPing);
    }
}
