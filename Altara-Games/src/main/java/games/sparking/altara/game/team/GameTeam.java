package games.sparking.altara.game.team;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.game.player.GamePlayerState;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a group of players competing together within a single {@link Game} instance.
 * <p>
 * Teams are identified by a unique {@code id} string within their game and carry a
 * display {@link #name} and a {@link TeamColor}.  Players are tracked as
 * {@link GamePlayer} objects so that state (alive / eliminated / spectating) is
 * always available.
 */
@Getter
public class GameTeam {

    private final String id;
    private String name;
    private TeamColor color;

    /** Players currently on this team, keyed by their {@link java.util.UUID}. */
    private final Map<UUID, GamePlayer> players = new LinkedHashMap<>();

    public GameTeam(String id, String name, TeamColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    // -------------------------------------------------------------------------
    // Player management
    // -------------------------------------------------------------------------

    /**
     * Adds a {@link GamePlayer} to this team and records the team on the player.
     *
     * @param gamePlayer the player to add
     * @return {@code true} if the player was newly added; {@code false} if already present
     */
    public boolean addPlayer(GamePlayer gamePlayer) {
        if (players.containsKey(gamePlayer.getUuid())) return false;
        players.put(gamePlayer.getUuid(), gamePlayer);
        gamePlayer.setTeam(this);
        return true;
    }

    /**
     * Removes a player from this team by their Bukkit {@link Player} handle.
     *
     * @param player the player to remove
     * @return {@code true} if the player was removed; {@code false} if they were not on this team
     */
    public boolean removePlayer(Player player) {
        GamePlayer gp = players.remove(player.getUniqueId());
        if (gp != null) {
            if (gp.getTeam() == this) gp.setTeam(null);
            return true;
        }
        return false;
    }

    /**
     * @param player the player to check
     * @return {@code true} if the player is a member of this team
     */
    public boolean hasPlayer(Player player) {
        return players.containsKey(player.getUniqueId());
    }

    /**
     * Returns all {@link GamePlayer}s on this team whose state is {@link GamePlayerState#ALIVE}.
     */
    public List<GamePlayer> getAlivePlayers() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * Returns all {@link GamePlayer}s on this team regardless of state.
     */
    public List<GamePlayer> getAllPlayers() {
        return new ArrayList<>(players.values());
    }

    // -------------------------------------------------------------------------
    // State queries
    // -------------------------------------------------------------------------

    /** @return {@code true} if at least one team member is {@link GamePlayerState#ALIVE}. */
    public boolean isAlive() {
        return players.values().stream().anyMatch(GamePlayer::isAlive);
    }

    /** @return the total number of members on this team. */
    public int getSize() {
        return players.size();
    }

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    /**
     * Sends a message to every online player on this team.
     *
     * @param message the message to broadcast
     */
    public void broadcast(String message) {
        players.values().forEach(gp -> gp.sendMessage(message));
    }

    // -------------------------------------------------------------------------
    // Mutators
    // -------------------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }

    public void setColor(TeamColor color) {
        this.color = color;
    }

    /** Returns the formatted team name with its colour prefix. */
    public String getFormattedName() {
        return color.prefix() + name;
    }

    @Override
    public String toString() {
        return "GameTeam{id='" + id + "', name='" + name + "', size=" + players.size() + '}';
    }
}

