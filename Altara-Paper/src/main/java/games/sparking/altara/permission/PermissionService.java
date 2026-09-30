package games.sparking.altara.permission;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.configuration.entry.LocalPermissionEntry;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;

/**
 * Applies rank, profile and server-local permissions to online players through a
 * single {@link PermissionAttachment} each.  Main thread only.
 */
public class PermissionService {

    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public void injectPlayer(Player player) {
        uninjectPlayer(player);
        attachments.put(player.getUniqueId(), player.addAttachment(AltaraPaper.getPlugin()));
        updatePermissions(player);
    }

    public void uninjectPlayer(Player player) {
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    public void updatePermissions(Player player) {
        Profile profile = AltaraPaper.getSharedInstance().getProfileService().getProfile(player);
        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (profile == null || attachment == null) {
            AltaraPaper.getPlugin().getLogger().warning("Can't update permissions of " + player.getName()
                    + (profile == null ? " (no profile loaded)" : " (not injected)"));
            return;
        }

        attachment.getPermissions().keySet().forEach(attachment::unsetPermission);
        getEffectivePermissions(profile).forEach(attachment::setPermission);
    }

    /**
     * Rank permissions (lowest weight first so higher ranks win), then the profile's own
     * permissions, then this server's local overrides.
     */
    public Map<String, Boolean> getEffectivePermissions(Profile profile) {
        Map<String, Boolean> effective = new HashMap<>();

        List<Grant> grants = new ArrayList<>(profile.getActiveGrants());
        grants.sort(Grant.COMPARATOR);
        for (Grant grant : grants) {
            Rank rank = grant.asRank();
            if (rank != null) effective.putAll(convert(rank.getAllPermissions()));
        }

        effective.putAll(convert(profile.getPermissions()));

        LocalPermissionEntry entry = AltaraPaper.getPaperInstance().getLocalPermissionConfig().getEntry(profile);
        if (entry != null) {
            effective.putAll(convert(entry.getPermissions()));
        }
        return effective;
    }

    /** {@code node} grants, {@code -node} negates. */
    public Map<String, Boolean> convert(List<String> list) {
        Map<String, Boolean> permissions = new LinkedHashMap<>();
        for (String permission : list) {
            if (permission.startsWith("-")) permissions.put(permission.substring(1), false);
            else permissions.put(permission, true);
        }
        return permissions;
    }
}
