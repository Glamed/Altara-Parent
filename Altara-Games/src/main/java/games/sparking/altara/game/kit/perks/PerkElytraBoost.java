package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Elytra Boost</b>
 *
 * <p>While gliding, toggling flight (double-tap jump) provides a burst of speed.
 * Cooldown: {@value COOLDOWN_MS}ms.
 */
public class PerkElytraBoost extends Perk implements Listener {

    private static final long COOLDOWN_MS = 30_000;
    private static final double BOOST_MULTIPLIER = 2.5;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkElytraBoost() {
        super("Elytra Boost", new String[]{
                "§7§nDouble-tap jump§r §7while gliding for a §aspeed burst§7.",
                "§7Cooldown: §a30s"
        });
    }

    @Override
    public void apply(Player player) {
        // Enable flight so double-tap works while gliding
        if (player.isGliding()) player.setAllowFlight(true);
    }

    @EventHandler
    public void onBoost(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!player.isGliding()) return;
        if (!event.isFlying()) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) {
            long rem = (COOLDOWN_MS - (now - cooldowns.getOrDefault(player.getUniqueId(), 0L))) / 1000;
            player.sendMessage("§cElytra Boost on cooldown for §e" + rem + "s§c.");
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);
        cooldowns.put(player.getUniqueId(), now);

        // Boost!
        Vector dir = player.getLocation().getDirection().normalize();
        player.setVelocity(dir.multiply(BOOST_MULTIPLIER));
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
        player.sendMessage("§aElytra Boost activated!");

        // Particle trail
        new BukkitRunnable() {
            int t = 0;
            @Override
            public void run() {
                if (t++ > 20) { cancel(); return; }
                if (!player.isGliding()) { cancel(); return; }
                player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }.runTaskTimer(games.sparking.altara.AltaraPaper.getPlugin(), 0L, 1L);
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
    }
}

