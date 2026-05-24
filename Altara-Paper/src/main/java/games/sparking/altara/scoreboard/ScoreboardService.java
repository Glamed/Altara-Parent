package games.sparking.altara.scoreboard;

import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardService {

    public static Map<UUID, Scoreboard> scoreboards = new HashMap<>();

    @Getter
    private final ScoreboardAdapter adapter;


    public ScoreboardService(ScoreboardAdapter adapter) {
        this.adapter = adapter;
        startScoreboardUpdater();
    }


    public void startScoreboardUpdater() {
        Tasks.runTimer(() -> {
            for (Scoreboard scoreboard : scoreboards.values()) {
                scoreboard.setTitle(CC.translate(getAdapter().getTitle(scoreboard.getPlayer())));
                scoreboard.update();
            }
        }, 0, 3);
        Tasks.runTimer(() -> {
            for (Scoreboard scoreboard : scoreboards.values()) {
                List<String> lines = getAdapter().getLines(scoreboard.getPlayer());

                int maxLines = 15; // your scoreboard array size

                while (lines.size() > maxLines) {
                    lines.remove(lines.size() - 1);
                }

                int currentLine = 0;
                boolean lastWasBlank = false;

                for (String line : lines) {
                    boolean isBlank = line.trim().isEmpty();

                    if (isBlank) {
                        if (lastWasBlank) {
                            continue;
                        }
                        lastWasBlank = true;
                    } else {
                        lastWasBlank = false;
                    }

                    // avoid writing beyond scoreboard capacity
                    if (currentLine >= maxLines) {
                        break;
                    }

                    scoreboard.setLeftAlignedText(currentLine++, CC.translate(line));
                }

                // clear leftovers
                for (int i = currentLine; i < maxLines; i++) {
                    scoreboard.setLeftAlignedText(i, null);
                }

                scoreboard.update();
            }
        }, 0, 20L);
    }


}
