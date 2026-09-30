package games.sparking.altara.grant.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.grant.GrantPermissions;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Step 1 of {@code /grant}: pick the rank. */
public class GrantRankMenu extends PagedMenu {

    private final GrantProcedure procedure;
    private boolean continued = false;

    public GrantRankMenu(GrantProcedure procedure) {
        this.procedure = procedure;
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Grant", procedure.getTarget().getName()};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        List<Rank> ranks = Altara.getSharedInstance().getRankService().getRanksSorted();
        for (Rank rank : ranks) {
            buttons.put(buttons.size(), new RankButton(rank));
        }
        return buttons;
    }

    @Override
    public void onClose(Player player) {
        if (!continued) {
            procedure.getProfile().setGrantProcedure(null);
            player.sendMessage(CC.info("Grant cancelled."));
        }
    }

    private class RankButton extends Button {

        private final Rank rank;

        RankButton(Rank rank) {
            this.rank = rank;
        }

        @Override
        public ItemStack getItem(Player player) {
            Gui.Lore lore = Gui.lore()
                    .value("Weight", String.valueOf(rank.getWeight()));

            if (GrantPermissions.canGrant(player, rank)) {
                lore.cta("grant " + rank.getName() + " to " + procedure.getTarget().getName());
            } else {
                lore.blank()
                        .line(Component.text(Theme.CROSS + " ", Theme.TEXT)
                                .append(Component.text("Unavailable", Theme.ERROR)))
                        .text(rank.isDefaultRank()
                                ? "Everyone already has the default rank."
                                : "You can't grant this rank.");
            }

            return new ItemBuilder(rank.getMaterial())
                    .setDisplayName(CC.format(rank.getDisplayName()))
                    .setLore(lore.build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            if (!GrantPermissions.canGrant(player, rank)) return;

            continued = true;
            procedure.setRank(rank);
            procedure.getProfile().setGrantProcedure(procedure);
            new GrantDurationMenu(procedure.getProfile()).openMenu(player);
        }
    }
}
