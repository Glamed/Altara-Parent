package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Effect;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Flash</b>
 *
 * <p>Sneak to activate a short forward dash with a speed burst.
 */
public class PerkFlash extends Perk implements Listener {

    private static final long COOLDOWN_MS = 8000L;
    private static final double VELOCITY = 1.4;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkFlash() {
        super("Flash", new String[]{
                "§7Sneak to dash forward at high speed.",
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

        Vector dir = player.getLocation().getDirection().setY(0).normalize().multiply(VELOCITY);
        player.setVelocity(dir);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, 3, false, false));
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 5, 0.3, 0.3, 0.3, 0);
    }
}



