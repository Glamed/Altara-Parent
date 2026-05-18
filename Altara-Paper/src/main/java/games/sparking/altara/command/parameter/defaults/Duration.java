package games.sparking.altara.command.parameter.defaults;


import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class Duration {

    private long duration;
    private String source;
    private boolean permanent;

    public static class Type implements ParameterType<Duration> {

        public Duration parse(CommandSender sender, String source) {
            if (source.equalsIgnoreCase("perm") || (source.equalsIgnoreCase("permanent"))) {
                return new Duration(-1, source, true);
            }

            long parsed = Time.parseTime(source);
            if (parsed == -1) {
                sender.sendMessage(CC.errorMsg("Invalid arguments.", source + " is not a valid duration."));
                return null;
            }

            return new Duration(parsed, source, false);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, List<String> flags) {
            return Collections.emptyList();
        }
    }
}