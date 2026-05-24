package games.sparking.altara.punishment.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.Time;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        String banMessage = "&cYour account has been suspended"
                + "\n&7\"" + reasonText + "&7\""
                + "\n\n&7This suspension " + (permanent
                    ? "will never expire"
                    : "will expire in &c" + Time.formatDetailed(suspension.getDuration()) + "&7")
                + ". Visit &c&n" + Altara.getSharedInstance().getMainConfig().getServerConfig().getWebsite() + "/appeal&r&7 to submit an appeal";

        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', banMessage));
    }

    private void handleNoticeActions(Player player) {
        sendHeader(player);
        player.sendMessage(f(" &7Your recent activity violated our Terms of Service"));

        String msg = punishment.getMessage();
        if (msg != null && !msg.isBlank()) {
            player.sendMessage("");
            player.sendMessage(f("  &8&l→ &8[&7Member&8]&7 " + player.getName() + " &8»&f " + msg));
        }

        player.sendMessage("");
        player.sendMessage(f(" &7We took these actions&8:"));

        for (RestrictionAction action : punishment.getActions()) {
            player.sendMessage(f("  &4x&c " + action.getType().getActionLine(action.getDuration())));
        }
        if (msg != null && !msg.isBlank()) {
            player.sendMessage(f("  &4x&c This content has been removed so no one can see it."));
        }

        sendReasonSection(player);
        sendHeader(player);
    }

    private void sendHeader(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&5&m" + "─".repeat(48)));
    }

    private void sendReasonSection(Player player) {
        player.sendMessage("");
        var infraction = punishment.getInfractionTypeEnum();
        if (infraction == games.sparking.altara.punishment.InfractionType.TEMP_AUTOMATED) {
            player.sendMessage(f(" &7Why this action was taken&8:"));
            player.sendMessage(f("  &7This temporary action was triggered by our"));
            player.sendMessage(f("  &7automated moderation systems and is pending"));
            player.sendMessage(f("  &7manual review by our Trust & Safety team."));
            player.sendMessage("");
            player.sendMessage(f(" &7You cannot appeal this action at this time."));
        } else {
            String name = infraction != null ? infraction.getDisplayName() : punishment.getInfractionType();
            player.sendMessage(f(" &7Why we took these actions&8:"));
            player.sendMessage(f("  &7Our trust and safety team uses automation and manual"));
            player.sendMessage(f("  &7review to enforce our rules. We believe that you have"));
            player.sendMessage(f("  &7violated our community guidelines on &d" + name + "&7."));
            player.sendMessage("");
            player.sendMessage(f(" &7Please review our &b&nCommunity Guidelines&7."));
            player.sendMessage(f(" &7Did we make a mistake? &b&nLet us know&7!"));
        }
    }

    private static String f(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}

