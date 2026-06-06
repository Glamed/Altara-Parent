package games.sparking.altara.rank.setup;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;

import java.util.UUID;

public class ColorPrompt extends ChatInput<String> {

    public ColorPrompt() {
        super(String.class);
        text(
                CC.noticeMsg("", "Please enter the color for this rank"),
                CC.noticeMsg("", "You can type *cancel* at any time to exit this process.")
        );
        escapeMessage(CC.errorMsg("You cancelled the further rank setup."));
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            if (input.contains(" ")) {
                player.sendMessage(CC.errorMsg("Invalid color.", "The color cannot contain a white space."));
                return false;
            }

            UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
            Rank rank = rankId == null ? null : Altara.getSharedInstance().getRankService().getRank(rankId);
            if (rank == null) {
                player.sendMessage(CC.errorMsg("Invalid rank.", "The rank you were setting up no longer exists."));
                return true;
            }

            RankCommands.INSTANCE.rankSetColor(player, rank, input);
            // next: prefix
            return true;
        });
    }
}
