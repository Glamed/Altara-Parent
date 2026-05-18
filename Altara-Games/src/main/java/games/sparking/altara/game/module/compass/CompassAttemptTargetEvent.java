package games.sparking.altara.game.module.compass;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired just before the {@link CompassModule} assigns a compass target for a player.
 * Cancel this event to prevent a specific entity from being targeted.
 */
public class CompassAttemptTargetEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity target;
    private boolean cancelled;

    CompassAttemptTargetEvent(Player player, Entity target) {
        super(player);
        this.target = target;
    }

    /** The entity the compass module is about to target. */
    public Entity getTarget() { return target; }

    @Override public boolean     isCancelled()               { return cancelled;      }
    @Override public void        setCancelled(boolean value) { this.cancelled = value; }
    @Override public HandlerList getHandlers()               { return HANDLERS;       }
    public  static HandlerList   getHandlerList()            { return HANDLERS;       }
}

