package games.sparking.altara.gamemode;

import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.utils.CC;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand {

    public static final String PERMISSION = "altara.command.gamemode";
    public static final String OTHERS_PERMISSION = "altara.command.gamemode.others";

    @Command(names = {"gamemode", "gm"}, permission = PERMISSION, description = "Change a game mode")
    public boolean gamemode(CommandSender sender,
                            @Param(name = "mode", defaultValue = "@toggle") GameMode mode,
                            @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, mode);
    }

    @Command(names = {"gms", "gm0"}, permission = PERMISSION, description = "Switch to survival")
    public boolean gms(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.SURVIVAL);
    }

    @Command(names = {"gmc", "gm1"}, permission = PERMISSION, description = "Switch to creative")
    public boolean gmc(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.CREATIVE);
    }

    @Command(names = {"gma", "gm2"}, permission = PERMISSION, description = "Switch to adventure")
    public boolean gma(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.ADVENTURE);
    }

    @Command(names = {"gmsp", "gm3"}, permission = PERMISSION, description = "Switch to spectator")
    public boolean gmsp(CommandSender sender, @Param(name = "player", defaultValue = "@self") Player target) {
        return execute(sender, target, GameMode.SPECTATOR);
    }

    private boolean execute(CommandSender sender, Player target, GameMode mode) {
        if (target == null) {
            sender.sendMessage(CC.error("Invalid player.", "Name a player to change their game mode."));
            return false;
        }
        if (!sender.equals(target) && !sender.hasPermission(OTHERS_PERMISSION)) {
            sender.sendMessage(CC.error("No permission.", "You can't change other players' game modes."));
            return false;
        }

        target.setGameMode(mode);
        String modeName = StringUtils.capitalize(mode.name().toLowerCase());
        if (sender.equals(target)) {
            sender.sendMessage(CC.success("Game mode updated.", "You're now in *" + modeName + "*."));
        } else {
            sender.sendMessage(CC.success("Game mode updated.", "*" + target.getName() + "* is now in *" + modeName + "*."));
            target.sendMessage(CC.info("Your game mode was changed to *" + modeName + "*."));
        }
        return true;
    }
}
