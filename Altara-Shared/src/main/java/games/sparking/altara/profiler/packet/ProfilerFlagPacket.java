package games.sparking.altara.profiler.packet;

import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.profiler.ProfilerRecord;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Theme;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Published by the server that flagged an account (the record is already saved in
 * Redis).  Every Paper server caches the flag if the player is online there and
 * alerts its online staff.
 */
@AllArgsConstructor
@NoArgsConstructor
public class ProfilerFlagPacket extends Packet {

    private ProfilerRecord record;

    @Override
    public void receive() {
        if (Altara.getSystemType() != SystemType.PAPER || record == null) return;

        if (Bukkit.getPlayer(record.getUuid()) != null) {
            Altara.getSharedInstance().getProfilerService().cache(record);
        }

        Component alert = CC.notice(
                Component.text("Profiler flag."),
                Component.text()
                        .append(Component.text(record.getName(), Theme.TEXT_STRONG))
                        .append(Component.text(" may be using a compromised account. ", Theme.TEXT))
                        .append(CC.action("View the reasons for this flag.",
                                ClickEvent.runCommand("/profiler"))
                                .hoverEvent(HoverEvent.showText(buildHover(record)))));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(ProfilerService.PERMISSION)) staff.sendMessage(alert);
        }
        Bukkit.getConsoleSender().sendMessage("[Profiler] " + record.getName() + " flagged (score="
                + record.getScore() + "): " + String.join("; ", record.getReasons()));
    }

    /** Hover text listing the score and every reason — shared with /profiler. */
    public static Component buildHover(ProfilerRecord record) {
        TextComponent.Builder hover = Component.text()
                .append(Component.text(record.getName(), Theme.PRIMARY))
                .append(Component.newline())
                .append(Component.text("- Score: ", Theme.TEXT))
                .append(Component.text(record.getScore() + "/" + ProfilerService.FLAG_THRESHOLD, Theme.TEXT_STRONG))
                .append(Component.newline())
                .append(Component.text("- Banned linked accounts: ", Theme.TEXT))
                .append(Component.text(record.getBannedAltCount(),
                        record.getBannedAltCount() > 0 ? Theme.ERROR : Theme.TEXT_STRONG))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("> ", Theme.PRIMARY_DARK))
                .append(Component.text("Reasons:", Theme.TEXT_STRONG));

        for (String reason : record.getReasons()) {
            hover.append(Component.newline())
                    .append(Component.text("  - ", Theme.PRIMARY))
                    .append(Component.text(reason, Theme.TEXT));
        }

        return hover.append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Use /profilerverify " + record.getName() + " to clear it.", Theme.TEXT))
                .build();
    }
}
