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
            sender.sendMessage(CC.errorMsg("Invalid duration.", "Reboots can not be permanent."));
            return false;
        }

        if (RebootService.isRebooting()) {
            sender.sendMessage(CC.errorMsg("Invalid request.", "Server reboot is already in progress."));
            return false;
        }

        RebootService.reboot(duration.getDuration());
        sender.sendMessage(CC.noticeMsg("", "Server will reboot in *" + Time.formatDetailed(duration.getDuration()) + "*."));
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
            sender.sendMessage(CC.errorMsg("Invalid Request.", "This server is Not rebooting."));
            return false;
        }

        RebootService.cancel();
        return true;
    }

}
