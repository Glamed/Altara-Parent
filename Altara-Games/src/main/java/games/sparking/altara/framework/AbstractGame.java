package games.sparking.altara.framework;

import games.sparking.altara.framework.module.team.GameTeam;
import games.sparking.altara.framework.module.team.TeamColor;
import lombok.Getter;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Convenient base class for every game that uses the Altara game framework.
 *
 * <p>{@code AbstractGame} wires together the three main concepts your game needs:
 * <ol>
 *   <li><b>Game type</b> — {@link GameType#SOLO} or {@link GameType#TEAM}, declared once in the constructor.</li>
 *   <li><b>Player states</b> — per-player {@link GameState} tracking with O(1) reads and writes.</li>
 *   <li><b>Team management</b> — colour-coded {@link GameTeam}s, player-to-team mapping, and
 *       a fast {@link #areTeammates} check (always {@code false} in SOLO games).</li>
 *   <li><b>Spectator management</b> — convenience methods to place players into spectator mode.
 *       Pair with {@link games.sparking.altara.framework.module.spectator.SpectatorModule} to get automatic
 *       damage/inventory/interaction cancellation.</li>
 * </ol>
 *
 * <h3>Implementing a game</h3>
 * <pre>
 * public class BedWarsGame extends AbstractGame {
 *
 *     private final BedWarsCombatModule combat = new BedWarsCombatModule(this);
 *
 *     public BedWarsGame() {
 *         super(GameType.TEAM);
 *     }
 *
 *     {@literal @}Override
 *     public String id() { return "bedwars"; }
 *
 *     {@literal @}Override
 *     public List{@literal <GameModule>} modules() {
 *         return List.of(
 *             new SpectatorModule(),
 *             new TeamModule(this),
 *             combat
 *         );
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
public abstract class AbstractGame implements Game {

    // -------------------------------------------------------------------------
    // Core fields
    // -------------------------------------------------------------------------

    /** Whether this is a solo or team game. Set once at construction. */
    @Getter
    private final GameType gameType;

    /** player UUID → their current state in this session */
    private final Map<UUID, GameState> playerStates = new HashMap<>();

    /** colour → team (only populated when gameType == TEAM) */
    private final Map<TeamColor, GameTeam> teams = new LinkedHashMap<>();

    /** player UUID → team (fast reverse lookup) */
    private final Map<UUID, GameTeam> playerTeamMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    /**
     * @param gameType whether players compete solo or in colour-coded teams
     */
    protected AbstractGame(GameType gameType) {
        this.gameType = gameType;
    }

    // -------------------------------------------------------------------------
    // Game interface — default implementations
    // -------------------------------------------------------------------------

    /**
     * Returns the player's current state, or {@link GameState#LOBBY} if they
     * are not tracked by this game.
     */
    @Override
    public GameState getState(UUID player) {
        return playerStates.getOrDefault(player, GameState.LOBBY);
    }

    /**
     * Returns {@code true} when the player is tracked and not in
     * {@link GameState#LOBBY} (i.e. PLAYING, DEAD, or SPECTATING).
     */
    @Override
    public boolean isActive(UUID player) {
        GameState s = playerStates.get(player);
        return s != null && s != GameState.LOBBY;
    }

    // -------------------------------------------------------------------------
    // Player state management
    // -------------------------------------------------------------------------

    /**
     * Sets the player's in-game state.
     * Passing {@link GameState#LOBBY} is equivalent to calling {@link #removePlayer(UUID)}.
     */
    public void setPlayerState(UUID player, GameState state) {
        if (state == GameState.LOBBY) {
            removePlayer(player);
        } else {
            playerStates.put(player, state);
        }
    }

    /**
     * Completely removes a player from this game session.
     * Clears their state and removes them from their team (if any).
     */
    public void removePlayer(UUID player) {
        playerStates.remove(player);
        GameTeam team = playerTeamMap.remove(player);
        if (team != null) team.removeMember(player);
    }

    /**
     * Returns a snapshot of all UUIDs whose state is currently {@link GameState#PLAYING}.
     */
    public Set<UUID> getActivePlayers() {
        Set<UUID> active = new HashSet<>();
        for (Map.Entry<UUID, GameState> e : playerStates.entrySet()) {
            if (e.getValue() == GameState.PLAYING) active.add(e.getKey());
        }
        return Collections.unmodifiableSet(active);
    }

    /**
     * Returns a snapshot of all UUIDs whose state is currently {@link GameState#SPECTATING}.
     */
    public Set<UUID> getSpectators() {
        Set<UUID> spectating = new HashSet<>();
        for (Map.Entry<UUID, GameState> e : playerStates.entrySet()) {
            if (e.getValue() == GameState.SPECTATING) spectating.add(e.getKey());
        }
        return Collections.unmodifiableSet(spectating);
    }

    // -------------------------------------------------------------------------
    // Spectator management
    // -------------------------------------------------------------------------

    /**
     * Transitions a player into spectator mode:
     * <ol>
     *   <li>Sets their in-game state to {@link GameState#SPECTATING}.</li>
     *   <li>Puts them into Bukkit's {@link GameMode#SPECTATOR} so they can fly
     *       through walls and see other spectators.</li>
     * </ol>
     *
     * <p>Add {@link games.sparking.altara.framework.module.spectator.SpectatorModule} to your
     * module list to automatically prevent damage, inventory interaction, etc.
     *
     * @param player the online player to spectate
     */
    public void addSpectator(Player player) {
        setPlayerState(player.getUniqueId(), GameState.SPECTATING);
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * Removes a player from spectator mode and clears their tracked state.
     * Does <em>not</em> change their Bukkit game mode — call
     * {@code player.setGameMode(...)} yourself after this if needed.
     *
     * @param player the player leaving spectator mode
     */
    public void removeSpectator(UUID player) {
        if (getState(player) == GameState.SPECTATING) {
            removePlayer(player);
        }
    }

    /**
     * Returns {@code true} if the player is currently in the SPECTATING state.
     */
    public boolean isSpectating(UUID player) {
        return getState(player) == GameState.SPECTATING;
    }

    // -------------------------------------------------------------------------
    // Team management
    // -------------------------------------------------------------------------

    /**
     * Creates and registers a {@link GameTeam} for the given colour.
     *
     * <p>Call this during {@link #start()} for each colour your game needs.
     * Duplicate registrations are ignored (the existing team is returned).
     *
     * @throws IllegalStateException if the game type is {@link GameType#SOLO}
     */
    public GameTeam createTeam(TeamColor color) {
        if (gameType == GameType.SOLO) {
            throw new IllegalStateException("Cannot create teams in a SOLO game.");
        }
        return teams.computeIfAbsent(color, GameTeam::new);
    }

    /** Returns the {@link GameTeam} registered for {@code color}, or {@code null} if not created. */
    public GameTeam getTeam(TeamColor color) {
        return teams.get(color);
    }

    /** Returns an unmodifiable view of all registered teams. */
    public Collection<GameTeam> getTeams() {
        return Collections.unmodifiableCollection(teams.values());
    }

    /**
     * Returns the team the player currently belongs to, or {@code null} if the
     * player has no team assignment (always {@code null} in SOLO games).
     */
    public GameTeam getPlayerTeam(UUID player) {
        return playerTeamMap.get(player);
    }

    /**
     * Assigns a player to a team, removing them from their previous team first.
     * Also sets the player's state to {@link GameState#PLAYING} if they were in LOBBY.
     *
     * @throws IllegalStateException if the game type is {@link GameType#SOLO}
     */
    public void assignTeam(UUID player, GameTeam team) {
        if (gameType == GameType.SOLO) {
            throw new IllegalStateException("Cannot assign teams in a SOLO game.");
        }

        // Remove from previous team
        GameTeam previous = playerTeamMap.get(player);
        if (previous != null) previous.removeMember(player);

        team.addMember(player);
        playerTeamMap.put(player, team);

        // Ensure the player is tracked as active if they were in LOBBY
        if (getState(player) == GameState.LOBBY) {
            setPlayerState(player, GameState.PLAYING);
        }
    }

    /**
     * Returns {@code true} if {@code a} and {@code b} are on the same team.
     * Always returns {@code false} in {@link GameType#SOLO} games.
     */
    public boolean areTeammates(UUID a, UUID b) {
        if (gameType == GameType.SOLO) return false;
        GameTeam teamA = playerTeamMap.get(a);
        return teamA != null && teamA.hasMember(b);
    }

    /**
     * Returns the list of teams that still have at least one player in
     * {@link GameState#PLAYING} state.
     */
    public List<GameTeam> getAliveTeams() {
        List<GameTeam> alive = new ArrayList<>();
        for (GameTeam team : teams.values()) {
            boolean hasAlive = false;
            for (UUID m : team.getMembers()) {
                if (getState(m) == GameState.PLAYING) {
                    hasAlive = true;
                    break;
                }
            }
            if (hasAlive) alive.add(team);
        }
        return Collections.unmodifiableList(alive);
    }
}

