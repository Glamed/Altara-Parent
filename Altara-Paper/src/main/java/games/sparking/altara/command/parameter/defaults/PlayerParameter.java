package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.uuid.UUIDUtils;
import games.sparking.altara.visibility.VisibilityService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;

public class PlayerParameter implements ParameterType<Player> {

    public static final BiFunction<CommandSender, List<String>, List<String>> TAB_COMPLETE_FUNCTION = (sender, flags) -> {
        List<String> completions = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers())
            if (VisibilityService.getOnlineTreatProvider().apply(player, sender))
                completions.add(player.getName());

        return completions;
    };

    @Override
    public Player parse(CommandSender sender, String source) {
        if ((source.equals("@self")) && (sender instanceof Player))
            return (Player) sender;

        Player player;
        if (UUIDUtils.isUUID(source))
            player = Bukkit.getPlayer(UUID.fromString(source));
        else player = Bukkit.getPlayer(source);

        if (player == null || !VisibilityService.getOnlineTreatProvider().apply(player, sender)) {
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "Player " + source + " is not online."));
            return null;
        }

        return player;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return TAB_COMPLETE_FUNCTION.apply(sender, flags);
    }
}
