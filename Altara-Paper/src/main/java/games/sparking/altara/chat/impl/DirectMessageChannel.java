package games.sparking.altara.chat.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public final class DirectMessageChannel {

    private static final DirectMessageChannel INSTANCE = new DirectMessageChannel();
    public static DirectMessageChannel getInstance() { return INSTANCE; }

    /** When {@code true} every DM is printed to the server console. */
    private boolean log = true;

    private DirectMessageChannel() {}

    public void dispatch(Profile sender, Profile target, String message, List<Profile> spies) {
        Component toSender = formatOutgoing(sender, target, message);
        Component toTarget = formatIncoming(sender, target, message);
        Component toSpy    = formatSpy(sender, target, message);

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
                            ": " + message
            );
        }
    }

    private Component formatOutgoing(Profile sender, Profile target, String message) {
        return CC.format(
                "<gray>(To </gray>" + rankNameLegacy(target) + "<gray>) </gray><white>" + message
        );
    }

    private Component formatIncoming(Profile sender, Profile target, String message) {
        return CC.format(
                "<gray>(From </gray>" + rankNameLegacy(sender) + "<gray>) </gray><white>" + message
        );
    }

    private Component formatSpy(Profile sender, Profile target, String message) {
        return CC.format(
                "<gold>[SPY] </gold><gray>(" +
                        rankNameLegacy(sender) +
                        "<gray> -> </gray>" +
                        rankNameLegacy(target) +
                        "<gray>) </gray><white>" +
                        message
        );
    }

    private static String rankNameLegacy(Profile profile) {
        String prefix = profile.getCurrentGrant().asRank().getPrefix();
        String name   = profile.getCurrentName();
        return prefix + name;
    }
}