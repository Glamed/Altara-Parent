package games.sparking.altara.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.updater.FileUpdater;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import org.bukkit.command.CommandSender;

import java.util.Properties;

public class BuildVersionCommand {

    @Command(names = {"bversion", "bv"}, description = "View the current build version", permission = "op")
    public void bversion(CommandSender caller) {
        Properties buildProperties = FileUpdater.getBuildProperties();

        caller.sendMessage(CC.header(Panel.DEV, "Build"));
        caller.sendMessage(CC.item("Date", buildProperties.getProperty("build.date", "Unknown")));
        caller.sendMessage(CC.item("User", buildProperties.getProperty("build.user", "Unknown")));
        caller.sendMessage(CC.item("Commit", buildProperties.getProperty("build.git", "Unknown")));
        caller.sendMessage(CC.footer(Panel.DEV));
    }
}
