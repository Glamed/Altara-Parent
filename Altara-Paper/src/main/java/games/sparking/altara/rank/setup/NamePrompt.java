package games.sparking.altara.rank.setup;

import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;

public class NamePrompt extends ChatInput<String> {

    public NamePrompt() {
        super(String.class);
        text(
                CC.noticeMsg("", "Please enter the name for the rank"),
                CC.noticeMsg("", "You can type *cancel* at any time to exit this process.")
        );
        escapeMessage(CC.errorMsg("You cancelled the rank creation."));
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            if (input.contains(" ")) {
                player.sendMessage(CC.RED + "The name cannot contain a white space.");
                return false;
            }

            Rank rank = RankCommands.createRank(player, input);
            if (rank != null)
                RankEditingMenu.RANK_SETUPS.put(player.getUniqueId(), rank.getUuid());
            // next: color
            return true;
        });
    }
}
