package games.sparking.altara;

import games.sparking.altara.command.annotation.Command;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AltaraCommand {

    @Command(names = "test", description = "A test command")
    public void testCommand(Player sender) {
        sender.sendMessage(Component.text("Altara test command"));
    }
}
