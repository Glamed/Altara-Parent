package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.EnchantmentWrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentParameter implements ParameterType<Enchantment> {

    @Override
    public Enchantment parse(CommandSender sender, String source) {
        EnchantmentWrapper enchantment = EnchantmentWrapper.fromString(source);
        if (enchantment == null) {
            sender.sendMessage(CC.errorMsg("Invalid arguments.", "Enchantment " + source + " was not found."));
            return null;
        }
        return enchantment.toBukkitEnchant();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (EnchantmentWrapper value : EnchantmentWrapper.values()) {
            completions.add(value.name().toLowerCase());
        }
        return completions;
    }
}
