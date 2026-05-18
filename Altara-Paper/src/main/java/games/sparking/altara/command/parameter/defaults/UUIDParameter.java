package games.sparking.altara.command.parameter.defaults;


import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.uuid.UUIDUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class UUIDParameter implements ParameterType<UUID> {

    @Override
    public UUID parse(CommandSender sender, String source) {
        if ((source.equals("@self")) && (sender instanceof Player)) {
            return ((Player) sender).getUniqueId();
        }

        if (UUIDUtils.isUUID(source)) {
            return UUID.fromString(source);
        }

        return UUID.fromString(source);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return PlayerParameter.TAB_COMPLETE_FUNCTION.apply(sender, flags);
    }
}
