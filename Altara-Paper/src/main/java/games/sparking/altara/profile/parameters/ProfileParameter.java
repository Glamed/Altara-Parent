package games.sparking.altara.profile.parameters;

import games.sparking.altara.Altara;
import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.command.parameter.defaults.PlayerParameter;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ProfileParameter implements ParameterType<Profile> {

    @Override
    public Profile parse(CommandSender sender, String source) {
        if (Bukkit.isPrimaryThread()) {
            sender.sendMessage(CC.RED + "Cannot use ProfileParameter on primary thread. Please inform server" +
                    " administration to mark issued command as async.");
            return null;
        }

        if (source.equals("@self") && sender instanceof Player) {
            return Altara.getSharedInstance().getProfileService().getProfile((Player) sender);
        }

        if (Bukkit.getPlayer(source) != null)
            return Altara.getSharedInstance().getProfileService().getProfile((Player) Bukkit.getOfflinePlayer(source));

        UUID uuid = UUIDUtils.isUUID(source) ? UUID.fromString(source) : UUIDCache.getUuid(source);

        if (uuid == null) {
            sender.sendMessage(CC.errorMsg(Messages.CONNECTED));
            return null;
        }

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (profile != null)
            return profile;

        RequestResponse response = RequestHandler.get("api/profile/%s", uuid.toString());
        if (!response.wasSuccessful()) {
            sender.sendMessage(CC.format("<red>Could not load profile of <yellow>%s<red>: %s (%d)",
                    source, response.getErrorMessage(), response.getCode()));
            return null;
        }

        profile = new Profile(response.asObject());
        Altara.getSharedInstance().getProfileService().cacheProfile(profile);
        return profile;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return PlayerParameter.TAB_COMPLETE_FUNCTION.apply(sender, flags);
    }
}