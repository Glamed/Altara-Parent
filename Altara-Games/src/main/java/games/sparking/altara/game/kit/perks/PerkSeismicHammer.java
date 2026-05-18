package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
 * <b>Seismic Hammer</b>
 *
 * <p>Sneak to smash the ground, sending a shockwave that knocks back and damages nearby enemies.
 */
public class PerkSeismicHammer extends Perk implements Listener {

    private static final long COOLDOWN_MS = 10000L;
    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 5.0;
    private static final double KNOCKBACK = 1.2;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkSeismicHammer() {
        super("Seismic Hammer", new String[]{
                "§7Sneak to unleash a §cseismic shockwave§7.",
                "§7Knocks back and damages enemies within §a" + (int) RADIUS + " blocks§7.",
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

        // Shockwave particles
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 12) {
            for (double r = 1; r <= RADIUS; r += 1) {
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                loc.getWorld().spawnParticle(Particle.BLOCK,
                        loc.clone().add(x, 0.1, z), 3,
                        org.bukkit.Material.STONE.createBlockData());
            }
        }
        loc.getWorld().playSound(loc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.5f, 0.5f);
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);

        // Knock back and damage nearby players
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, RADIUS, RADIUS, RADIUS)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(player)) continue;
            if (!getGame().hasPlayer(target)) continue;

            Vector dir = target.getLocation().subtract(loc).toVector().setY(0.4).normalize().multiply(KNOCKBACK);
            target.setVelocity(dir);
            target.damage(DAMAGE, player);
        }
    }
}

