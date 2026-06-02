package games.sparking.altara.npc.config;

import games.sparking.altara.configuration.StaticConfiguration;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.npc.equipment.EquipmentSlot;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class NPCConfigEntry implements StaticConfiguration {

    private LocationConfig location;
    private String name;
    private String displayName;
    private String texture;
    private String signature;
    private String command;
    private boolean consoleCommand;
    private Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

}
