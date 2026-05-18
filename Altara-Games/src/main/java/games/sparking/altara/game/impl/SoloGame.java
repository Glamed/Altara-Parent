package games.sparking.altara.game.impl;

import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Abstract base for free-for-all (solo) game modes.
 */
public abstract class SoloGame extends Game {

    protected abstract void onWinnerDecided(@Nullable GamePlayer winner);

    /**
     * Safety-net win check every second via UpdateEvent.
     * Immediate checks happen via {@link #onPlayerEliminated(GamePlayer)}.
     */
    @EventHandler
    public void onUpdateSec(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC || !isLive()) return;
        checkWinCondition();
    }

    /**
     * Immediately checks the win condition when a player is eliminated.
     * Override and call {@code super.onPlayerEliminated(gp)} to add a broadcast.
     */
    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        checkWinCondition();
    }

    protected final void checkWinCondition() {
        if (!isLive()) return;

        List<GamePlayer> alive = getPlayers().values().stream()
                .filter(GamePlayer::isAlive)
                .toList();

        if (alive.size() <= 1) {
            onWinnerDecided(alive.isEmpty() ? null : alive.getFirst());
            endGame();
        }
    }
}
