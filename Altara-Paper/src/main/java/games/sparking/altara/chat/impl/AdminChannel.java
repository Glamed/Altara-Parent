package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.FilteredChatChannel;
import games.sparking.altara.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Admin-only channel relayed across all servers.
 * Requires {@code altara.admin} to receive.
 *
 * <p>Prefix: {@code $}
 */
public final class AdminChannel extends FilteredChatChannel {

    private static final AdminChannel INSTANCE = new AdminChannel();
    public static AdminChannel getInstance() { return INSTANCE; }

    private AdminChannel() {
        super("Admin", "$", true, true, true, "altara.admin");
    }

    @Override
    public Component format(Profile sender, String message) {
        return Component.empty()
                .append(Component.text("[ADMIN] ", NamedTextColor.DARK_RED))
                .append(legacy(sender.getCurrentGrant().asRank().getPrefix()))
                .append(legacy(sender.getCurrentName()))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }
}

