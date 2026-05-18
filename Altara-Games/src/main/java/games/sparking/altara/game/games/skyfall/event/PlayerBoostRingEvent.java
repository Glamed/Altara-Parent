package games.sparking.altara.game.games.skyfall.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a player flies through a booster ring in {@link games.sparking.altara.game.games.skyfall.Skyfall}.
 *
 * <p>Listeners (e.g. {@link games.sparking.altara.game.games.skyfall.perks.PerkIncreaseBoosters})
 * may modify the boost strength before it is applied.
 */
public class PlayerBoostRingEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private boolean cancelled;
    private double strength;

    public PlayerBoostRingEvent(Player player, double strength) {
        super(player);
        this.strength = strength;
    }

    public double getStrength() { return strength; }
    public void setStrength(double s)     { this.strength = s; }
    public void multiplyStrength(double m){ this.strength *= m; }

    @Override public boolean isCancelled()         { return cancelled; }
    @Override public void    setCancelled(boolean c){ this.cancelled = c; }
    @Override public HandlerList getHandlers()      { return HANDLERS; }
    public static HandlerList getHandlerList()      { return HANDLERS; }
}

