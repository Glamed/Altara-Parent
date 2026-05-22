package games.sparking.altara.rank.parameter;


import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.command.parameter.ParameterType;
import games.sparking.blazora.rank.Rank;
import games.sparking.blazora.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class RankParameter implements ParameterType<Rank> {

    private static final BlazoraPaper zircon = BlazoraPaper.getPaperInstance();

    @Override
    public Rank parse(CommandSender sender, String source) {
        if (source.equals("@default")) {
            return zircon.getRankService().getDefaultRank();
        }
        Rank rank = zircon.getRankService().getRank(source);
        if (rank == null) {
            sender.sendMessage(CC.format("&cRank &e%s &cnot found.", source));
            return null;
        }
        return rank;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (Rank rank : zircon.getRankService().getRanksSorted()) {
            completions.add(rank.getName());
        }
        return completions;
    }
}
