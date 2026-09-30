package games.sparking.altara.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Shared error copy used by {@link CC#error(Messages, Object...)}.
 * {@code main} may contain {@code %s} placeholders and {@code *emphasis*}.
 */
@Getter
@AllArgsConstructor
public enum Messages {

    MODULE("Feature disabled.", "This feature is currently unavailable."),
    NEVER_JOINED("Invalid player.", "That player has never joined Sparking."),
    PLAYER_NOT_FOUND("Invalid player.", "That player could not be found."),
    PLAYER_OFFLINE("Invalid player.", "That player is offline or on another realm."),
    PERMISSION("No permission.", "You don't have permission to do that."),
    REBOOT("Realm rebooting.", "This command is unavailable while the realm reboots."),
    WORLD("Invalid world.", "This command isn't available in *%s*."),
    REALM_ONLY("Invalid realm.", "This feature is only available on *%s*."),
    REALM_EXCLUDED("Invalid realm.", "This feature isn't available on *%s*."),
    COOLDOWN("Command on cooldown.", "You can use this again in *%s*."),
    UNKNOWN_COMMAND("Unknown command.", "Type */help* for a list of commands."),
    REALM_UNAVAILABLE("Realm unavailable.", "*%s* can't be joined right now."),
    REALM_OFFLINE("Realm offline.", "*%s* is currently offline."),
    PLAYERS_ONLY("Players only.", "This command can only be used in-game."),
    API_ERROR("Something went wrong.", "Please try again in a moment.");

    private final String reason;
    private final String main;
}
