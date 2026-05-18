package games.sparking.altara.game.command.parameter;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.game.GameIdRef;
import games.sparking.altara.game.GameManager;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Parses and tab-completes game short-IDs (e.g. {@code "a1b2"}).
 * Tab list is drawn live from {@link GameManager#getActiveGames()}.
 * An empty string is accepted and passed through so callers can
 * fall back to the player's own current game (same semantics as before).
 */
public class GameIdParameter implements ParameterType<GameIdRef> {

    @Override
    public GameIdRef parse(CommandSender sender, String source) {
        if (source == null || source.isEmpty()) return new GameIdRef("");

        if (GameManager.getInstance().getGameByShortId(source).isEmpty()) {
            sender.sendMessage(CC.errorMsg("No active game found with ID: " + CC.WHITE + source));
            return null;
        }
        return new GameIdRef(source);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return GameManager.getInstance().getActiveGames().values().stream()
                .map(game -> game.getShortId())
                .sorted()
                .toList();
    }
}

