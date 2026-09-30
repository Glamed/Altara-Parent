package games.sparking.altara.command.parameter.defaults;

import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.EnchantmentWrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;

import java.util.List;

public class EnchantmentParameter implements ParameterType<Enchantment> {

    @Override
    public Enchantment parse(CommandSender sender, String source) {
        Enchantment enchantment = EnchantmentWrapper.resolve(source);
        if (enchantment == null) {
            sender.sendMessage(CC.error("Invalid enchantment.", "*" + source + "* doesn't exist."));
        }
        return enchantment;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        return EnchantmentWrapper.completions();
    }
}
