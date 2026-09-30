package games.sparking.altara.report;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Keeps a short rolling window of recent chat on this server so a {@code /chatreport} can
 * attach real context instead of just the one message the reporter clicked on.
 *
 * <p>Registered at {@link EventPriority#LOW}, <em>before</em> {@code ChatListener}'s
 * {@code HIGHEST} handler cancels the vanilla event for the custom channel system — this
 * only ever reads the message, so running early keeps it simple and channel-agnostic.
 */
public class ReportChatCache implements Listener {

    private static final long WINDOW_MS = 3 * 60 * 1000L;
    private static final int MAX_ENTRIES = 500;

    private static final List<ReportMessage> RECENT = new CopyOnWriteArrayList<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        String text = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).trim();
        if (text.isEmpty()) return;

        List<String> recipients = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            recipients.add(player.getUniqueId().toString());
        }

        RECENT.add(new ReportMessage(sender.getUniqueId().toString(), System.currentTimeMillis(), text, recipients));
        trim();
    }

    private static void trim() {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        RECENT.removeIf(message -> message.getSentAt() < cutoff);
        while (RECENT.size() > MAX_ENTRIES) {
            RECENT.remove(0);
        }
    }

    /** Recent messages this server has seen that {@code viewerUuid} would have received. */
    public static List<ReportMessage> recentlySeenBy(String viewerUuid) {
        trim();
        List<ReportMessage> result = new ArrayList<>();
        for (ReportMessage message : RECENT) {
            if (message.getRecipients().contains(viewerUuid)) result.add(message);
        }
        return result;
    }
}
