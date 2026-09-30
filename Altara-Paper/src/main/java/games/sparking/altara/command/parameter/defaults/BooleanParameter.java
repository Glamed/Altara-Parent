package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BooleanParameter implements ParameterType<Boolean> {

    private final Map<String, Boolean> map = new HashMap<>() {{
        put("true", true);
        put("on", true);
        put("yes", true);
        put("enabled", true);
        put("false", false);
        put("off", false);
        put("no", false);
        put("disabled", false);
    }};

    @Override
    public Boolean parse(CommandSender sender, String source) {
        if (!map.containsKey(source.toLowerCase())) {
            sender.sendMessage(CC.error("Invalid value.", "Use *true* or *false*."));
            return null;
        }
        return this.map.get(source.toLowerCase());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return new ArrayList<>(this.map.keySet());
    }
}
