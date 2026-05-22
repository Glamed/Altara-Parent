package games.sparking.altara.profile.parameters;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.profile.UnloadedProfile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnloadedProfileParameter implements ParameterType<UnloadedProfile> {

    @Override
    public UnloadedProfile parse(CommandSender sender, String source) {
        if ((source.equals("@self")) && (sender instanceof Player player)) {
            return new UnloadedProfile(player.getUniqueId(), player.getName());
        }

        if (Bukkit.getPlayer(source) != null) {
            Player player = Bukkit.getPlayer(source);
            return new UnloadedProfile(player.getUniqueId(), player.getName());
        }

        UUID uuid = UUIDCache.getUuid(source);
        if (uuid != null) {
            return new UnloadedProfile(uuid, source);
        }

        sender.sendMessage(CC.errorMsg(Messages.CONNECTED));
        return null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!(sender instanceof Player)) {
                completions.add(player.getName());
            } else {
                if (((Player) sender).canSee(player)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}