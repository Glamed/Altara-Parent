package games.sparking.altara.hologram.config;

import games.sparking.altara.configuration.StaticConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class HologramConfig implements StaticConfiguration {

    private List<HologramConfigEntry> holograms = new ArrayList<>();

}