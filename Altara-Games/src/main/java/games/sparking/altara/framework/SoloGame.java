package games.sparking.altara.framework;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Convenience base class for every game where players compete <em>individually</em>.
 *
 * <p>Hardwires {@link GameType#SOLO} into {@link AbstractGame} so implementing
 * classes only need to define their id, modules, and lifecycle hooks.
 *
 * <h3>Win-condition helpers</h3>
 * <ul>
 *   <li>{@link #hasWinner()} — returns {@code true} when at most one player
 *       is still in the {@link GameState#PLAYING} state.</li>
 *   <li>{@link #getWinner()} — returns that player's UUID wrapped in an
 *       {@link Optional}, or {@link Optional#empty()} if nobody is left.</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>
 * {@literal @}RegisterGame
 * public class MyFFA extends SoloGame {
 *
 *     {@literal @}Override
 *     public String id() { return "ffa"; }
 *
 *     {@literal @}Override
 *     public void start() {
 *         // spawn players, etc.
 *     }
 *
 *     {@literal @}Override
 *     public void stop() {
 *         getActivePlayers().forEach(this::removePlayer);
 *     }
 * }
 * </pre>
 */
public abstract class SoloGame extends AbstractGame {

    protected SoloGame() {
        super(GameType.SOLO);
    }

    // -------------------------------------------------------------------------
    // Win-condition helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when one or zero players are still actively
     * {@link GameState#PLAYING} — i.e. the game can declare a winner (or a draw).
     */
    public boolean hasWinner() {
        return getActivePlayers().size() <= 1;
    }

    /**
     * Returns the sole surviving player, if exactly one {@link GameState#PLAYING}
     * player remains.
     *
     * @return the winner's UUID, or {@link Optional#empty()} if nobody is left or
     *         more than one player is still alive
     */
    public Optional<UUID> getWinner() {
        Set<UUID> active = getActivePlayers();
        if (active.size() == 1) return Optional.of(active.iterator().next());
        return Optional.empty();
    }
}

