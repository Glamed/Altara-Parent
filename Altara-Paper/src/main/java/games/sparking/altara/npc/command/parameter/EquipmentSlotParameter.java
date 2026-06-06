package games.sparking.altara.npc.command.parameter;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.npc.equipment.EquipmentSlot;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EquipmentSlotParameter implements ParameterType<EquipmentSlot> {

    @Override
    public EquipmentSlot parse(CommandSender sender, String source) {
        try {
            return EquipmentSlot.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            sender.sendMessage(CC.format(
                    "<red>Slot <yellow>%s <red>not found. Available: <yellow>%s",
                    source,
                    Arrays.stream(EquipmentSlot.values())
                          .map(Enum::name)
                          .collect(Collectors.joining("<red>, <yellow>"))));
            return null;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return Arrays.stream(EquipmentSlot.values())
                     .map(s -> s.name().toLowerCase())
                     .collect(Collectors.toList());
    }
}
