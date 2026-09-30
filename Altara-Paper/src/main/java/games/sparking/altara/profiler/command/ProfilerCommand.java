package games.sparking.altara.profiler.command;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.profiler.ProfilerRecord;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.profiler.packet.ProfilerBanPacket;
import games.sparking.altara.profiler.packet.ProfilerFlagPacket;
import games.sparking.altara.profiler.packet.ProfilerVerifyPacket;
import games.sparking.altara.punishment.InfractionType;
import games.sparking.altara.punishment.PunishmentNotifier;
import games.sparking.altara.punishment.PunishmentService;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.uuid.UUIDCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Profiler commands (staff).  Flags are network-wide, so targets don't need to be on
 * the same server.
 *
 * <ul>
 *   <li>{@code /profiler}              — list all flagged accounts on the network.</li>
 *   <li>{@code /profilerverify <name>} — clear a flag and trust the account for 30 days.</li>
 *   <li>{@code /profilerban <name>}    — ban a flagged account for Compromised Account.</li>
 * </ul>
 */
public class ProfilerCommand {

    @Command(
            names       = {"profiler"},
            permission  = ProfilerService.PERMISSION,
            description = "View flagged accounts",
            async       = true
    )
    public void profiler(CommandSender sender) {
        List<ProfilerRecord> records = Altara.getSharedInstance().getProfilerService().getAllFlags();

        sender.sendMessage(CC.header(Panel.STAFF, "Profiler", records.size() + " flagged"));
        if (records.isEmpty()) {
            sender.sendMessage(CC.empty("No accounts are currently flagged."));
        }

        for (ProfilerRecord record : records) {
            boolean online = Bukkit.getPlayer(record.getUuid()) != null;
            long ago = System.currentTimeMillis() - record.getFlaggedAt();

            Component line = Component.text()
                    .append(CC.statusDot(online))
                    .append(Component.space())
                    .append(Component.text(record.getName(), Theme.TEXT_STRONG)
                            .hoverEvent(HoverEvent.showText(ProfilerFlagPacket.buildHover(record)))
                            .clickEvent(ClickEvent.suggestCommand("/profilerverify " + record.getName())))
                    .append(Component.text(" - score ", Theme.TEXT))
                    .append(Component.text(record.getScore(), Theme.TEXT_STRONG))
                    .append(Component.text(", flagged " + formatAgo(ago) + " ago", Theme.TEXT))
                    .build();

            sender.sendMessage(CC.item(line));
        }
        sender.sendMessage(CC.footer(Panel.STAFF));
    }

    @Command(
            names       = {"profilerverify"},
            permission  = ProfilerService.PERMISSION,
            description = "Clear a flag and trust the account for 30 days",
            async       = true
    )
    public void profilerVerify(CommandSender sender, @Param(name = "player") String targetName) {
        ProfilerService svc = Altara.getSharedInstance().getProfilerService();
        UUID uuid = resolve(targetName);
        ProfilerRecord record = uuid != null ? svc.loadFlag(uuid) : null;
        if (record == null) {
            sender.sendMessage(CC.error("Not flagged.", "*" + targetName + "* isn't flagged by the profiler."));
            return;
        }

        svc.verify(uuid);
        new ProfilerVerifyPacket(uuid.toString(), record.getName(), sender.getName()).publish();
        sender.sendMessage(CC.success("Account verified.", "*" + record.getName() + "* is no longer shadow-muted."));
    }

    @Command(
            names       = {"profilerban"},
            permission  = "altara.profiler.ban",
            playerOnly  = true,
            description = "Suspend a flagged account as compromised",
            async       = true
    )
    public void profilerBan(Player sender, @Param(name = "player") String targetName) {
        ProfilerService svc = Altara.getSharedInstance().getProfilerService();
        UUID uuid = resolve(targetName);
        ProfilerRecord record = uuid != null ? svc.loadFlag(uuid) : null;
        if (record == null) {
            sender.sendMessage(CC.error("Not flagged.", "*" + targetName + "* isn't flagged by the profiler."));
            return;
        }

        PunishmentService punSvc = Altara.getSharedInstance().getPunishmentService();
        punSvc.issuePunishment(
                sender.getUniqueId(),
                uuid,
                InfractionType.TEMP_AUTOMATED,
                List.of(RestrictionAction.permanent(PunishmentType.SUSPENSION)),
                "Compromised Account [Change Password & Appeal]",
                punishment -> {
                    if (punishment == null) {
                        sender.sendMessage(CC.error("Unable to suspend.", "The punishment couldn't be issued — try */punish* instead."));
                        return;
                    }

                    svc.deleteFlag(uuid);
                    svc.uncache(uuid);

                    Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(), () -> {
                        Player target = Bukkit.getPlayer(uuid);
                        if (target != null) PunishmentNotifier.deliver(target, punishment);
                    });

                    new ProfilerBanPacket(uuid.toString(), record.getName(), sender.getName()).publish();
                    sender.sendMessage(CC.success("Account suspended.", "*" + record.getName() + "* has been suspended."));
                },
                false
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static UUID resolve(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online.getUniqueId() : UUIDCache.getUuid(name);
    }

    private static String formatAgo(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        return (hours / 24) + "d";
    }
}
