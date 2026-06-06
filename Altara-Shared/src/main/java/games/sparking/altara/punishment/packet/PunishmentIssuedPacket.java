package games.sparking.altara.punishment.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.Time;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Published by <b>Altara-Web</b> after persisting a new punishment to MongoDB.
 *
 * <ul>
 *   <li>Paper servers receive it and apply the punishment effect to the online player.</li>
 *   <li>The Proxy can intercept for pre-login ban-kicks (not implemented here – handled at login).</li>
 *   <li>The Web server itself ignores it (it already holds the source of truth).</li>
 * </ul>
 */
@AllArgsConstructor
@NoArgsConstructor
public class PunishmentIssuedPacket extends Packet {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Punishment punishment;

    @Override
    public void receive() {
        // Update the local punishment cache on every node except the source (WEB)
        if (Altara.getSystemType() != SystemType.WEB) {
            Altara.getSharedInstance().getPunishmentService().updateCacheFromPacket(punishment);
        }

        if (Altara.getSystemType() != SystemType.PAPER) return;

        Player player = Bukkit.getPlayer(UUID.fromString(punishment.getPlayerUuid()));
        if (player == null) return; // player not on this server — another node will handle it

        RestrictionAction suspension = punishment.getSuspensionAction();
        if (suspension != null && !suspension.hasExpired(punishment.getIssuedAt())) {
            handleBan(player, suspension);
        } else {
            handleNoticeActions(player);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void handleBan(Player player, RestrictionAction suspension) {
        String reasonText = punishment.getInfractionTypeEnum() != null
                ? punishment.getInfractionTypeEnum().getDisplayName()
                : punishment.getInfractionType();
        boolean permanent = suspension.getDuration() == -1L;

        String banMessage = "<red>Your account has been suspended"
                + "\n<gray>\"" + reasonText + "<gray>\""
                + "\n\n<gray>This suspension " + (permanent
                    ? "will never expire"
                    : "will expire in <red>" + Time.formatDetailed(suspension.getDuration()) + "<gray>")
                + ". Visit <red><underlined>" + Altara.getSharedInstance().getMainConfig().getServerConfig().getWebsite() + "/appeal<reset><gray> to submit an appeal";

        player.kick(MM.deserialize(banMessage));
    }

    private void handleNoticeActions(Player player) {
        sendHeader(player);
        player.sendMessage(f(" <gray>Your recent activity violated our Terms of Service"));

        String msg = punishment.getMessage();
        if (msg != null && !msg.isBlank()) {
            player.sendMessage(Component.empty());
            player.sendMessage(f("  <dark_gray><bold>→ <dark_gray>[<gray>Member<dark_gray>]<gray> " + player.getName() + " <dark_gray>»<white> " + msg));
        }

        player.sendMessage(Component.empty());
        player.sendMessage(f(" <gray>We took these actions<dark_gray>:"));

        for (RestrictionAction action : punishment.getActions()) {
            player.sendMessage(f("  <dark_red>x<red> " + action.getType().getActionLine(action.getDuration())));
        }
        if (msg != null && !msg.isBlank()) {
            player.sendMessage(f("  <dark_red>x<red> This content has been removed so no one can see it."));
        }

        sendReasonSection(player);
        sendHeader(player);
    }

    private void sendHeader(Player player) {
        player.sendMessage(MM.deserialize("<dark_purple><strikethrough>" + "─".repeat(48)));
    }

    private void sendReasonSection(Player player) {
        player.sendMessage(Component.empty());
        var infraction = punishment.getInfractionTypeEnum();
        if (infraction == games.sparking.altara.punishment.InfractionType.TEMP_AUTOMATED) {
            player.sendMessage(f(" <gray>Why this action was taken<dark_gray>:"));
            player.sendMessage(f("  <gray>This temporary action was triggered by our"));
            player.sendMessage(f("  <gray>automated moderation systems and is pending"));
            player.sendMessage(f("  <gray>manual review by our Trust & Safety team."));
            player.sendMessage(Component.empty());
            player.sendMessage(f(" <gray>You cannot appeal this action at this time."));
        } else {
            String name = infraction != null ? infraction.getDisplayName() : punishment.getInfractionType();
            player.sendMessage(f(" <gray>Why we took these actions<dark_gray>:"));
            player.sendMessage(f("  <gray>Our trust and safety team uses automation and manual"));
            player.sendMessage(f("  <gray>review to enforce our rules. We believe that you have"));
            player.sendMessage(f("  <gray>violated our community guidelines on <light_purple>" + name + "<gray>."));
            player.sendMessage(Component.empty());
            player.sendMessage(f(" <gray>Please review our <aqua><underlined>Community Guidelines<gray>."));
            player.sendMessage(f(" <gray>Did we make a mistake? <aqua><underlined>Let us know<gray>!"));
        }
    }

    private static Component f(String s) {
        return MM.deserialize(s);
    }
}
