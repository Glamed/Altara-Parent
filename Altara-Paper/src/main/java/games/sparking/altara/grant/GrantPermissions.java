package games.sparking.altara.grant;

import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Who may grant or remove which rank.
 *
 * <ul>
 *   <li>Console and owners (rank weight ≥ owner weight) may grant anything but the default rank.</li>
 *   <li>Everyone else needs a strictly higher rank weight <em>and</em>
 *       {@code altara.grant.<rank>}.</li>
 *   <li>Removing a grant additionally requires {@link #REMOVE_PERMISSION}.</li>
 * </ul>
 */
public final class GrantPermissions {

    public static final String RANK_PERMISSION_PREFIX = "altara.grant.";
    public static final String REMOVE_PERMISSION = "altara.grants.remove";

    private GrantPermissions() {}

    public static boolean canGrant(CommandSender sender, Rank rank) {
        if (rank == null || rank.isDefaultRank()) return false;
        if (!(sender instanceof Player player)) return true;

        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        if (profile == null) return false;

        int weight = profile.getRealCurrentGrant().asRank().getWeight();
        if (weight >= Altara.getSharedInstance().getMainConfig().getOwnerWeight()) return true;

        return weight > rank.getWeight()
                && player.hasPermission(RANK_PERMISSION_PREFIX + rank.getName().toLowerCase());
    }

    public static boolean canRemove(CommandSender sender, Rank rank) {
        return canGrant(sender, rank) && sender.hasPermission(REMOVE_PERMISSION);
    }
}
