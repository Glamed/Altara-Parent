package games.sparking.altara.game.command.parameter;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.GameTypeRef;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Parses and tab-completes game-type identifiers (e.g. {@code "bomblobbers"}).
 * Tab list is drawn live from {@link GameManager#getRegisteredTypes()}.
 */
public class GameTypeParameter implements ParameterType<GameTypeRef> {

    @Override
    public GameTypeRef parse(CommandSender sender, String source) {
        GameManager gm = GameManager.getInstance();
        if (!gm.getRegisteredTypes().contains(source.toLowerCase())) {
            sender.sendMessage(CC.errorMsg("Unknown game type: " + CC.WHITE + source,
                    "Run &b/game types &cfor a list of available types."));
            return null;
        }
        return new GameTypeRef(source.toLowerCase());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return GameManager.getInstance().getRegisteredTypes().stream().sorted().toList();
    }
}

