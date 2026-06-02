package games.sparking.altara.framework;

/**
 * Represents the current state of a player within a game.
 */
public enum GameState {

    /** Player is in the pre-game lobby area. */
    LOBBY,

    /** Player is actively playing (in-game). */
    PLAYING,

    /** Player is watching the match as a spectator. */
    SPECTATING,

    /** Player has died and is waiting for the round to end. */
    DEAD
}

