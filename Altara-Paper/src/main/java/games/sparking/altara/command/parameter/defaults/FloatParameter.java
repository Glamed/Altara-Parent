package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class FloatParameter implements ParameterType<Float> {

    @Override
    public Float parse(CommandSender sender, String source) {
        Float value;
        try {
            value = Float.parseFloat(source);
            if (!Float.isFinite(value)) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(CC.error("Invalid number.", "*" + source + "* isn't a valid number."));
            return null;
        }
        return value;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return new ArrayList<>();
    }
}
