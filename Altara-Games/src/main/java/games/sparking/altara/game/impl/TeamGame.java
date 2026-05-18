package games.sparking.altara.game.impl;

import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.team.GameTeam;
import games.sparking.altara.game.team.TeamColor;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract base for team-based game modes.
 */
public abstract class TeamGame extends Game {

    /**
     * Tracks teams that have already been reported as eliminated, preventing
     * {@link #onTeamEliminated(GameTeam)} from firing more than once per team.
     */
    private final Set<String> eliminatedTeamIds = new HashSet<>();

    // =========================================================================
    // Abstract hooks
    // =========================================================================

    /** Called once when a team loses all its alive members. Broadcast your message here. */
    protected abstract void onTeamEliminated(GameTeam team);

    /** Called when the game is over. {@code winner} is {@code null} if no teams survived. */
    protected abstract void onWinnerDecided(@Nullable GameTeam winner);

    // =========================================================================
    // Elimination
    // =========================================================================

    /**
     * Immediately fires {@link #onTeamEliminated(GameTeam)} if the eliminated player's
     * team is now dead, then checks the win condition.
     * Override and call {@code super.onPlayerEliminated(gp)} to add a broadcast message.
     */
    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        GameTeam team = gp.getTeam();
        if (team != null && !team.isAlive() && eliminatedTeamIds.add(team.getId())) {
            onTeamEliminated(team);
        }
        checkWinCondition();
    }

    // =========================================================================
    // Periodic safety-net via UpdateEvent
    // =========================================================================

    /**
     * Safety-net: checks once per second whether any team has been eliminated
     * (primary path is via {@link #onPlayerEliminated(GamePlayer)}).
     */
    @EventHandler
    public void onUpdateSec(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC || !isLive()) return;

        for (GameTeam team : getTeams()) {
            if (!team.isAlive() && !team.getAllPlayers().isEmpty()
                    && eliminatedTeamIds.add(team.getId())) {
                onTeamEliminated(team);
            }
        }
    }

    // =========================================================================
    // Win condition
    // =========================================================================

    public final void checkWinCondition() {
        if (!isLive()) return;

        List<GameTeam> aliveTeams = getTeams().stream()
                .filter(GameTeam::isAlive)
                .toList();

        if (aliveTeams.size() <= 1) {
            onWinnerDecided(aliveTeams.isEmpty() ? null : aliveTeams.get(0));
            endGame();
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    public GameTeam createTeam(String id, String name, TeamColor color) {
        return addTeam(new GameTeam(id, name, color));
    }

    public void distributePlayersEvenly() {
        List<GameTeam> teamList = getTeams();
        if (teamList.isEmpty()) return;
        int i = 0;
        for (var gp : getPlayers().values()) {
            teamList.get(i++ % teamList.size()).addPlayer(gp);
        }
    }
}
