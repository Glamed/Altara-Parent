package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.chat.FilteredChatChannel;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Staff-only channel relayed across all servers.  Requires {@code altara.staff} to
 * send or receive; staff can mute it with {@link AltaraSettings#STAFF_MESSAGES}.
 *
 * <p>Prefix: {@code @}
 */
public final class StaffChannel extends FilteredChatChannel {

    public static final String PERMISSION = "altara.staff";

    private static final StaffChannel INSTANCE = new StaffChannel();
    public static StaffChannel getInstance() { return INSTANCE; }

    private StaffChannel() {
        super("Staff", "@", true, true, true, PERMISSION);
    }

    @Override
    public Component format(Profile sender, String message) {
        return Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text("Staff", Theme.PRIMARY))
                .append(Component.text("] ", Theme.STRUCTURE))
                .append(senderName(sender))
                .append(Component.text(": ", Theme.TEXT))
                .append(Component.text(message, Theme.TEXT_STRONG))
                .build();
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return hasAccess(viewer) && AltaraSettings.STAFF_MESSAGES.get(viewer);
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return hasAccess(viewer) && AltaraSettings.STAFF_MESSAGES.get(viewer);
            }
        };
    }
}
