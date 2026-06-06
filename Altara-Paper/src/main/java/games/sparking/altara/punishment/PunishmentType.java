package games.sparking.altara.punishment;


import lombok.Getter;

@Getter
public enum PunishmentType {
    SUSPENSION("Suspension"),
    CHAT_RESTRICTION("Chat Restriction"),
    DISCORD_RESTRICTION("Discord Restriction"),
    COMP_GAMEPLAY("Competitive Gameplay Restriction"),
    REPORT("Report Restriction"),
    WARN("Friendly Warning");

    private final String name;

    PunishmentType(String name) {
        this.name = name;
    }

    private static String formatDurationSuffix(long duration) {
        if (duration == -1) return "permanently";
        if (duration <= 0) return "immediately";
        return "for " + games.sparking.altara.utils.Time.formatDetailed(duration);
    }

    public String getActionLine(long duration) {
        String suffix = formatDurationSuffix(duration);
        return switch (this) {
            case CHAT_RESTRICTION -> "You cannot send Minecraft messages " + suffix + ".";
            case DISCORD_RESTRICTION -> "You cannot send Discord messages " + suffix + ".";
            case COMP_GAMEPLAY -> "You are restricted from competitive gameplay " + suffix + ".";
            case REPORT -> "You cannot submit new reports " + suffix + ".";
            case WARN -> "Friendly warning. Please correct future behavior.";
            default -> name + " applied " + suffix + ".";
        };
    }
}