package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Triple Jump</b>
 *
 * <p>The player can jump up to 3 times mid-air. Flight is re-enabled when they land.
 */
public class PerkTripleJump extends Perk implements Listener {

    private final double power;
    private final double heightMax;

    /** Remaining jumps in the current air-time. */
    private final Map<UUID, Integer> jumpsLeft = new ConcurrentHashMap<>();

    public PerkTripleJump(double power, double heightMax) {
        super("Triple Jump", new String[]{"§eTap Jump §7up to §a3 times §7mid-air."});
        this.power = power;
        this.heightMax = heightMax;
    }

    @Override
    public void apply(Player player) {
        player.setAllowFlight(true);
        player.setFlying(false);
        jumpsLeft.put(player.getUniqueId(), 3);
    }

    @Override
    public void remove(Player player) {
        jumpsLeft.remove(player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @EventHandler
    public void onFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        int left = jumpsLeft.getOrDefault(player.getUniqueId(), 0);
        if (left <= 0) return;

        event.setCancelled(true);
        player.setFlying(false);
        jumpsLeft.put(player.getUniqueId(), left - 1);
        if (left - 1 <= 0) player.setAllowFlight(false);

        Vector dir = player.getLocation().getDirection().normalize().multiply(power);
        dir.setY(Math.min(Math.max(dir.getY(), power * 0.5), heightMax));
        player.setVelocity(dir);
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
    }

    @EventHandler
    public void onLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!player.isOnGround()) return;
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        jumpsLeft.put(player.getUniqueId(), 3);
    }

    @Override
    public void onUnregister() { jumpsLeft.clear(); }
}

