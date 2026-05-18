package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ShortParameter implements ParameterType<Short> {

    @Override
    public Short parse(CommandSender sender, String source) {
        Short value;
        try {
            value = Short.parseShort(source);
        } catch (NumberFormatException e) {
            sender.sendMessage(CC.errorMsg("Invalid arguments.", source + " is not a valid number."));
            return null;
        }
        return value;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return new ArrayList<>();
    }
}
