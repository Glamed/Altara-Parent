package games.sparking.altara.rank.setup;

import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;

public class NamePrompt extends RankSetupPrompt<String> {

    public NamePrompt() {
        super(String.class, "Name the rank.", "Use letters, numbers, dashes or underscores.", null);
        accept((player, input) -> {
            Rank rank = RankCommands.createRank(player, input);
            if (rank == null) return false;

            RankEditingMenu.RANK_SETUPS.put(player.getUniqueId(), rank.getUuid());
            return true;
        });
    }
}
