package games.sparking.altara.framework;

/**
 * Determines the structural mode of a game session.
 *
 * <p>The game type is declared once per game class and drives both how players are
 * grouped and which framework modules are applicable:
 * <ul>
 *   <li>{@link #SOLO} — every participant fights or competes individually.</li>
 *   <li>{@link #TEAM} — participants are divided into colour-coded {@link team.GameTeam}s;
 *       the {@link team.TeamModule} can be added to enforce friendly-fire rules automatically.</li>
 * </ul>
 */
public enum GameType {

    /**
     * Every player competes for themselves.
     * No team assignments or friendly-fire checks are performed by the framework.
     */
    SOLO,

    /**
     * Players are grouped into colour-coded teams (see {@link team.TeamColor}).
     * Use {@link AbstractGame#createTeam(team.TeamColor)} to register teams,
     * and {@link AbstractGame#assignTeam(java.util.UUID, team.GameTeam)} to assign players.
     * Add {@link team.TeamModule} to your module list for automatic friendly-fire prevention.
     */
    TEAM
}

