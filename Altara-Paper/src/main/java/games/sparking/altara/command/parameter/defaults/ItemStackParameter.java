package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class ItemStackParameter implements ParameterType<ItemStack> {

    @Override
    public ItemStack parse(CommandSender sender, String source) {
        Material material = Material.matchMaterial(source);
        if (material == null) {
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "Item " + source + " not found."));
            return null;
        }
        return new ItemStack(material);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return Arrays.stream(Material.values())
                .map(m -> m.name().toLowerCase())
                .toList();
    }
}