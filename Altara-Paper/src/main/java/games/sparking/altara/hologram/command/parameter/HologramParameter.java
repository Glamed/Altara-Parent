package games.sparking.altara.hologram.command.parameter;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.hologram.HologramService;
import games.sparking.altara.hologram.statics.StaticHologram;
import games.sparking.altara.utils.CC;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class HologramParameter implements ParameterType<StaticHologram> {

    private final HologramService hologramService;

    @Override
    public StaticHologram parse(CommandSender sender, String source) {
        // Try name first
        StaticHologram hologram = hologramService.getHologram(source);
        if (hologram != null) return hologram;

        // Then try integer ID
        Integer id = CommandService.getParameter(Integer.class).parse(sender, source);
        if (id == null) {
            sender.sendMessage(CC.format("<red>Hologram '<yellow>%s<red>' not found.", source));
            return null;
        }

        hologram = hologramService.getHologram(id);
        if (hologram == null)
            sender.sendMessage(CC.format("<red>Hologram with id <yellow>%d <red>not found.", id));
        return hologram;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (StaticHologram hologram : hologramService.getSerializedHolograms()) {
            if (hologram.getName() != null)
                completions.add(hologram.getName());
            else
                completions.add(String.valueOf(hologram.getId()));
        }
        return completions;
    }
}
