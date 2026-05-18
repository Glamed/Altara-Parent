package games.sparking.altara.game.module;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player kill and assist counts within a single game instance.
 *
 * <p>Kills are awarded to the last direct attacker. Assists are awarded to any player
 * who dealt damage within the last {@value #ASSIST_WINDOW_MS} ms, excluding the killer.
 *
 * <p><b>Session isolation:</b> all handlers guard with {@link #getGame()}{@code .hasPlayer()},
 * so events from other concurrent games are ignored.
 */
public class CombatTrackerModule extends GameModule {

    private static final long ASSIST_WINDOW_MS = 15_000L;

    /** victim UUID → { attacker UUID → last damage timestamp } */
    private final Map<UUID, Map<UUID, Long>> recentDamage = new HashMap<>();

    private final Map<UUID, CombatData> combatData = new HashMap<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Returns (or lazily creates) the {@link CombatData} for {@code player}. */
    public CombatData getCombatData(Player player) {
        return combatData.computeIfAbsent(player.getUniqueId(), k -> new CombatData());
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onDisable() {
        combatData.clear();
        recentDamage.clear();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!getGame().hasPlayer(victim)) return;

        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        if (!getGame().hasPlayer(attacker)) return;

        recentDamage
                .computeIfAbsent(victim.getUniqueId(), k -> new HashMap<>())
                .put(attacker.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!getGame().hasPlayer(victim)) return;

        // Kill credit
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && getGame().hasPlayer(killer)) {
            getCombatData(killer).incrementKills();
        }

        // Assist credits
        Map<UUID, Long> attackers = recentDamage.remove(victim.getUniqueId());
        if (attackers != null) {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : attackers.entrySet()) {
                if (now - entry.getValue() > ASSIST_WINDOW_MS) continue;
                if (killer != null && entry.getKey().equals(killer.getUniqueId())) continue;
                Player assist = Bukkit.getPlayer(entry.getKey());
                if (assist != null && getGame().hasPlayer(assist)) {
                    getCombatData(assist).incrementAssists();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player p) return p;
        }
        return null;
    }
}

