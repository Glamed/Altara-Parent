package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;


public class WorldParameter implements ParameterType<World> {

    @Override
    public World parse(CommandSender sender, String source) {
        World world = Bukkit.getWorld(source);
        if (world == null) {
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "World " + source + " was not found."));
            return null;
        }
        return world;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            completions.add(world.getName());
        }
        return completions;
    }
}
