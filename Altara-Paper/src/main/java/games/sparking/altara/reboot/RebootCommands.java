package games.sparking.altara.reboot;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.command.parameter.defaults.Duration;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Time;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Header(value = "Reboot", panel = Panel.STAFF)
public class RebootCommands {

    private static final long CONFIRM_WINDOW = TimeUnit.SECONDS.toMillis(15);
    private final Map<String, Long> pendingConfirmations = new ConcurrentHashMap<>();

    @Command(names = {"reboot"}, permission = "command.reboot", description = "Schedule a reboot")
    public boolean reboot(CommandSender sender, @Param(name = "delay") Duration duration) {
        if (duration.isPermanent()) {
            sender.sendMessage(CC.error("Invalid delay.", "Choose a delay like *5m*."));
            return false;
        }
        if (RebootService.isRebooting()) {
            sender.sendMessage(CC.error("Reboot scheduled.", "Use */reboot cancel* first to change it."));
            return false;
        }

        RebootService.reboot(duration.getDuration());
        sender.sendMessage(CC.success("Reboot scheduled.", "This realm reboots in *" + Time.formatDetailed(duration.getDuration()) + "*."));
        return true;
    }

    @Command(names = {"reboot now"}, permission = "command.reboot", description = "Reboot immediately")
    public boolean now(CommandSender sender) {
        Long requested = pendingConfirmations.remove(sender.getName());
        if (requested == null || System.currentTimeMillis() - requested > CONFIRM_WINDOW) {
            pendingConfirmations.put(sender.getName(), System.currentTimeMillis());
            CC.confirmation("reboot this realm immediately",
                    "Everyone online is disconnected without warning").forEach(sender::sendMessage);
            return true;
        }

        Bukkit.getServer().restart();
        return true;
    }

    @Command(names = {"reboot cancel"}, permission = "command.reboot", description = "Cancel a scheduled reboot")
    public boolean cancel(CommandSender sender) {
        if (!RebootService.isRebooting()) {
            sender.sendMessage(CC.error("No reboot scheduled.", "This realm isn't rebooting."));
            return false;
        }

        RebootService.cancel();
        sender.sendMessage(CC.success("Reboot cancelled."));
        return true;
    }
}
