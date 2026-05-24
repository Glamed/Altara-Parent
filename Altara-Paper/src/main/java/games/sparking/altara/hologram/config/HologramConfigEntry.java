package games.sparking.altara.hologram.config;

import games.sparking.altara.configuration.StaticConfiguration;
import games.sparking.altara.configuration.defaults.SimpleLocationConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HologramConfigEntry implements StaticConfiguration {

    private SimpleLocationConfig location;
    private String name;
    private double spacing;
    private List<String> lines;

}