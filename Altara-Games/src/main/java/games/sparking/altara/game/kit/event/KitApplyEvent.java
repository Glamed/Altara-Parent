package games.sparking.altara.game.kit.event;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.Kit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired just before a {@link Kit} is applied to a {@link Player}.
 * Cancelling prevents the apply, but the kit selection remains unchanged.
 */
@Getter
@RequiredArgsConstructor
public class KitApplyEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Game game;
    private final Kit kit;
    private final Player player;

    @Setter private boolean cancelled;

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}

