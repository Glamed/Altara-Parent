package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameModeParameter implements ParameterType<GameMode> {

    private final Map<String, GameMode> map = new HashMap<>() {{
        put("survival", GameMode.SURVIVAL);
        put("s", GameMode.SURVIVAL);
        put("0", GameMode.SURVIVAL);
        put("creative", GameMode.CREATIVE);
        put("c", GameMode.CREATIVE);
        put("1", GameMode.CREATIVE);
        put("adventure", GameMode.ADVENTURE);
        put("a", GameMode.ADVENTURE);
        put("2", GameMode.ADVENTURE);
        put("spectator", GameMode.SPECTATOR);
        put("spec", GameMode.SPECTATOR);
        put("sp", GameMode.SPECTATOR);
        put("3", GameMode.SPECTATOR);
    }};

    @Override
    public GameMode parse(CommandSender sender, String source) {
        if ((source.equals("@toggle")) && (sender instanceof Player)) {
            return ((Player) sender).getGameMode().equals(GameMode.CREATIVE) ? GameMode.SURVIVAL : GameMode.CREATIVE;
        }
        if (!map.containsKey(source.toLowerCase())) {
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "Gamemode " + source + " was not found."));
            return null;
        }

        return map.get(source.toLowerCase());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return new ArrayList<>(map.keySet());
    }
}
