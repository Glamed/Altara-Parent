package games.sparking.altara.punishment.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.Time;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Published by the Web API whenever a new {@link Punishment} is persisted to MongoDB.
 *
 * <p>Receipt is a no-op on non-Paper nodes. On Paper:
 * <ol>
 *   <li>The local {@code PunishmentService} cache is updated.</li>
 *   <li>If the punished player is online, the punishment is enforced immediately
 *       (kick for suspension, chat notice otherwise).</li>
 * </ol>
 */
@AllArgsConstructor
@NoArgsConstructor
public class PunishmentIssuedPacket extends Packet {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Punishment punishment;

    @Override
    public void receive() {
        // Only Paper servers need to react to issued punishments.
        if (Altara.getSystemType() != SystemType.PAPER) return;
        if (punishment == null) return;

        Altara.getSharedInstance().getPunishmentService().updateCacheFromPacket(punishment);
        Tasks.run(this::enforceLocally);
    }

    // ── Enforcement ────────────────────────────────────────────────────────────

    private void enforceLocally() {
        if (punishment.getPlayerUuid() == null) return;

        UUID playerUuid;
        try {
            playerUuid = UUID.fromString(punishment.getPlayerUuid());
        } catch (IllegalArgumentException e) {
            return;
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        RestrictionAction suspension = punishment.getActiveRestriction(PunishmentType.SUSPENSION);
        if (suspension != null) {
            sendSuspensionKick(player, suspension);
            return;
        }

        sendRestrictionNotice(player);
    }

    // ── Player messages ────────────────────────────────────────────────────────

    private void sendSuspensionKick(Player player, RestrictionAction suspension) {
        String reasonText = punishment.getReason() != null
                ? punishment.getReason().getDisplayName() : "Policy Violation";
        boolean permanent = suspension.getDuration() == -1L;
        long    duration  = suspension.getDuration();

        Component msg = MM.deserialize(
                "<dark_purple>Your account has been suspended"
                + "\n<gray>\"" + reasonText + "<gray>\""
                + "\n\n<gray>This suspension "
                + (permanent
                        ? "will never expire"
                        : "will expire in <light_purple>" + Time.formatDetailed(duration) + "<gray>")
                + ". Visit <light_purple><underlined>sparking.games/appeal<reset><gray> to submit an appeal"
        );

        player.kick(msg);
    }

    private void sendRestrictionNotice(Player player) {
        player.sendMessage(dashLine("Account Action"));
        player.sendMessage(MM.deserialize(" <gray>Your recent activity violated our Terms of Service"));

        if (punishment.getMessage() != null) {
            player.sendMessage(Component.empty());
            player.sendMessage(MM.deserialize(
                    "  <dark_gray><bold>→ </bold><dark_gray>[<gray>Member<dark_gray>]<gray> "
                    + player.getName() + " <dark_gray>»<white> " + punishment.getMessage()));
            player.sendMessage(Component.empty());
        }

        player.sendMessage(Component.empty());
        player.sendMessage(MM.deserialize(" <gray>We took these actions<dark_gray>:"));
        for (String line : buildActionLines()) {
            player.sendMessage(MM.deserialize("  <dark_red>✕ <red>" + line));
        }

        sendReasonSection(player);
        player.sendMessage(dashLine(null));
    }

    private List<String> buildActionLines() {
        List<String> lines = new ArrayList<>();
        if (punishment.getMessage() != null) {
            lines.add("This content has been removed so no one can see it.");
        }
        if (punishment.getActions() != null) {
            for (RestrictionAction action : punishment.getActions()) {
                lines.add(action.getType().getActionLine(action.getDuration()));
            }
        }
        return lines;
    }

    private void sendReasonSection(Player player) {
        player.sendMessage(Component.empty());

        if (punishment.getReason() == InfractionType.TEMP_AUTOMATED) {
            player.sendMessage(MM.deserialize(" <gray>Why this action was taken<dark_gray>:"));
            player.sendMessage(MM.deserialize("  <gray>This temporary action was triggered by our automated"));
            player.sendMessage(MM.deserialize("  <gray>moderation systems and is pending staff review."));
            player.sendMessage(Component.empty());
            player.sendMessage(MM.deserialize(" <gray>You cannot appeal this action at this time."));
            return;
        }

        String reasonDisplay = punishment.getReason() != null
                ? punishment.getReason().getDisplayName() : "Policy Violation";

        player.sendMessage(MM.deserialize(" <gray>Why we took these actions<dark_gray>:"));
        player.sendMessage(MM.deserialize("  <gray>Our trust and safety team believes you have violated"));
        player.sendMessage(MM.deserialize("  <gray>our community guidelines on <red>" + reasonDisplay + "<gray>."));
        player.sendMessage(Component.empty());
        player.sendMessage(MM.deserialize(" <gray>Please review our <aqua><underlined>Community Guidelines<gray>."));
        player.sendMessage(MM.deserialize(" <gray>Did we make a mistake? <aqua><underlined>Let us know<gray>!"));
    }

    // ── Line builder ───────────────────────────────────────────────────────────

    /**
     * Builds an alternating dark-gray / dark-red dash line.
     * If {@code label} is non-blank the label is centered between dash segments.
     */
    private static Component dashLine(String label) {
        if (label == null || label.isBlank()) {
            TextComponent.Builder b = Component.text();
            boolean alt = true;
            for (int i = 0; i < 48; i++) {
                b.append(Component.text("-", alt ? NamedTextColor.DARK_GRAY : NamedTextColor.DARK_RED));
                alt = !alt;
            }
            return b.build();
        }

        int dashes = Math.max(2, (48 - label.length() - 4) / 2);
        TextComponent.Builder b = Component.text();
        boolean alt = true;
        for (int i = 0; i < dashes; i++) {
            b.append(Component.text("-", alt ? NamedTextColor.DARK_GRAY : NamedTextColor.DARK_RED));
            alt = !alt;
        }
        b.append(Component.text("[", NamedTextColor.DARK_GRAY));
        b.append(Component.text(label, NamedTextColor.RED, TextDecoration.BOLD));
        b.append(Component.text("]", NamedTextColor.DARK_GRAY));
        for (int i = 0; i < dashes; i++) {
            b.append(Component.text("-", alt ? NamedTextColor.DARK_GRAY : NamedTextColor.DARK_RED));
            alt = !alt;
        }
        return b.build();
    }
}
