package games.sparking.altara.profiler.command;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.profiler.ProfilerListener;
import games.sparking.altara.profiler.ProfilerRecord;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.profiler.packet.ProfilerBanPacket;
import games.sparking.altara.profiler.packet.ProfilerVerifyPacket;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishmentService;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Profiler commands available to staff (Mod+).
 *
 * <ul>
 *   <li>{@code /profiler}              — list all online flagged players.</li>
 *   <li>{@code /profilerban <name>}    — ban a flagged player for Compromised Account.</li>
 *   <li>{@code /profilerverify <name>} — clear a flagged player's shadow mute.</li>
 * </ul>
 *
 * <p>All three commands require the {@code altara.profiler} permission.
 * {@code /profilerban} additionally requires {@code altara.profiler.ban} (Mod+).
 */
public class ProfilerCommand {

    // ── /profiler ──────────────────────────────────────────────────────────────

    @Command(
            names       = {"profiler"},
            permission  = ProfilerService.PERMISSION,
            description = "List all online flagged (shadow-muted) players."
    )
    public void profiler(CommandSender sender) {
        ProfilerService svc = Altara.getSharedInstance().getProfilerService();
        List<ProfilerRecord> records = svc.getUnresolvedRecords();

        if (records.isEmpty()) {
            sender.sendMessage(CC.noticeMsg("Profiler", "No flagged players are currently online."));
            return;
        }

        sender.sendMessage(CC.format(
                "<gold><bold>[PROFILER] <yellow>Currently flagged online players <gray>(" + records.size() + ")<yellow>:"));

        for (ProfilerRecord record : records) {
            boolean online = Bukkit.getPlayer(record.getUuid()) != null;

            Component line = Component.text()
                    .append(Component.text("  » ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(record.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD)
                            .hoverEvent(HoverEvent.showText(buildHoverText(record))))
                    .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Score: ", NamedTextColor.GRAY))
                    .append(Component.text(record.getScore(), NamedTextColor.GOLD))
                    .append(Component.text("  Alts: ", NamedTextColor.GRAY))
                    .append(Component.text(record.getCompromisedAltCount(), altColor(record.getCompromisedAltCount())))
                    .append(Component.text(online ? "  [ONLINE]" : "  [OFFLINE]",
                            online ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.ITALIC))
                    .build();

            sender.sendMessage(line);
        }
    }

    // ── /profilerban <name> ────────────────────────────────────────────────────

    @Command(
            names       = {"profilerban"},
            permission  = "altara.profiler.ban",
            playerOnly  = true,
            description = "Ban a flagged player for Compromised Account.",
            async       = true
    )
    public void profilerBan(Player sender, @Param(name = "player") String targetName) {
        ProfilerService svc = Altara.getSharedInstance().getProfilerService();

        // Find record (player must be in the same server/lobby)
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(CC.errorMsg("Profiler", "That player is not online on this server."));
            return;
        }

        ProfilerRecord record = svc.getRecord(target.getUniqueId());
        if (record == null) {
            sender.sendMessage(CC.errorMsg("Profiler", targetName + " is not currently flagged by the profiler."));
            return;
        }

        UUID targetUuid = target.getUniqueId();
        int  altCount   = record.getCompromisedAltCount();

        // Build the punishment reason according to the guidelines
        String reason = buildBanReason(altCount);

        PunishmentService punSvc = Altara.getSharedInstance().getPunishmentService();
        punSvc.issuePunishment(
                sender.getUniqueId(),
                targetUuid,
                InfractionType.TEMP_AUTOMATED,
                List.of(RestrictionAction.permanent(PunishmentType.SUSPENSION)),
                reason,
                punishment -> {
                    if (punishment == null) {
                        sender.sendMessage(CC.errorMsg("Profiler", "Failed to issue punishment. Please try manually."));
                        return;
                    }

                    // Kick the player
                    Bukkit.getScheduler().runTask(
                            games.sparking.altara.AltaraPaper.getPlugin(),
                            () -> target.kick(CC.format(
                                    "<dark_purple>Your account has been suspended\n<gray>Compromised Account [Change Password & Appeal]"
                            ))
                    );

                    // Broadcast to the network
                    new ProfilerBanPacket(targetUuid.toString(), target.getName(), sender.getName()).publish();
                    sender.sendMessage(CC.successMsg("Profiler", "Successfully banned " + target.getName() + "."));
                },
                false
        );
    }

    // ── /profilerverify <name> ─────────────────────────────────────────────────

    @Command(
            names       = {"profilerverify"},
            permission  = ProfilerService.PERMISSION,
            playerOnly  = true,
            description = "Verify a flagged player and remove their shadow mute.",
            async       = false
    )
    public void profilerVerify(Player sender, @Param(name = "player") String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(CC.errorMsg("Profiler", "That player is not online on this server."));
            return;
        }

        ProfilerService svc = Altara.getSharedInstance().getProfilerService();
        ProfilerRecord record = svc.getRecord(target.getUniqueId());
        if (record == null) {
            sender.sendMessage(CC.errorMsg("Profiler", targetName + " is not currently flagged by the profiler."));
            return;
        }

        // Broadcast verification to the network (marks verified on all servers)
        new ProfilerVerifyPacket(target.getUniqueId().toString(), target.getName(), sender.getName()).publish();

        // Clear the shadow-mute channel on this server immediately
        ProfilerListener.clearShadowMuteChannel(target);

        sender.sendMessage(CC.successMsg("Profiler", "Verified " + target.getName() + ". Shadow mute removed."));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String buildBanReason(int altCount) {
        String suffix = altCount >= 300 ? " [300+]" : "";
        return "Compromised Account [Change Password & Appeal]" + suffix;
    }

    private static Component buildHoverText(ProfilerRecord record) {
        return Component.text()
                .append(Component.text("Compromised alt accounts: ", NamedTextColor.GRAY))
                .append(Component.text(record.getCompromisedAltCount(), altColor(record.getCompromisedAltCount()), TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Score: ", NamedTextColor.GRAY))
                .append(Component.text(record.getScore(), NamedTextColor.GOLD))
                .append(Component.newline())
                .append(Component.text("Verified: ", NamedTextColor.GRAY))
                .append(Component.text(record.isVerified() ? "Yes" : "No",
                        record.isVerified() ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build();
    }

    private static NamedTextColor altColor(int count) {
        if (count >= 300) return NamedTextColor.RED;
        if (count >= 150) return NamedTextColor.GOLD;
        return NamedTextColor.GREEN;
    }
}

