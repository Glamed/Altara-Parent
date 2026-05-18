package games.sparking.altara.game;

import games.sparking.altara.game.command.parameter.GameIdParameter;

/**
 * Lightweight wrapper around a game short-ID string.
 * Used as a distinct Java type so that
 * {@link GameIdParameter} is resolved
 * instead of the plain {@code StringParameter} for game-ID arguments.
 */
public record GameIdRef(String value) {
    @Override
    public String toString() {
        return value;
    }
}

