package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * The default public channel.  Messages are relayed to every server; whether a
 * player sees messages from <em>other</em> servers is controlled by their
 * {@link AltaraSettings#GLOBAL_CHAT} preference.
 */
public final class GlobalChannel extends ChatChannel {

    private static final GlobalChannel INSTANCE = new GlobalChannel();
    public static GlobalChannel getInstance() { return INSTANCE; }

    private GlobalChannel() {
        super("Global", null, true, true, true);
    }

    @Override
    public Component format(Profile sender, String message) {
        return Component.text()
                .append(senderName(sender))
                .append(Component.text(" " + Theme.ARROW + " ", Theme.STRUCTURE))
                .append(messageComponent(sender, message))
                .build();
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            /** Local delivery — anyone who hasn't disabled chat. */
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return AltaraSettings.ALL_CHAT.get(viewer);
            }

            /** Remote delivery — only players who opt in to cross-realm chat. */
            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return AltaraSettings.GLOBAL_CHAT.get(viewer) && AltaraSettings.ALL_CHAT.get(viewer);
            }
        };
    }
}
