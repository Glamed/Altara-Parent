package games.sparking.altara.game.event;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired just before a player is added to a {@link Game} instance.
 * <p>
 * Cancelling this event will prevent the player from joining the game.
 */
@Getter
@RequiredArgsConstructor
public class PlayerJoinGameEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final GamePlayer gamePlayer;
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

