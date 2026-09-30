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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Resolves a player name or UUID to a {@link Profile}.  Online players use their cached
 * profile; offline players are fetched from the API <em>without</em> being cached, so
 * a later login always loads fresh data.  Blocking — commands using it must be async.
 */
public class ProfileParameter implements ParameterType<Profile> {

    @Override
    public Profile parse(CommandSender sender, String source) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getLogger().log(Level.SEVERE, "ProfileParameter used on the main thread — mark the command async",
                    new IllegalStateException());
            sender.sendMessage(CC.error(Messages.API_ERROR));
            return null;
        }

        if (source.equals("@self") && sender instanceof Player player) {
            return Altara.getSharedInstance().getProfileService().getProfile(player);
        }

        Player online = Bukkit.getPlayerExact(source);
        UUID uuid = online != null ? online.getUniqueId()
                : UUIDUtils.isUUID(source) ? UUID.fromString(source)
                : UUIDCache.getUuid(source);

        if (uuid == null) {
            sender.sendMessage(CC.error(Messages.NEVER_JOINED));
            return null;
        }

        Profile cached = Altara.getSharedInstance().getProfileService().getProfile(uuid);
        if (cached != null) return cached;

        RequestResponse response = RequestHandler.get("api/profile/%s", uuid.toString());
        if (response.getCode() == 404) {
            sender.sendMessage(CC.error(Messages.NEVER_JOINED));
            return null;
        }
        if (!response.wasSuccessful()) {
            sender.sendMessage(CC.error("Unable to load player.", response.getErrorMessage() + " (" + response.getCode() + ")"));
            return null;
        }

        return new Profile(response.asObject());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return PlayerParameter.TAB_COMPLETE_FUNCTION.apply(sender, flags);
    }
}
