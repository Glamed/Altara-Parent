package games.sparking.altara.game.player;

/**
 * Represents a player's current participation state within a game instance.
 */
public enum GamePlayerState {

    /** Player is actively playing the game. */
    ALIVE,

    /** Player was eliminated but remains in the game as a spectator. */
    ELIMINATED,

    /** Player is spectating without having been a participant. */
    SPECTATING
}

