package games.sparking.altara.game.event;

import games.sparking.altara.game.impl.Game;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a {@link Game} transitions to {@link games.sparking.altara.game.GameState#Live}.
 */
@Getter
@RequiredArgsConstructor
public class GameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

