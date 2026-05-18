package games.sparking.altara.game.games.skywars;

import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.team.GameTeam;
import games.sparking.altara.game.team.TeamColor;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import games.sparking.altara.world.AltaraWorld;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * <h1>TeamSkyWars</h1>
 *
 * <p>Team-based SkyWars session. Players are distributed evenly across teams
 * using spawn-group names from the arena's {@code WorldConfig.dat}
 * ({@code TEAM_NAME:Red}, {@code TEAM_NAME:Blue}, etc.).
 *
 * <p>Multiple concurrent sessions are fully supported and isolated.
 */
public class TeamSkyWars extends BaseSkyWars {

    private static final List<TeamColor> TEAM_COLORS = List.of(
            TeamColor.RED, TeamColor.BLUE, TeamColor.GREEN, TeamColor.YELLOW,
            TeamColor.AQUA, TeamColor.PINK, TeamColor.WHITE, TeamColor.ORANGE
    );

    /** Teams that have already been reported as eliminated (fire once per team). */
    private final Set<String> eliminatedTeamIds = new HashSet<>();

    // =========================================================================
    // Metadata
    // =========================================================================

    @Override public String getName()        { return "TeamSkyWars"; }
    @Override public String getDescription() { return "Team sky battle — the last team standing wins!"; }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onRecruit() {
        broadcast(ChatColor.AQUA + "Team SkyWars §7— choose your kit!");
        broadcast(ChatColor.GRAY + "Last team alive wins!");
        super.onRecruit();
    }

    @Override
    protected void onStart() {
        AltaraWorld arena = getArenaWorld();
        List<Location> allSpawns = new ArrayList<>();

        if (arena != null) {
            // Each TEAM_NAME entry in WorldConfig.dat becomes a game team.
            Map<String, List<Location>> allSpawnMap = arena.getAllSpawns();
            if (!allSpawnMap.isEmpty()) {
                int colorIdx = 0;
                for (Map.Entry<String, List<Location>> entry : allSpawnMap.entrySet()) {
                    String tName = entry.getKey();
                    TeamColor color = TEAM_COLORS.get(colorIdx++ % TEAM_COLORS.size());
                    createTeam(tName.toLowerCase(), tName, color);
                    allSpawns.addAll(entry.getValue());
                }
            } else {
                // Fallback: single "Players" team (effectively solo)
                allSpawns.addAll(arena.getSpawns("Players"));
                createTeam("players", "Players", TeamColor.YELLOW);
            }
        }

        distributePlayersEvenly();
        startGame(allSpawns);
    }

    // =========================================================================
    // Team win condition
    // =========================================================================

    @Override
    protected void onPlayerEliminated(GamePlayer gp) {
        Player p = gp.getPlayer();
        String name = (p != null) ? p.getName() : gp.getName();
        broadcast(ChatColor.GRAY + name + " was eliminated! §e(" + getAliveCount() + " left)");

        // Check if this elimination knocked out a whole team
        GameTeam team = gp.getTeam();
        if (team != null && !team.isAlive() && eliminatedTeamIds.add(team.getId())) {
            broadcastTeamEliminated(team);
        }

        checkWinCondition();
    }

    private void broadcastTeamEliminated(GameTeam team) {
        broadcast(team.getColor().prefix() + team.getName()
                + ChatColor.GRAY + " has been eliminated!");
    }

    /** Safety-net once per second — catches edge cases where onPlayerEliminated wasn't called. */
    @EventHandler
    public void onUpdateSec(UpdateEvent event) {
        if (event.getType() != UpdateType.SEC || !isLive()) return;

        for (GameTeam team : getTeams()) {
            if (!team.isAlive() && !team.getAllPlayers().isEmpty()
                    && eliminatedTeamIds.add(team.getId())) {
                broadcastTeamEliminated(team);
            }
        }

        checkWinCondition();
    }

    private void checkWinCondition() {
        if (!isLive()) return;

        List<GameTeam> alive = getTeams().stream()
                .filter(GameTeam::isAlive)
                .toList();

        if (alive.size() <= 1) {
            onWinnerDecided(alive.isEmpty() ? null : alive.getFirst());
            endGame();
        }
    }

    private void onWinnerDecided(@Nullable GameTeam winner) {
        if (winner == null) {
            broadcast(ChatColor.GRAY + "No winner — all teams were eliminated!");
        } else {
            broadcast(winner.getColor().prefix() + ChatColor.BOLD + winner.getName()
                    + ChatColor.RESET + ChatColor.GOLD + " wins Team SkyWars!");
        }
    }
}
