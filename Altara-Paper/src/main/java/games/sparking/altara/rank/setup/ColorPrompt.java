package games.sparking.altara.rank.setup;


import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.chatinput.ChatInput;
import games.sparking.blazora.rank.Rank;
import games.sparking.blazora.rank.commands.RankCommands;
import games.sparking.blazora.rank.menu.RankEditingMenu;
import games.sparking.blazora.utils.CC;

import java.util.UUID;

public class ColorPrompt extends ChatInput<String> {

    public ColorPrompt(BlazoraPaper zircon) {
        super(String.class);
        text(CC.translate("&ePlease enter the color for this rank, or type &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the further rank setup.");
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            if (input.contains(" ")) {
                player.sendMessage(CC.RED + "The color cannot contain a white space.");
                return false;
            }

            UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
            Rank rank = rankId == null ? null : zircon.getRankService().getRank(rankId);
            if (rank == null) {
                player.sendMessage(CC.RED + "The rank you were setting up no longer exists.");
                return true;
            }

            RankCommands.INSTANCE.rankSetColor(player, rank, input);
            // next: prefix
            return true;
        });
    }
}
