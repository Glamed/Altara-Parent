package games.sparking.altara.framework;

import games.sparking.altara.framework.module.team.GameTeam;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

/**
 * Convenience base class for every game where players compete in
 * colour-coded <em>teams</em>.
 *
 * <p>Hardwires {@link GameType#TEAM} into {@link AbstractGame} and adds a
 * configurable {@link #getPlayersPerTeam() players-per-team} contract so
 * match-making and balancing logic has a single source of truth.
 *
 * <h3>Win-condition helpers</h3>
 * <ul>
 *   <li>{@link #hasWinner()} — returns {@code true} when at most one team
 *       still has players in the {@link GameState#PLAYING} state.</li>
 *   <li>{@link #getWinnerTeam()} — returns that team wrapped in an
 *       {@link Optional}, or {@link Optional#empty()} when no teams remain.</li>
 * </ul>
 *
 * <h3>Example — SkyWars Duos (2-player teams)</h3>
 * <pre>
 * {@literal @}RegisterGame
 * public class SkyWarsDuosGame extends TeamGame {
 *
 *     public SkyWarsDuosGame() {
 *         super(2);   // 2 players per team
 *     }
 *
 *     {@literal @}Override
 *     public String id() { return "skywars-duos"; }
 *
 *     {@literal @}Override
 *     public List{@literal <GameModule>} modules() {
 *         return List.of(new SpectatorModule(), new TeamModule(this));
 *     }
 *
 *     {@literal @}Override
 *     public void start() {
 *         createTeam(TeamColor.RED);
 *         createTeam(TeamColor.BLUE);
 *     }
 *
 *     {@literal @}Override
 *     public void stop() {
 *         getActivePlayers().forEach(this::removePlayer);
 *     }
 * }
 * </pre>
 */
public abstract class TeamGame extends AbstractGame {

    /**
     * The maximum number of players that may be placed on a single team.
     * Typically {@code 2} for duos, {@code 4} for squads, etc.
     */
    @Getter
    private final int playersPerTeam;

    /**
     * @param playersPerTeam the maximum number of players that may be placed
     *                       on a single team (e.g. {@code 2} for duos)
     */
    protected TeamGame(int playersPerTeam) {
        super(GameType.TEAM);
        if (playersPerTeam < 1) throw new IllegalArgumentException("playersPerTeam must be >= 1");
        this.playersPerTeam = playersPerTeam;
    }

    // -------------------------------------------------------------------------
    // Win-condition helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when one or zero teams are still alive —
     * i.e. the game can declare a winner (or a draw).
     */
    public boolean hasWinner() {
        return getAliveTeams().size() <= 1;
    }

    /**
     * Returns the sole surviving team, if exactly one team still has players
     * in the {@link GameState#PLAYING} state.
     *
     * @return the winning {@link GameTeam}, or {@link Optional#empty()} if no
     *         teams remain alive or more than one team is still in the match
     */
    public Optional<GameTeam> getWinnerTeam() {
        List<GameTeam> alive = getAliveTeams();
        if (alive.size() == 1) return Optional.of(alive.get(0));
        return Optional.empty();
    }
}

