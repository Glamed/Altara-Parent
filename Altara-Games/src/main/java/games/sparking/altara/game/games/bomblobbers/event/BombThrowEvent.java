package games.sparking.altara.game.games.bomblobbers.event;

import games.sparking.altara.game.games.bomblobbers.BombLobbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player throws a bomb (TNT) in a {@link BombLobbers} game.
 */
@Getter
@RequiredArgsConstructor
public class BombThrowEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BombLobbers game;
    private final Player thrower;
    private final TNTPrimed tnt;

    @Override
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

