package games.sparking.altara.command.permission;

import games.sparking.altara.command.CommandService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
@Getter
public abstract class PermissionAdapter {

    private final String permission;

    public boolean test(CommandSender sender) {
        boolean b = testSilent(sender);
        if (!b)
            sender.sendMessage(CommandService.NO_PERMISSION_MESSAGE);
        return b;
    }

    public abstract boolean testSilent(CommandSender sender);

}
