package games.sparking.altara.reboot;


import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.command.parameter.defaults.Duration;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class RebootCommands {

    @Command(names = {"reboot"},
             permission = "command.reboot",
             description = "Start the reboot timer")
    public boolean reboot(CommandSender sender, @Param(name = "duration") Duration duration) {
        if (duration.isPermanent()) {
            sender.sendMessage(CC.RED + "Cannot start permanent reboot task.");
            return false;
        }

        if (RebootService.isRebooting()) {
            sender.sendMessage(CC.RED + "Reboot already in progress.");
            return false;
        }

        RebootService.reboot(duration.getDuration());
        sender.sendMessage(CC.format("&9Rebooting in &e%s&9.", Time.formatDetailed(duration.getDuration())));
        return true;
    }

    @Command(names = {"reboot now"},
            permission = "command.reboot",
            description = "Cancel the reboot timer")
    public boolean now(CommandSender sender) {
        Bukkit.getServer().restart();

        return true;
    }


    @Command(names = {"reboot cancel"},
             permission = "command.reboot",
             description = "Cancel the reboot timer")
    public boolean cancel(CommandSender sender) {
        if (!RebootService.isRebooting()) {
            sender.sendMessage(CC.RED + "Not rebooting.");
            return false;
        }

        RebootService.cancel();
        return true;
    }

}
