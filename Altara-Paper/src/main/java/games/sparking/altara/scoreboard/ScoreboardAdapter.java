package games.sparking.altara.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.List;

public interface ScoreboardAdapter {

    Component getTitle(Player player);

    List<Component> getLines(Player player);

}