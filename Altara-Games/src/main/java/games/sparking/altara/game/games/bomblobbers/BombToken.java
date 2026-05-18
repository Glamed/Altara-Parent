package games.sparking.altara.game.games.bomblobbers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Tracks the state of a single in-flight {@link org.bukkit.entity.TNTPrimed} entity.
 * One token is created per thrown bomb and removed when the explosion resolves.
 */
public class BombToken {

    /** UUID of the player who threw this bomb. */
    public final UUID thrower;

    /** System time (ms) when the bomb was thrown. */
    public final long created;

    /**
     * When {@code true} the bomb is considered "primed" – on next block contact
     * (or after the hard cap) it will be instantly detonated.
     */
    public boolean primed = false;

    /**
     * {@code true} once a direct-hit has already been registered for this bomb,
     * preventing multiple direct-hit events from the same projectile.
     */
    public boolean directHit = false;

    // Previous and current positions, used to draw the particle trail.
    public Location prevLoc = null;
    public Location currLoc = null;

    public BombToken(Player player) {
        this.thrower = player.getUniqueId();
        this.created = System.currentTimeMillis();
    }

    /** Milliseconds elapsed since the bomb was thrown. */
    public long age() {
        return System.currentTimeMillis() - created;
    }
}

