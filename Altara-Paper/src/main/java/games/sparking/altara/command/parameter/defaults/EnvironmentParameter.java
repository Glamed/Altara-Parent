package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentParameter implements ParameterType<World.Environment> {

    @Override
    public World.Environment parse(CommandSender sender, String source) {
        World.Environment environment;
        try {
            environment = World.Environment.valueOf(source.toUpperCase());

        } catch (IllegalArgumentException exception) {
            sender.sendMessage(CC.error("Invalid environment.", "*" + source + "* doesn't exist."));
            return null;
        }

        return environment;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();

        for (World.Environment value : World.Environment.values())
            completions.add(value.name());

        return completions;
    }
}
