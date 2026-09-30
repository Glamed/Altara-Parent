package games.sparking.altara.chat.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Theme;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Private messages.  {@code (To Name) message} for the sender,
 * {@code (From Name) message} for the recipient, {@code [Spy] (A → B) message}
 * for social spies.  Message text is never parsed as MiniMessage.
 */
@Setter
@Getter
public final class DirectMessageChannel {

    private static final DirectMessageChannel INSTANCE = new DirectMessageChannel();
    public static DirectMessageChannel getInstance() { return INSTANCE; }

    /** When {@code true} every DM is printed to the server console. */
    private boolean log = true;

    private DirectMessageChannel() {}

    public void dispatch(Profile sender, Profile target, String message, List<Profile> spies) {
        sender.player().sendMessage(formatOutgoing(target, message));
        target.player().sendMessage(formatIncoming(sender, message));

        Component toSpy = formatSpy(sender, target, message);
        List<String> spyNames = new ArrayList<>();
        for (Profile spy : spies) {
            Player spyPlayer = spy.player();
            if (spyPlayer != null) {
                spyPlayer.sendMessage(toSpy);
                spyNames.add(spy.getName());
            }
        }

        if (log) {
            Altara.getSharedInstance().getLogger().info("[DM] " + sender.getCurrentName() + " -> " + target.getCurrentName()
                    + (spyNames.isEmpty() ? "" : " (spied by: " + String.join(", ", spyNames) + ")") + ": " + message);
        }
    }

    /**
     * DM from a profiler-flagged player: the sender sees it as sent, but it is only
     * delivered to online staff (tagged {@code [SM]}), never to the target.
     */
    public void dispatchShadowMuted(Profile sender, Profile target, String message) {
        sender.player().sendMessage(formatOutgoing(target, message));

        Component toStaff = ShadowMuteChannel.staffMarker().append(formatSpy(sender, target, message));
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.getUniqueId().equals(sender.getUuid()) && staff.hasPermission(StaffChannel.PERMISSION)) {
                staff.sendMessage(toStaff);
            }
        }

        if (log) {
            Altara.getSharedInstance().getLogger().info(
                    "[DM] [SHADOW-MUTE] " + sender.getCurrentName() + " -> " + target.getCurrentName() + ": " + message);
        }
    }

    private Component formatOutgoing(Profile target, String message) {
        return Component.text()
                .append(Component.text("(To ", Theme.TEXT))
                .append(name(target))
                .append(Component.text(") ", Theme.TEXT))
                .append(Component.text(message, Theme.TEXT_STRONG))
                .build();
    }

    private Component formatIncoming(Profile sender, String message) {
        return Component.text()
                .append(Component.text("(From ", Theme.TEXT))
                .append(name(sender))
                .append(Component.text(") ", Theme.TEXT))
                .append(Component.text(message, Theme.TEXT_STRONG))
                .hoverEvent(HoverEvent.showText(Component.text("Click to reply to " + sender.getCurrentName() + ".", Theme.TEXT)))
                .clickEvent(ClickEvent.suggestCommand("/msg " + sender.getCurrentName() + " "))
                .build();
    }

    private Component formatSpy(Profile sender, Profile target, String message) {
        return Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text("Spy", Theme.SPECIAL))
                .append(Component.text("] (", Theme.STRUCTURE))
                .append(name(sender))
                .append(Component.text(" " + Theme.ARROW + " ", Theme.STRUCTURE))
                .append(name(target))
                .append(Component.text(") ", Theme.STRUCTURE))
                .append(Component.text(message, Theme.TEXT_STRONG))
                .build();
    }

    /** Rank colour + current (possibly disguised) name. */
    private static Component name(Profile profile) {
        Rank rank = profile.getCurrentGrant().asRank();
        return CC.format(rank.getColor() + "<name>", Placeholder.unparsed("name", profile.getCurrentName()));
    }
}
