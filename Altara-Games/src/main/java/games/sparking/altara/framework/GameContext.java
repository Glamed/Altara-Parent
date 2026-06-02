package games.sparking.altara.framework;

import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Immutable context object injected into every @GameEvent handler call.
 * Carries the three pieces of information every handler needs, avoiding
 * repeated lookups inside the handler body.
 */
@Getter
public final class GameContext {

    private final Player player;
    private final Game game;
    private final GameState state;

    public GameContext(Player player, Game game, GameState state) {
        this.player = player;
        this.game = game;
        this.state = state;
    }
}

