package games.sparking.altara.rank.setup;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;

import java.util.UUID;

public class WeightPrompt extends ChatInput<Integer> {

    public WeightPrompt() {
        super(Integer.class);

        text(
                CC.noticeMsg("", "Please enter the weight for this rank"),
                CC.noticeMsg("", "You can type *cancel* at any time to exit this process.")
        );
        escapeMessage(CC.errorMsg("You cancelled further rank setup."));

        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
            Rank rank = rankId == null ? null : Altara.getSharedInstance().getRankService().getRank(rankId);
            if (rank == null) {
                player.sendMessage(CC.RED + "The rank you were setting up no longer exists.");
                return true;
            }

            RankCommands.INSTANCE.rankSetWeight(player, rank, input);
            // next: queue priority
            return true;
        });
    }
}
