package games.sparking.altara.rank.menu;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInputChain;
import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.page.PagedMenu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.setup.*;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/** {@code /rank edit}: every rank, plus a button to set up a new one through chat. */
public class RankEditOverviewMenu extends PagedMenu {

    private static final ChatInputChain SETUP_CHAIN = new ChatInputChain()
            .next(new NamePrompt())
            .next(new ColorPrompt())
            .next(new PrefixPrompt())
            .next(new WeightPrompt())
            .next(new QueuePriorityPrompt());

    private final Profile profile;

    public RankEditOverviewMenu(Profile profile) {
        this.profile = profile;
    }

    @Override
    public String[] getBreadcrumb(Player player) {
        return new String[]{"Ranks"};
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        Altara.getSharedInstance().getRankService().getRanksSorted()
                .forEach(rank -> buttons.put(buttons.size(), new RankButton(rank)));
        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        return Map.of(49, new SetupRankButton());
    }

    /** Lore lines describing a rank, shared with {@link RankEditingMenu}. */
    static Gui.Lore summary(Rank rank) {
        return Gui.lore()
                .value("Prefix", CC.format(rank.getPrefix() + rank.getColor() + "Example"))
                .value("Chat", CC.format(rank.getChatColor() + "Example"))
                .value("Weight", String.valueOf(rank.getWeight()))
                .value("Queue priority", String.valueOf(rank.getQueuePriority()))
                .value("Default", CC.state(rank.isDefaultRank(), "Yes", "No"))
                .value("Inherits", rank.getInherits().isEmpty() ? "None"
                        : String.join(", ", rank.getInherits().stream().map(Rank::getName).toList()))
                .value("Permissions", rank.getPermissions().size() + " (+" + rank.getLocalPermissions().size() + " local)");
    }

    private class RankButton extends Button {

        private final Rank rank;

        RankButton(Rank rank) {
            this.rank = rank;
        }

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(rank.getMaterial())
                    .setDisplayName(CC.format(rank.getDisplayName()))
                    .setLore(summary(rank).cta("edit this rank").build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            new RankEditingMenu(profile, rank).openMenu(player);
        }
    }

    private static class SetupRankButton extends Button {

        @Override
        public ItemStack getItem(Player player) {
            return new ItemBuilder(Material.EMERALD)
                    .setDisplayName(Gui.name("New", "Rank"))
                    .setLore(Gui.lore()
                            .text("Create a rank and set its", "color, prefix and weight in chat.")
                            .cta("set up a new rank")
                            .build())
                    .build();
        }

        @Override
        public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
            player.closeInventory();
            SETUP_CHAIN.start(player);
        }
    }
}
