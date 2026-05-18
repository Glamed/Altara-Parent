package games.sparking.altara.gamemode;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.utils.CC;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand {


    @Command(names = {"gamemode", "gm"},
            permission = "invictus.command.gamemode",
            description = "Set the gamemode of a player")
    public boolean gamemode(CommandSender sender,
                            @Param(name = "mode", defaultValue = "@toggle") GameMode mode,
                            @Param(name = "player", defaultValue = "@self") Player target) {
        execute(sender, target, mode);
        return true;
    }

    @Command(names = {"gms", "gm0"},
            permission = "command.gamemode",
            description = "Set the gamemode of a player to survival")
    public boolean gms(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.SURVIVAL);
    }

    @Command(names = {"gmc", "gm1"},
            permission = "command.gamemode",
            description = "Set the gamemode of a player to creative")
    public boolean gmc(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.CREATIVE);
    }

    @Command(names = {"gma", "gm2"},
            permission = "invictus.command.gamemode",
            description = "Set the gamemode of a player to adventure")
    public boolean gma(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.ADVENTURE);
    }

    @Command(names = {"gmsp", "gm3"},
            permission = "invictus.command.gamemode",
            description = "Set the gamemode of a player to spectator")
    public boolean gmsp(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.SPECTATOR);
    }

    private boolean execute(CommandSender sender, Player target, GameMode mode) {
        if ((!sender.equals(target)) && (!sender.hasPermission("command.gamemode.other"))) {
            sender.sendMessage(CC.errorMsg("You are not allowed to change the gamemode of other players."));
            return false;
        }

        target.setGameMode(mode);
        String subject = sender.equals(target) ? "Your" : "*" + target.getName() + "'s*";
        sender.sendMessage(CC.noticeMsg("Gamemode updated.", subject + " gamemode is now *" + WordUtils.capitalizeFully(mode.name()) + "*."));
        return true;
    }
}
