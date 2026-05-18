package games.sparking.altara.command.permission.defaults;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.command.permission.PermissionAdapter;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;


public class ConsoleOnlyPermission extends PermissionAdapter {

    public ConsoleOnlyPermission() {
        super("console");
    }

    public boolean test(CommandSender sender) {
        boolean b = testSilent(sender);
        if (!b) {
            if (sender.isOp())
                sender.sendMessage(CC.errorMsg(Messages.UNKNOWN_COMMAND));
            else sender.sendMessage(CommandService.NO_PERMISSION_MESSAGE);
        }
        return b;
    }

    @Override
    public boolean testSilent(CommandSender sender) {
        return sender instanceof ConsoleCommandSender;
    }
}
