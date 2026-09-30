package games.sparking.altara.chat.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * System-only channel used by the Profiler to silently quarantine suspicious
 * players.  The sender sees their own message exactly as normal global chat; staff
 * see it with a {@code [SM]} marker.  Messages are never relayed to other servers.
 *
 * <p>Players are never switched into this channel — {@code ChatListener} routes a
 * flagged player's messages here regardless of their selected channel.
 */
public final class ShadowMuteChannel extends ChatChannel {

    private static final ShadowMuteChannel INSTANCE = new ShadowMuteChannel();
    public static ShadowMuteChannel getInstance() { return INSTANCE; }

    private ShadowMuteChannel() {
        super("ShadowMute", null, true, false, false);
    }

    @Override
    public boolean canUse(Player player) {
        return false;
    }

    @Override
    public void dispatch(Player sender, String rawMessage) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender.getUniqueId());
        if (profile == null) profile = fallbackProfile(sender);

        Component senderView = format(profile, rawMessage);
        Component staffView = staffMarker().append(senderView);
        List<String> staffRecipients = new ArrayList<>();

        sender.sendMessage(senderView);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.equals(sender) && staff.hasPermission(StaffChannel.PERMISSION)) {
                staff.sendMessage(staffView);
                staffRecipients.add(staff.getName());
            }
        }
        Bukkit.getConsoleSender().sendMessage(staffView);

        Altara.getSharedInstance().getLogger().info(
                "[SHADOW-MUTE] " + sender.getName() + " -> staff[" + String.join(", ", staffRecipients) + "]: " + rawMessage);
    }

    /** Identical to {@link GlobalChannel#format} so the sender can't tell. */
    @Override
    public Component format(Profile sender, String message) {
        return GlobalChannel.getInstance().format(sender, message);
    }

    /** Bold dark-red {@code [SM]} tag prepended to messages shown to staff. */
    public static Component staffMarker() {
        return Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text("SM", Theme.ERROR_DARK, TextDecoration.BOLD))
                .append(Component.text("] ", Theme.STRUCTURE))
                .build();
    }

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return viewer.equals(sender) || viewer.hasPermission(StaffChannel.PERMISSION);
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return false;
            }
        };
    }
}
