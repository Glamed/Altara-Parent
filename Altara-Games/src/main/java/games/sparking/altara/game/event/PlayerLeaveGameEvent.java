package games.sparking.altara.game.event;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired after a player has been removed from a {@link Game} instance.
 */
@Getter
@RequiredArgsConstructor
public class PlayerLeaveGameEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GamePlayer gamePlayer;

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

