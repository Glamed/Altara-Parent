package games.sparking.altara.hologram.statics;

import games.sparking.altara.configuration.defaults.SimpleLocationConfig;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.HologramLine;
import games.sparking.altara.hologram.config.HologramConfigEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class StaticHologram extends Hologram {

    @Getter @Setter
    private String name;

    private final List<HologramLine> lines = new ArrayList<>();

    protected StaticHologram(StaticHologramBuilder builder) {
        super(builder);
        for (String line : builder.getLines())
            lines.add(new HologramLine(line));
    }

    @Override
    public List<HologramLine> getLines() {
        return lines;
    }

    public void addLines(String... newLines) {
        for (String line : newLines)
            lines.add(new HologramLine(line));
        update();
    }

    public void setLine(int position, String text) {
        destroy();
        if (position >= lines.size())
            lines.add(new HologramLine(text));
        else
            lines.set(position, new HologramLine(text));
        spawn();
    }

    public void setLines(Iterable<String> newLines) {
        lines.clear();
        for (String line : newLines)
            lines.add(new HologramLine(line));
        update();
    }

    public List<HologramLine> getCurrentLines() {
        return lines;
    }

    public HologramConfigEntry toConfig() {
        List<String> texts = new ArrayList<>();
        for (HologramLine line : lines)
            texts.add(line.getText());
        return new HologramConfigEntry(new SimpleLocationConfig(getLocation()), name, getLineSpacing(), texts);
    }
}
