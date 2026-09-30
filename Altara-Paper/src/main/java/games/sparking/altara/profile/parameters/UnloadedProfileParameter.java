package games.sparking.altara.profile.parameters;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.command.parameter.defaults.PlayerParameter;
import games.sparking.altara.profile.UnloadedProfile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class UnloadedProfileParameter implements ParameterType<UnloadedProfile> {

    @Override
    public UnloadedProfile parse(CommandSender sender, String source) {
        if (source.equals("@self") && sender instanceof Player player) {
            return new UnloadedProfile(player.getUniqueId(), player.getName());
        }

        Player online = Bukkit.getPlayerExact(source);
        if (online != null) {
            return new UnloadedProfile(online.getUniqueId(), online.getName());
        }

        UUID uuid = UUIDCache.getUuid(source);
        if (uuid != null) {
            String name = UUIDCache.getName(uuid);
            return new UnloadedProfile(uuid, name != null ? name : source);
        }

        sender.sendMessage(CC.error(Messages.NEVER_JOINED));
        return null;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return PlayerParameter.TAB_COMPLETE_FUNCTION.apply(sender, flags);
    }
}
