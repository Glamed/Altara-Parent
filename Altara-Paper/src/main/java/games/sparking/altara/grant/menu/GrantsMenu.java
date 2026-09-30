package games.sparking.altara.grant.menu;

import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.GrantPermissions;
import games.sparking.altara.grant.input.GrantRemoveInput;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Time;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/** {@code /grants <player>}: every grant, newest first; active ones can be removed. */
public class GrantsMenu extends PagedMenu {

    private final Profile target;
    private final List<Grant> grants;

    public GrantsMenu(Profile target, List<Grant> grants) {
        this.target = target;
        this.grants = new ArrayList<>(grants);
        this.grants.sort(Comparator.comparingLong(Grant::getGrantedAt).reversed());
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Grants", target.getName()};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        for (Grant grant : grants) {
            buttons.put(buttons.size(), new GrantButton(grant));
        }
        return buttons;
    }

    private static String nameOf(String id) {
        if (!UUIDUtils.isUUID(id)) return id;
        String name = UUIDCache.getName(UUID.fromString(id));
        return name != null ? name : id;
    }

    private class GrantButton extends Button {

        private final Grant grant;

        GrantButton(Grant grant) {
            this.grant = grant;
        }

        @Override
        public ItemStack getItem(Player player) {
            Rank rank = grant.asRank();
            boolean active = grant.isActive() && !grant.isRemoved();
            String state = grant.isRemoved() ? "Removed" : grant.isActive() ? "Active" : "Expired";

            List<String> scopes = new ArrayList<>();
            grant.getScopes().forEach(scope -> scopes.add(scope.equals("GLOBAL") ? "Every realm" : StringUtils.capitalize(scope)));

            Gui.Lore lore = Gui.lore()
                    .value("Granted by", nameOf(grant.getGrantedBy()))
                    .value("Reason", grant.getGrantedReason())
                    .value("Scopes", String.join(", ", scopes))
                    .value("Granted on", Time.formatDate(grant.getGrantedAt(), AltaraSettings.TIME_ZONE.get(player)))
                    .value("Duration", grant.getDuration() == -1 ? "Permanent" : Time.formatDetailed(grant.getDuration()));

            if (active && grant.getDuration() != -1) {
                lore.value("Expires in", Time.formatDetailed(grant.getRemainingTime()));
            }

            if (grant.isRemoved()) {
                lore.blank()
                        .heading("Removed")
                        .entry("By " + nameOf(grant.getRemovedBy()))
                        .entry("On " + Time.formatDate(grant.getRemovedAt(), AltaraSettings.TIME_ZONE.get(player)))
                        .entry(grant.getRemovedReason());
            } else if (active && GrantPermissions.canRemove(player, rank)) {
                lore.cta("remove this grant");
            }

            return new ItemBuilder(active ? Material.LIME_WOOL : grant.isRemoved() ? Material.RED_WOOL : Material.GRAY_WOOL)
                    .setDisplayName(CC.format(rank.getDisplayName())
                            .append(Component.space())
                            .append(CC.bracketed(CC.state(active, state, state))))
                    .setLore(lore.build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (grant.isRemoved() || !grant.isActive() || !GrantPermissions.canRemove(player, grant.asRank()))
                return;

            player.closeInventory();
            new GrantRemoveInput(target, grant).send(player);
        }
    }
}
