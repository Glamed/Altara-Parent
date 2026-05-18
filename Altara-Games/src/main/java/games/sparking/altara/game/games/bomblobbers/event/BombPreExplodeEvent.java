package games.sparking.altara.game.games.bomblobbers.event;

import games.sparking.altara.game.games.bomblobbers.BombLobbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired just before a tracked bomb is allowed to explode in a {@link BombLobbers} game.
 * <p>
 * Cancelling this event will suppress the explosion (the TNT entity will be removed
 * silently).  Kit implementations may listen here to intercept enemy bombs.
 */
@Getter
@RequiredArgsConstructor
public class BombPreExplodeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BombLobbers game;
    private final Player thrower;
    private final TNTPrimed tnt;
    private final Location explosionPoint;

    @Setter private boolean cancelled;

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

