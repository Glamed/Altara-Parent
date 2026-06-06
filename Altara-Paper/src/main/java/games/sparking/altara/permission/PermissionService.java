package games.sparking.altara.permission;
import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.configuration.entry.LocalPermissionEntry;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class PermissionService {

    private static final Logger LOG = Bukkit.getLogger();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public void injectPlayer(Player player) {
        Profile profile = AltaraPaper.getSharedInstance().getProfileService().getProfile(player);
        if (profile == null) {
            LOG.warning(String.format("Tried to inject player without profile: %s (%s)", player.getUniqueId(), player.getName()));
            return;
        }

        // Clean old attachment if any
        if (attachments.containsKey(player.getUniqueId())) {
            player.removeAttachment(attachments.get(player.getUniqueId()));
        }

        PermissionAttachment attachment = player.addAttachment(AltaraPaper.getPlugin());
        attachments.put(player.getUniqueId(), attachment);

        updatePermissions(player);
    }

    public void uninjectPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (attachments.containsKey(uuid)) {
            player.removeAttachment(attachments.get(uuid));
            attachments.remove(uuid);
        }
    }

    public void updatePermissions(Player player) {
        Profile profile = AltaraPaper.getSharedInstance().getProfileService().getProfile(player);
        if (profile == null) {
            LOG.warning(String.format("Tried to update player without profile: %s (%s)", player.getUniqueId(), player.getName()));
            return;
        }

        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (attachment == null) return;

        // Clear old permissions
        attachment.getPermissions().keySet().forEach(attachment::unsetPermission);

        Map<String, Boolean> perms = getEffectivePermissions(profile);
        perms.forEach(attachment::setPermission);
    }

    public Map<String, Boolean> getEffectivePermissions(Profile profile) {
        Map<String, Boolean> effectivePermissions = new HashMap<>();

        List<Grant> grants = new ArrayList<>(profile.getActiveGrants());
        grants.sort(Grant.COMPARATOR.reversed());

        for (Grant grant : grants) {
            effectivePermissions.putAll(convert(grant.asRank().getAllPermissions()));
        }

        effectivePermissions.putAll(convert(profile.getPermissions()));

        LocalPermissionEntry entry = AltaraPaper.getPaperInstance().getLocalPermissionConfig().getEntry(profile);
        if (entry != null) {
            effectivePermissions.putAll(convert(entry.getPermissions()));
        }

        return effectivePermissions;
    }

    public Map<String, Boolean> convert(List<String> list) {
        Map<String, Boolean> permissions = new HashMap<>();
        list.forEach(permission -> {
            if (permission.startsWith("-"))
                permissions.put(permission.substring(1), false);
            else
                permissions.put(permission, true);
        });
        return permissions;
    }

    public List<Component> getDebugInfo(Player player, String permission) {
        List<Component> debugs = new ArrayList<>();
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        AtomicBoolean hasPermission = new AtomicBoolean(false);

        profile.getActiveGrants().stream()
                .map(Grant::getRank)
                .sorted()
                .forEach(uuid -> {
                    Rank rank = Altara.getSharedInstance().getRankService().getRank(uuid);
                    Map<String, Boolean> perms = convert(rank.getAllPermissions());
                    Component result = Component.text("NOT_SET", CC.GRAY);
                    if (perms.containsKey(permission.toLowerCase())) {
                        Boolean value = perms.get(permission.toLowerCase());
                        result = CC.colorBoolean(value, "YES", "NEGATED");
                        if (value)
                            hasPermission.set(true);
                    }
                    debugs.add(Component.text("Grant " + rank.getName() + ": ", CC.BLUE).append(result));
                });

        Map<String, Boolean> perms = convert(profile.getPermissions());
        Component result = Component.text("NOT_SET", CC.GRAY);
        if (perms.containsKey(permission.toLowerCase())) {
            Boolean value = perms.get(permission.toLowerCase());
            result = CC.colorBoolean(value, "YES", "NEGATED");
            if (value)
                hasPermission.set(true);
        }

        debugs.add(Component.text("Profile: ", CC.BLUE).append(result));
        debugs.add(CC.format("<blue>Result: <yellow>%s</yellow> %s <blue>permission <yellow>%s</yellow>.</blue>",
                player.getName(),
                CC.strip(CC.colorBoolean(hasPermission.get(), "has", "doesn't have")),
                permission));

        return debugs;
    }
}