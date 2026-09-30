package games.sparking.altara.command.permission.defaults;

import games.sparking.altara.command.permission.PermissionAdapter;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class PlayerOnlyPermission extends PermissionAdapter {

    public PlayerOnlyPermission() {
        super("player");
    }

    public boolean test(CommandSender sender) {
        boolean b = testSilent(sender);
        if (!b)
            sender.sendMessage(CC.error(Messages.PLAYERS_ONLY));
        return b;
    }

    @Override
    public boolean testSilent(CommandSender sender) {
        return sender instanceof Player;
    }
}
