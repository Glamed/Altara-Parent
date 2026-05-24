package games.sparking.altara.npc.config;

import games.sparking.altara.configuration.StaticConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class NPCConfig implements StaticConfiguration {

    private List<NPCConfigEntry> npcs = new ArrayList<>();

}