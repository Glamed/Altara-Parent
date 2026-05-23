package games.sparking.altara.rank.parameter;


import games.sparking.altara.Altara;
import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class RankParameter implements ParameterType<Rank> {

    @Override
    public Rank parse(CommandSender sender, String source) {
        if (source.equals("@default")) {
            return Altara.getSharedInstance().getRankService().getDefaultRank();
        }
        Rank rank = Altara.getSharedInstance().getRankService().getRank(source);
        if (rank == null) {
            sender.sendMessage(CC.format("&cRank &e%s &cnot found.", source));
            return null;
        }
        return rank;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (Rank rank : Altara.getSharedInstance().getRankService().getRanksSorted()) {
            completions.add(rank.getName());
        }
        return completions;
    }
}
