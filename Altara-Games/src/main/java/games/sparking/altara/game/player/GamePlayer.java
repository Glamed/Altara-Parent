package games.sparking.altara.game.player;

import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.team.GameTeam;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Wraps a Bukkit {@link Player} with game-specific context such as
 * their current {@link GamePlayerState} and the {@link GameTeam} they belong to.
 * <p>
 * The backing {@link Player} reference is resolved lazily via
 * {@link Bukkit#getPlayer(UUID)} so that a {@code GamePlayer} object remains
 * valid even across short disconnects.
 */
@Getter
public class GamePlayer {

    private final UUID uuid;
    private final String name;
    private final Game game;

    @Setter private GamePlayerState state = GamePlayerState.ALIVE;
    @Setter private GameTeam team;

    public GamePlayer(Player player, Game game) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.game = game;
    }

    /**
     * Returns the online {@link Player}, or {@code null} if the player is offline.
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /** @return {@code true} if the player is currently online. */
    public boolean isOnline() {
        return getPlayer() != null;
    }

    /** @return {@code true} if the player's state is {@link GamePlayerState#ALIVE}. */
    public boolean isAlive() {
        return state == GamePlayerState.ALIVE;
    }

    /** @return {@code true} if the player is spectating (either eliminated or pure spectator). */
    public boolean isSpectating() {
        return state == GamePlayerState.SPECTATING || state == GamePlayerState.ELIMINATED;
    }

    /**
     * Transitions this player to {@link GamePlayerState#ELIMINATED}.
     * Call this when the player loses but you want them to stay in the game world.
     */
    public void eliminate() {
        this.state = GamePlayerState.ELIMINATED;
    }

    /**
     * Transitions this player to {@link GamePlayerState#SPECTATING}.
     * Use for players who joined purely as observers.
     */
    public void makeSpectator() {
        this.state = GamePlayerState.SPECTATING;
    }

    /**
     * Sends a message to this player if they are online.
     *
     * @param message the message to send
     */
    public void sendMessage(String message) {
        Player p = getPlayer();
        if (p != null) p.sendMessage(message);
    }
}

