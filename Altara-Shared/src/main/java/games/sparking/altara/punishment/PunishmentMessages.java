package games.sparking.altara.punishment;

import games.sparking.altara.Altara;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.ArrayList;
import java.util.List;

/** Player-facing punishment text, shared by the login check and live enforcement. */
public final class PunishmentMessages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private PunishmentMessages() {}

    private static String website() {
        return Altara.getSharedInstance().getMainConfig().getServerConfig().getWebsite();
    }

    private static String reasonOf(Punishment punishment) {
        return punishment.getReason() != null ? punishment.getReason().getDisplayName() : "Policy Violation";
    }

    /** Disconnect screen for an active suspension. */
    public static Component suspensionScreen(Punishment punishment) {
        long remaining = punishment.getRemainingDuration(PunishmentType.SUSPENSION);
        boolean automated = punishment.getReason() == InfractionType.TEMP_AUTOMATED;

        TextComponent.Builder screen = Component.text()
                .append(Component.text("Your account has been suspended.", Theme.ERROR))
                .append(Component.newline())
                .append(Component.text(reasonOf(punishment), Theme.TEXT_STRONG))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(remaining == -1 ? "This suspension doesn't expire." : "This suspension expires in ", Theme.TEXT));

        if (remaining != -1) {
            screen.append(Component.text(Time.formatDetailed(remaining), Theme.TEXT_STRONG))
                    .append(Component.text(".", Theme.TEXT));
        }

        screen.append(Component.newline());
        if (automated) {
            screen.append(Component.text("This action was automated and is pending staff review.", Theme.TEXT));
        } else {
            screen.append(Component.text("Appeal at ", Theme.TEXT))
                    .append(Component.text(website() + "/appeal", Theme.PRIMARY));
        }
        return screen.build();
    }

    /**
     * Chat notice for non-suspension restrictions (mutes, warnings, …).  The player's name and
     * the offending message are inserted as plain text — they are player input and must never
     * be parsed as MiniMessage.
     */
    public static List<Component> restrictionNotice(Punishment punishment, String playerName) {
        List<Component> lines = new ArrayList<>();
        lines.add(dashLine("Account Action"));
        lines.add(MM.deserialize(" <gray>Your recent activity violated our Terms of Service"));

        if (punishment.getMessage() != null) {
            lines.add(Component.empty());
            lines.add(MM.deserialize("  <dark_gray><bold>→ </bold><dark_gray>[<gray>Member<dark_gray>]<gray> <name> <dark_gray>»<white> <message>",
                    Placeholder.unparsed("name", playerName),
                    Placeholder.unparsed("message", punishment.getMessage())));
            lines.add(Component.empty());
        }

        lines.add(Component.empty());
        lines.add(MM.deserialize(" <gray>We took these actions<dark_gray>:"));
        if (punishment.getMessage() != null) {
            lines.add(actionLine("This content has been removed so no one can see it."));
        }
        if (punishment.getActions() != null) {
            for (RestrictionAction action : punishment.getActions()) {
                lines.add(actionLine(action.getType().getActionLine(action.getDuration())));
            }
        }

        lines.add(Component.empty());
        if (punishment.getReason() == InfractionType.TEMP_AUTOMATED) {
            lines.add(MM.deserialize(" <gray>Why this action was taken<dark_gray>:"));
            lines.add(MM.deserialize("  <gray>This temporary action was triggered by our automated"));
            lines.add(MM.deserialize("  <gray>moderation systems and is pending staff review."));
            lines.add(Component.empty());
            lines.add(MM.deserialize(" <gray>You cannot appeal this action at this time."));
        } else {
            lines.add(MM.deserialize(" <gray>Why we took these actions<dark_gray>:"));
            lines.add(MM.deserialize("  <gray>Our trust and safety team believes you have violated"));
            lines.add(MM.deserialize("  <gray>our community guidelines on <red><reason><gray>.",
                    Placeholder.unparsed("reason", reasonOf(punishment))));
            lines.add(Component.empty());
            lines.add(MM.deserialize(" <gray>Please review our <aqua><underlined>Community Guidelines<gray>."));
            lines.add(MM.deserialize(" <gray>Did we make a mistake? <aqua><underlined>Let us know<gray>!"));
        }

        lines.add(dashLine(null));
        return lines;
    }

    private static Component actionLine(String text) {
        return MM.deserialize("  <dark_red>✕ <red><line>", Placeholder.unparsed("line", text));
    }

    /** Alternating dark gray / dark red line, 48 dashes wide, optionally with a centred label. */
    private static Component dashLine(String label) {
        int dashes = label == null || label.isBlank() ? 48 : Math.max(2, (48 - label.length() - 4) / 2);
        TextComponent.Builder line = Component.text();
        boolean alt = true;
        for (int i = 0; i < dashes; i++) {
            line.append(Component.text("-", alt ? NamedTextColor.DARK_GRAY : NamedTextColor.DARK_RED));
            alt = !alt;
        }
        if (label == null || label.isBlank()) return line.build();

        line.append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(label, NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("]", NamedTextColor.DARK_GRAY));
        for (int i = 0; i < dashes; i++) {
            line.append(Component.text("-", alt ? NamedTextColor.DARK_GRAY : NamedTextColor.DARK_RED));
            alt = !alt;
        }
        return line.build();
    }

    /** One-line reply when a muted player tries to chat. */
    public static Component chatRestricted(Punishment mute) {
        long remaining = mute == null ? -1 : mute.getRemainingDuration(PunishmentType.CHAT_RESTRICTION);
        return CC.error("You're muted.", remaining == -1
                ? "Your chat restriction doesn't expire."
                : "You can chat again in *" + Time.formatDetailed(remaining) + "*.");
    }

}
