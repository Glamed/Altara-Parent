package games.sparking.altara.punishment;

import games.sparking.altara.utils.Time;
import lombok.Getter;

/**
 * The concrete restriction applied to a player as part of a punishment.
 * A single {@link Punishment} can carry multiple restriction actions.
 */
@Getter
public enum PunishmentType {

    SUSPENSION       ("Suspension",                       "Your account has been suspended %s."),
    CHAT_RESTRICTION ("Chat Restriction",                 "You cannot send Minecraft messages %s."),
    DISCORD_RESTRICTION("Discord Restriction",            "You cannot send Discord messages %s."),
    COMP_GAMEPLAY    ("Competitive Gameplay Restriction", "You are restricted from competitive gameplay %s."),
    REPORT           ("Report Restriction",               "You cannot submit new reports %s."),
    WARN             ("Friendly Warning",                 "Friendly warning. Please correct future behaviour.");

    private final String displayName;
    private final String actionLine;

    PunishmentType(String displayName, String actionLine) {
        this.displayName = displayName;
        this.actionLine  = actionLine;
    }

    /** Returns the human-readable restriction sentence for a given millisecond duration. */
    public String getActionLine(long duration) {
        return String.format(actionLine, formatDurationSuffix(duration));
    }

    private static String formatDurationSuffix(long duration) {
        if (duration == -1) return "permanently";
        if (duration <= 0)  return "immediately";
        return "for " + Time.formatDetailed(duration);
    }
}
