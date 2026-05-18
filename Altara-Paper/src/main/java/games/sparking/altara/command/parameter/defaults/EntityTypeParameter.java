package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class EntityTypeParameter implements ParameterType<EntityType> {

    @Override
    public EntityType parse(CommandSender sender, String source) {
        EntityType parsed = null;
        for (EntityType type : EntityType.values()) {
            if (type.name().equalsIgnoreCase(source)) {
                parsed = type;
                break;
            }

            if (type.getKey().getKey().equalsIgnoreCase(source)) {
                parsed = type;
                break;
            }
        }

        if (parsed == null)
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "Entity " + source + " was not found."));

        return parsed;
    }


    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            completions.add(type.name().toLowerCase());
        }
        return completions;
    }
}
