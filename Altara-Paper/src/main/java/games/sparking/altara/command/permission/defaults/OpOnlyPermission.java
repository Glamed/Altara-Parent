package games.sparking.altara.command.permission.defaults;

import games.sparking.altara.command.permission.PermissionAdapter;
import org.bukkit.command.CommandSender;

public class OpOnlyPermission extends PermissionAdapter {

    public OpOnlyPermission() {
        super("op");
    }

    @Override
    public boolean testSilent(CommandSender sender) {
        return sender.isOp();
    }
}
