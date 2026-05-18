package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Slow Down</b>
 *
 * <p>While gliding, press Shift to rapidly decelerate. Cooldown: {@value COOLDOWN_MS}ms.
 */
public class PerkSlowDown extends Perk implements Listener {

    private static final long COOLDOWN_MS = 10_000;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public PerkSlowDown() {
        super("Slow Down", new String[]{
                "§7Press §eShift §7while gliding to decelerate.",
                "§7Cooldown: §a10s"
        });
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) return; // Only trigger on release
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!player.isGliding()) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < COOLDOWN_MS) return;
        cooldowns.put(player.getUniqueId(), now);

        // Cancel current velocity and apply a small forward push
        Vector dir = player.getLocation().getDirection().normalize();
        double speed = player.getVelocity().length();
        double decel = Math.max(0.2, speed * 0.3);
        player.setVelocity(dir.multiply(decel));

        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_BREATHE, 1f, 1.5f);
        player.sendMessage("§aSlow Down activated!");
    }

    @Override
    public void onUnregister() {
        cooldowns.clear();
    }
}

