package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.FilteredChatChannel;
import games.sparking.altara.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
                .append(legacy(sender.getCurrentGrant().asRank().getPrefix()))
                .append(legacy(sender.getCurrentName()))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }
}

