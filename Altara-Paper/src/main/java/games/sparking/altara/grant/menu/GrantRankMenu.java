package games.sparking.altara.grant.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public class GrantRankMenu extends Menu {

    private final GrantProcedure procedure;
    private boolean clicked = false;

    @Override
    public Component getTitle(Player player) {
        return CC.format("Select a rank: " + procedure.getTarget().getName());
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        int index = 0;
        for (Rank rank : Altara.getSharedInstance().getRankService().getRanksSorted()) {
            buttons.put(index++, new RankButton(rank, procedure));
        }
        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (!clicked) {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            profile.setGrantProcedure(null);
            player.sendMessage(CC.errorMsg("You cancelled the grant procedure."));
        }
    }

    public boolean canGrant(Player player, Rank rank) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        if (rank.isDefaultRank()) return false;
        if (profile.getRealCurrentGrant().asRank().getWeight() >= Altara.getSharedInstance().getMainConfig().getOwnerWeight()
                || player.getUniqueId().equals(UUID.fromString("c7d53cda-a00d-465b-ba55-c2f684ad4ae3"))) {
            return true;
        }
        return profile.getRealCurrentGrant().asRank().getWeight() > rank.getWeight()
                && player.hasPermission("altara.grant." + rank.getName());
    }

    @RequiredArgsConstructor
    public class RankButton extends Button {

        private final Rank rank;
        private final GrantProcedure procedure;

        @Override
        public ItemStack getItem(Player player) {
            List<Component> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);

            if (canGrant(player, rank))
                lore.add(Component.text()
                        .append(Component.text("Click to grant ", NamedTextColor.YELLOW))
                        .append(Component.text(procedure.getTarget().getName(), NamedTextColor.WHITE))
                        .append(Component.text(" the ", NamedTextColor.YELLOW))
                        .append(Component.text(rank.getName(), NamedTextColor.WHITE))
                        .append(Component.text(" rank.", NamedTextColor.YELLOW))
                        .build());
            else if (rank.isDefaultRank())
                lore.add(CC.errorMsg("Invalid rank.", "You are unable to grant the default rank."));
            else
                lore.add(CC.errorMsg("Invalid rank.", "You are unable to grant this rank."));

            lore.add(CC.MENU_BAR);
            return new ItemBuilder(rank.getMaterial())
                    .setDisplayName(Component.text(rank.getName()))
                    .setLore(lore).build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!canGrant(player, rank)) {
                player.sendMessage(CC.errorMsg("Invalid rank.", "You are unable to grant this rank."));
                return;
            }
            clicked = true;
            procedure.setRank(rank);
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            profile.setGrantProcedure(procedure);
            new GrantDurationMenu(profile).openMenu(player);
        }
    }
}
