package games.sparking.altara.chat.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.chat.ChannelAudience;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * System-only channel used by the Profiler to silently quarantine suspicious
 * players.  The sender sees their own message normally; staff see it with a
 * {@code [SM]} marker.  Messages are never relayed to other servers.
 *
 * <p>This channel is <em>not persistable</em> — moving a player into it never
 * overwrites their saved channel preference, so they return to the correct
 * channel once the shadow mute is lifted.
 */
public final class ShadowMuteChannel extends ChatChannel {

    private static final ShadowMuteChannel INSTANCE = new ShadowMuteChannel();
    public static ShadowMuteChannel getInstance() { return INSTANCE; }

    private ShadowMuteChannel() {
        // no prefix — players cannot switch into this channel manually
        super("ShadowMute", null, true, false, false);
    }

    // ── Custom dispatch ────────────────────────────────────────────────────────

    /**
     * Overrides the default dispatch so only the sender and online staff receive
     * the message, with appropriate formatting per audience.
     */
    @Override
    public void dispatch(Player sender, String rawMessage) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender.getUniqueId());
        Component senderView = format(profile != null ? profile : new Profile(sender.getUniqueId(), sender.getName()), rawMessage);
        Component staffView  = formatForStaff(profile != null ? profile : new Profile(sender.getUniqueId(), sender.getName()), rawMessage);

        List<String> staffRecipients = new ArrayList<>();

        // Sender always receives their own message (appears normal to them).
        sender.sendMessage(senderView);

        // Staff receive the staff-annotated version.
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.getUniqueId().equals(sender.getUniqueId())) continue;
            if (staff.hasPermission("altara.staff")) {
                staff.sendMessage(staffView);
                staffRecipients.add(staff.getName());
            }
        }

        // Console always gets the staff view.
        Bukkit.getConsoleSender().sendMessage(staffView);

        // Log.
        Altara.getSharedInstance().getLogger().info(
                "[SHADOW-MUTE] " + sender.getName() + " -> staff[" +
                String.join(", ", staffRecipients) + "]: " + rawMessage);
    }

    // ── Formatting ─────────────────────────────────────────────────────────────

    /** The version the muted player sees — looks like normal global chat. */
    @Override
    public Component format(Profile sender, String message) {
        return Component.empty()
                .append(CC.format(sender.getCurrentGrant().asRank().getPrefix()))
                .append(CC.format(sender.getCurrentName()))
                .append(CC.format(sender.getCurrentGrant().asRank().getSuffix()))
                .append(Component.text(": "))
                .append(CC.format(sender.getCurrentGrant().asRank().getChatColor() + message));
    }

    /** The version staff sees — prefixed with a bold red [SM] tag. */
    private Component formatForStaff(Profile sender, String message) {
        return Component.empty()
                .append(Component.text("[SM] ", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(format(sender, message));
    }

    // ── Audience (fallback — dispatch is fully overridden above) ───────────────

    @Override
    public ChannelAudience getAudience() {
        return new ChannelAudience() {
            @Override
            public boolean canSee(Player viewer, Player sender, ChatChannel channel) {
                return viewer.getUniqueId().equals(sender.getUniqueId())
                        || viewer.hasPermission("altara.staff");
            }

            @Override
            public boolean canSeeRemote(Player viewer, ChatChannel channel) {
                return false;
            }
        };
    }
}

