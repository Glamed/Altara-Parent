package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * The default public channel. Every message is published to Redis so other
 * servers receive it — but whether a player *sees* cross-server messages is
 * controlled by their {@link AltaraSettings#GLOBAL_CHAT} preference.
 *
 * <p>Prefix: {@code !}
 */
public final class GlobalChannel extends ChatChannel {

    private static final GlobalChannel INSTANCE = new GlobalChannel();
    public static GlobalChannel getInstance() { return INSTANCE; }

    private GlobalChannel() {
        super("Global", null, true, true, true);
    }

    @Override
    public Component format(Profile sender, String message) {
        Rank rank = sender.getCurrentGrant().asRank();
        return Component.empty()
                .append(CC.format(rank.getPrefix()))
                .append(CC.format(rank.getColor() + sender.getCurrentName()))
                .append(CC.format(" <dark_gray>» "))
                .append(CC.format(sender.getCurrentGrant().asRank().getChatColor() + message));
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {

            /**
             * Local delivery — every player on this server sees the message as long
             * as they haven't disabled ALL_CHAT.  The GLOBAL_CHAT preference only
             * controls whether they receive relayed messages from *other* servers.
             */
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return AltaraSettings.ALL_CHAT.get(viewer);
            }

            /**
             * Remote delivery — only players who opt in to cross-server messages
             * (GLOBAL_CHAT = true) will receive this relay.
             */
            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return AltaraSettings.GLOBAL_CHAT.get(viewer)
                        && AltaraSettings.ALL_CHAT.get(viewer);
            }
        };
    }
}

