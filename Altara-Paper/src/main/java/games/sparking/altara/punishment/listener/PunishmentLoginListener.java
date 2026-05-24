package games.sparking.altara.punishment.listener;

import games.sparking.altara.Altara;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentService;
import games.sparking.altara.punishment.RestrictionAction;
import games.sparking.altara.utils.Time;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Enforces active bans at the server gate.
 *
 * <ul>
 *   <li>{@link AsyncPlayerPreLoginEvent} — loads + checks punishments asynchronously
 *       before the player object is created, to avoid blocking the main thread.</li>
 *   <li>{@link PlayerLoginEvent} — secondary synchronous check (safety-net for
 *       punishments issued between pre-login and login).</li>
 *   <li>{@link PlayerQuitEvent} — evicts the punishment cache entry for this player
 *       so the next join always fetches fresh data.</li>
 * </ul>
 */
public class PunishmentLoginListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        PunishmentService svc = Altara.getSharedInstance().getPunishmentService();

        // Load punishments from Web API (or local cache if already populated)
        svc.loadPunishments(uuid, punishments -> {}, false);

        Punishment ban = svc.getActiveBan(uuid);
        if (ban != null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, buildBanMessage(ban));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PunishmentService svc = Altara.getSharedInstance().getPunishmentService();

        Punishment ban = svc.getActiveBan(uuid);
        if (ban != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, buildBanMessage(ban));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        // Evict cached punishments so the next login loads fresh data from the API
        Altara.getSharedInstance().getPunishmentService()
                .invalidateCache(event.getPlayer().getUniqueId());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static String buildBanMessage(Punishment ban) {
        RestrictionAction suspension = ban.getSuspensionAction();
        String reasonText = ban.getInfractionTypeEnum() != null
                ? ban.getInfractionTypeEnum().getDisplayName()
                : ban.getInfractionType();
        boolean permanent = suspension == null || suspension.getDuration() == -1L;

        String raw = "&5Your account has been suspended"
                + "\n&7\"" + reasonText + "&7\""
                + "\n\n&7This suspension " + (permanent
                    ? "will never expire"
                    : "will expire in &d" + (suspension != null
                        ? Time.formatDetailed(suspension.getDuration()) : "N/A") + "&7")
                + ". Visit &d&ncrystalwars.net/appeal&r&7 to submit an appeal";

        return ChatColor.translateAlternateColorCodes('&', raw);
    }
}

