package games.sparking.altara.command;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.updater.FileUpdater;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.util.Properties;

public class BuildVersionCommand {

    @Command(names = {"bversion", "bv"}, description = "Displays the current build version info", permission = "op")
    public void bversion(CommandSender caller) {
        Properties buildProperties = FileUpdater.getBuildProperties();

        String git  = buildProperties.getProperty("build.git",  "Unknown");
        String date = buildProperties.getProperty("build.date", "Unknown");
        String user = buildProperties.getProperty("build.user", "Unknown");

        caller.sendMessage(Component.text("Build Version", NamedTextColor.RED, TextDecoration.BOLD));
        caller.sendMessage(Component.text()
                .append(Component.text("  Date ", NamedTextColor.GOLD))
                .append(Component.text(date, NamedTextColor.WHITE)));
        caller.sendMessage(Component.text()
                .append(Component.text("  User ", NamedTextColor.GOLD))
                .append(Component.text(user, NamedTextColor.WHITE)));
        caller.sendMessage(Component.text()
                .append(Component.text("  Git  ", NamedTextColor.GOLD))
                .append(Component.text(git, NamedTextColor.WHITE)));
    }
}
