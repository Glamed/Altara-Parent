package games.sparking.altara.command;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.updater.FileUpdater;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.Properties;

public class BuildVersionCommand {

    @Command(names = {"bversion", "bv"}, description = "Displays the current build version info", permission = "op")
    public void bversion(CommandSender caller) {
        Properties buildProperties = FileUpdater.getBuildProperties();

        String git  = buildProperties.getProperty("build.git",  "Unknown");
        String date = buildProperties.getProperty("build.date", "Unknown");
        String user = buildProperties.getProperty("build.user", "Unknown");

        caller.sendMessage(CC.RED + CC.BOLD + "Build Version;");
        caller.sendMessage("  " + CC.GOLD + "Date " + CC.WHITE + date);
        caller.sendMessage("  " + CC.GOLD + "User " + CC.WHITE + user);
        caller.sendMessage("  " + CC.GOLD + "Git  " + CC.WHITE + git);
    }
}

