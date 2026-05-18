package games.sparking.altara.game.event;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.GameState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired just before a {@link Game} transitions from one {@link GameState} to another.
 * <p>
 * Cancelling this event will prevent the state change from occurring.
 */
@Getter
@RequiredArgsConstructor
public class GameStateChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GameState oldState;
    private final GameState newState;
    private boolean cancelled;

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

