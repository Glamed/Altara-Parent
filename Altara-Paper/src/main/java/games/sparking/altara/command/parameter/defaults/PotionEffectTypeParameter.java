package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class PotionEffectTypeParameter implements ParameterType<PotionEffectType> {

    @Override
    public PotionEffectType parse(CommandSender sender, String source) {
        PotionEffectType type = PotionEffectType.getByName(source);

        if (type == null)
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "Potion effect " + source + " was not found."));

        return type;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type == null || type.getName() == null)
                continue;

            completions.add(type.getName());
        }
        return completions;
    }
}
