package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.chat.FilteredChatChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * Staff-only channel relayed across all servers.
 * Requires {@code altara.staff} to receive.
 *
 * <p>Prefix: {@code @}
 */
public final class StaffChannel extends FilteredChatChannel {

    private static final StaffChannel INSTANCE = new StaffChannel();
    public static StaffChannel getInstance() { return INSTANCE; }

    private StaffChannel() {
        super("Staff", "@", true, true, true, "altara.staff");
    }

    @Override
    public Component format(Profile sender, String message) {
        return Component.empty()
                .append(Component.text("[STAFF] ", NamedTextColor.AQUA))
                .append(CC.format(sender.getCurrentGrant().asRank().getPrefix()))
                .append(CC.format(sender.getCurrentName()))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {

            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return AltaraSettings.STAFF_MESSAGES.get(viewer);
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return AltaraSettings.STAFF_MESSAGES.get(viewer);
            }
        };
    }

}

