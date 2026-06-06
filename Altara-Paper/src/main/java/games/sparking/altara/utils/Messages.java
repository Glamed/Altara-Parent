package games.sparking.altara.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Messages {

    MODULE("This system is currently disabled.", ""),
    CONNECTED("Invalid player.", "That player has never been on Sparking before."),
    COMMAND("No Permission.", "You don't have permission to use this command."),
    PLAYER_OFFLINE("Offline player.", "This player is offline or on a different server."),
    REBOOT("Realm rebooting.", "This command is disabled before a reboot."),
    WORLD("Invalid world.", "This command is not available in %s."),
    SERVER_FEATURE("Invalid realm.", "This feature is only available on %s."),
    SERVER_EXCLUDED("Invalid realm.", "This feature is not available on %s."),
    COOLDOWN("Command on cooldown.", "You must wait %s."),
    UNKNOWN_COMMAND("Unknown command.", "Type /help for assistance."),
    SERVER_UNAVAILABLE("Invalid Server.", "*%s* is currently unavailable."),
    SERVER_OFFLINE("Invalid Server.", "%s is currently offline."),
    PERMISSION("No Permission.", "You don't have permission to use this command.");

    private final String reason;
    private final String main;

}