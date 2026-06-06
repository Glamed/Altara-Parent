package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Local-server-only channel — messages are never relayed over Redis.
 * Useful for per-world or minigame chats.
 *
 * <p>Prefix: {@code #}
 */
public final class LocalChannel extends ChatChannel {

    private static final LocalChannel INSTANCE = new LocalChannel();
    public static LocalChannel getInstance() { return INSTANCE; }

    private LocalChannel() {
        super("Local", "#", true, false, true);
    }

    @Override
    public Component format(Profile sender, String message) {
        return Component.empty()
                .append(Component.text("[LOCAL] ", NamedTextColor.GRAY))
                .append(CC.format(sender.getCurrentGrant().asRank().getPrefix()))
                .append(CC.format(sender.getCurrentName()))
                .append(CC.format(" &8» "))
                .append(CC.format(sender.getCurrentGrant().asRank().getChatColor() + message));
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return AltaraSettings.ALL_CHAT.get(viewer);
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return false; // never relayed
            }
        };
    }
}

