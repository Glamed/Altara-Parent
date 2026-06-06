package games.sparking.altara.chat.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles the formatting, delivery, and logging of direct (private) messages.
 *
 * <p>This is not a switchable channel — it is used internally by
 * {@code MessageCommands} so that DMs pass
 * through the same formatting and logging infrastructure as every other channel.
 *
 * <p>Logging can be toggled at runtime via {@link #setLog(boolean)}.
 */
public final class DirectMessageChannel {

    private static final DirectMessageChannel INSTANCE = new DirectMessageChannel();
    public static DirectMessageChannel getInstance() { return INSTANCE; }

    /** When {@code true} every DM is printed to the server console. */
    private boolean log = true;

    private DirectMessageChannel() {}

    public boolean isLog() { return log; }
    public void setLog(boolean log) { this.log = log; }

    // ── Dispatch ───────────────────────────────────────────────────────────────

    /**
     * Formats and delivers a private message from {@code sender} to
     * {@code target}, then logs it and notifies any social spies.
     *
     * @param sender   the sending profile
     * @param target   the receiving profile
     * @param message  raw message text
     * @param spies    profiles of players who are social-spying on this conversation
     */
    public void dispatch(Profile sender, Profile target, String message, List<Profile> spies) {
        Component toSender   = formatOutgoing(sender, target, message);
        Component toTarget   = formatIncoming(sender, target, message);
        Component toSpy      = formatSpy(sender, target, message);

        sender.player().sendMessage(toSender);
        target.player().sendMessage(toTarget);

        List<String> spyNames = new ArrayList<>();
        for (Profile spy : spies) {
            if (spy.player() != null) {
                spy.player().sendMessage(toSpy);
                spyNames.add(spy.getName());
            }
        }

        if (log) {
            Altara.getSharedInstance().getLogger().info(
                    "[DM] " + sender.getCurrentName() +
                    " -> " + target.getCurrentName() +
                    (spyNames.isEmpty() ? "" : " (spied by: " + String.join(", ", spyNames) + ")") +
                    ": " + message);
        }
    }

    // ── Formatting ─────────────────────────────────────────────────────────────

    /** What the sender sees: {@code (To <target>) message} */
    private Component formatOutgoing(Profile sender, Profile target, String message) {
        return Component.empty()
                .append(Component.text("(To ", NamedTextColor.GRAY))
                .append(rankName(target))
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    /** What the target sees: {@code (From <sender>) message} */
    private Component formatIncoming(Profile sender, Profile target, String message) {
        return Component.empty()
                .append(Component.text("(From ", NamedTextColor.GRAY))
                .append(rankName(sender))
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    /** What social spies see: {@code (<sender> -> <target>) message} */
    private Component formatSpy(Profile sender, Profile target, String message) {
        return Component.empty()
                .append(Component.text("[SPY] ", NamedTextColor.GOLD))
                .append(Component.text("(", NamedTextColor.GRAY))
                .append(rankName(sender))
                .append(Component.text(" -> ", NamedTextColor.GRAY))
                .append(rankName(target))
                .append(Component.text(") ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));
    }

    private static Component rankName(Profile profile) {
        String prefix = profile.getCurrentGrant().asRank().getPrefix();
        String name   = profile.getCurrentName();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + name);
    }
}



