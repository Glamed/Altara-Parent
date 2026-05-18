package games.sparking.altara.game;

import games.sparking.altara.game.command.parameter.GameTypeParameter;

/**
 * Lightweight wrapper around a game-type identifier string.
 * Used as a distinct Java type so that
 * {@link GameTypeParameter} is resolved
 * instead of the plain {@code StringParameter} for game-type arguments.
 */
public record GameTypeRef(String value) {
    @Override
    public String toString() {
        return value;
    }
}

