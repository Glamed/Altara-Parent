package games.sparking.altara.game.module.generator;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Fired when a player walks over a {@link Generator} and collects its item.
 */
public class GeneratorCollectEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Generator generator;

    public GeneratorCollectEvent(Player who, Generator generator) {
        super(who);
        this.generator = generator;
    }

    /** The generator that was collected from. */
    public Generator getGenerator() { return generator; }

    @Override public HandlerList getHandlers()    { return HANDLERS; }
    public static HandlerList    getHandlerList() { return HANDLERS; }
}

