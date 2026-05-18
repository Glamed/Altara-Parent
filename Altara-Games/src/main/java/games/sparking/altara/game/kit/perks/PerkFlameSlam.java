package games.sparking.altara.game.kit.perks;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.kit.Perk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Flame Slam</b>
 *
 * <p>Sneak to slam to the ground, creating a ring of fire that damages nearby enemies.
 */
public class PerkFlameSlam extends Perk implements Listener {

    private static final long COOLDOWN_MS = 12000L;
    private static final double RADIUS = 4.0;
    private static final double DAMAGE = 4.0;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkFlameSlam() {
        super("Flame Slam", new String[]{
                "§7Sneak to slam the ground, creating a §cring of fire§7.",
                "§7Deals §c" + DAMAGE + " damage§7 to nearby enemies.",
                "§7Cooldown: §a" + (COOLDOWN_MS / 1000) + "s§7."
        });
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        Location loc = player.getLocation();

        // Visual ring
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
            double x = Math.cos(angle) * RADIUS;
            double z = Math.sin(angle) * RADIUS;
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, 0.1, z), 3, 0, 0, 0, 0);
        }

        // Set nearby enemies on fire and damage them
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;
            if (!getGame().hasPlayer(target)) continue;
            target.damage(DAMAGE, player);
            target.setFireTicks(60);
        }
    }
}

