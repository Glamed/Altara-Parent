package games.sparking.altara.rank.setup;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

import java.util.UUID;

public class QueuePriorityPrompt extends ChatInput<Integer> {

    public QueuePriorityPrompt() {
        super(Integer.class);

        text(CC.translate("&ePlease enter the queue priority for this rank, or type &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the further rank setup.");
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
            Rank rank = rankId == null ? null : Altara.getSharedInstance().getRankService().getRank(rankId);
            if (rank == null) {
                player.sendMessage(CC.RED + "The rank you were setting up no longer exists.");
                return true;
            }

            RankCommands.INSTANCE.rankSetQueuePriority(player, rank, input);
            return true;
        });
    }

    @Override
    public void send(Player player) {
        super.send(player);

        UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
        Rank rank = rankId == null ? null : Altara.getSharedInstance().getRankService().getRank(rankId);

        if (rank == null)
            return;

        new ChatMessage(CC.format(
                "&e(Click to get the suggested %d)",
                rank.getWeight()
        )).suggestCommand(String.valueOf(rank.getWeight())).send(player);
    }
}
