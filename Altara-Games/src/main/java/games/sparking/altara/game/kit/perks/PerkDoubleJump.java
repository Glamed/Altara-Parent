package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Double Jump</b>
 *
 * <p>When a player presses jump a second time mid-air, this perk cancels the flight
 * toggle, applies an upward (and optionally forward) velocity boost, and revokes the
 * {@code allowFlight} flag.  Flight is restored once the player lands again
 * (detected via {@link PlayerMoveEvent}).
 *
 * <h2>Parameters</h2>
 * <ul>
 *   <li>{@code power} – base velocity magnitude applied on double-jump (e.g. {@code 0.65})</li>
 *   <li>{@code heightCap} – maximum Y velocity component so the player does not fly too high
 *       (e.g. {@code 1.2}; set to {@code 0} to disable capping)</li>
 * </ul>
 */
public class PerkDoubleJump extends Perk implements Listener {

    private final double power;
    private final double heightCap;

    /** Players whose flight has been consumed mid-air (cannot double-jump again until landing). */
    private final Set<UUID> _usedJump = ConcurrentHashMap.newKeySet();

    public PerkDoubleJump(double power, double heightCap) {
        super("Double Jump", new String[]{
                "§7Tap §eJump §7twice to launch upward."
        });
        this.power = power;
        this.heightCap = heightCap;
    }

    @Override
    public void apply(Player player) {
        player.setAllowFlight(true);
        player.setFlying(false);
        _usedJump.remove(player.getUniqueId());
    }

    @Override
    public void remove(Player player) {
        _usedJump.remove(player.getUniqueId());
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    @Override
    public void onUnregister() {
        _usedJump.clear();
    }

    // =========================================================================
    // Double-jump trigger
    // =========================================================================

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!event.isFlying()) return; // isFlying = true → player pressed jump mid-air

        // Creative/Spectator: let Bukkit handle it.
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE
                || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;

        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);
        _usedJump.add(player.getUniqueId());

        // Apply the velocity boost.
        Vector dir = player.getLocation().getDirection().normalize();
        Vector vel = dir.multiply(power);
        if (heightCap > 0) vel.setY(Math.min(vel.getY(), heightCap));
        // Ensure a forward push even when looking straight down.
        vel.setY(Math.max(vel.getY(), power * 0.5));
        player.setVelocity(vel);

        // Jump sound.
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.5f);
    }

    // =========================================================================
    // Restore flight on landing
    // =========================================================================

    @EventHandler
    public void onLand(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;
        if (!_usedJump.contains(player.getUniqueId())) return;
        if (!player.isOnGround()) return;

        _usedJump.remove(player.getUniqueId());
        // Only restore if not already in flight mode.
        if (!player.getAllowFlight()
                && player.getGameMode() != org.bukkit.GameMode.CREATIVE
                && player.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
            player.setAllowFlight(true);
        }
    }
}

