package games.sparking.altara.command.parameter;

import org.bukkit.command.CommandSender;

import java.util.List;


public interface ParameterType<T> {

    T parse(CommandSender sender, String source);

    List<String> tabComplete(CommandSender sender, List<String> flags);

}
