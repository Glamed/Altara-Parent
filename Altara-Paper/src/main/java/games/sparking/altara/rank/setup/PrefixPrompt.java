package games.sparking.altara.rank.setup;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PrefixPrompt extends ChatInput<String> {


    public PrefixPrompt() {
        super(String.class);

        text("<yellow>Please enter the prefix for this rank, or type <red>cancel</red> to cancel.");
        escapeMessage("<red>You cancelled the further rank setup.");
        onCancel(player -> RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId()));

        accept((player, input) -> {
            UUID rankId = RankEditingMenu.RANK_SETUPS.get(player.getUniqueId());
            Rank rank = rankId == null ? null : Altara.getSharedInstance().getRankService().getRank(rankId);
            if (rank == null) {
                player.sendMessage(CC.translate("<red>The rank you were setting up no longer exists."));
                return true;
            }

            RankCommands.INSTANCE.rankSetPrefix(player, rank, input);
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

        String suggested = "<gray>[" + rank.getName() + "<gray>] ";
        player.sendMessage(CC.translate("<yellow>(Click to get the suggested prefix example)")
                .clickEvent(ClickEvent.suggestCommand(suggested)));
    }
}
