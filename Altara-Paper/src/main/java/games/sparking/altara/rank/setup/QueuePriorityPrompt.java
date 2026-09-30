package games.sparking.altara.rank.setup;

import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.commands.RankCommands;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public class QueuePriorityPrompt extends RankSetupPrompt<Integer> {

    public QueuePriorityPrompt() {
        super(Integer.class, "Choose a queue priority.", "Higher priorities are moved up in queues.",
                (player, rank, input) -> {
                    RankCommands.INSTANCE.rankSetQueuePriority(player, rank, input);
                    RankEditingMenu.RANK_SETUPS.remove(player.getUniqueId());
                    player.sendMessage(CC.success("Rank set up.", "Use */rank edit " + rank.getName() + "* to add permissions."));
                    return true;
                });
    }

    @Override
    public void send(Player player) {
        super.send(player);

        Rank rank = setupRank(player);
        if (rank == null) return;

        player.sendMessage(CC.info("Most ranks use their weight here. ")
                .append(CC.action("Click to use " + rank.getWeight() + ".",
                        ClickEvent.suggestCommand(String.valueOf(rank.getWeight())))));
    }
}
