package games.sparking.altara.report;

import lombok.Getter;
import org.bukkit.Material;

/**
 * The specific violation a report alleges.  Carries the {@link ReportGroup} it belongs to
 * (which decides whether chat history is attached), a priority weight used when ranking
 * the queue, and menu display metadata.
 */
@Getter
public enum ReportCategory {

    CHAT_ABUSE(ReportGroup.CHAT, 2, Material.BOOK,
            "Chat Abuse", "Swearing, discrimination, harassment, or other abusive chat."),

    CHEATING(ReportGroup.GAMEPLAY, 3, Material.IRON_SWORD,
            "Cheating", "Hacked clients, cross-teaming, or other unfair gameplay advantages."),

    BAD_NAME(ReportGroup.GAMEPLAY, 1, Material.NAME_TAG,
            "Inappropriate Name", "A username that is offensive, discriminatory, or inappropriate."),

    BAD_SKIN(ReportGroup.GAMEPLAY, 1, Material.ARMOR_STAND,
            "Inappropriate Skin", "A skin that is offensive, discriminatory, or inappropriate."),

    EXPLOITS(ReportGroup.GAMEPLAY, 2, Material.ANVIL,
            "Exploiting", "Abusing bugs or unintended mechanics for an unfair advantage.");

    private final ReportGroup group;
    private final int priority;
    private final Material material;
    private final String displayName;
    private final String description;

    ReportCategory(ReportGroup group, int priority, Material material, String displayName, String description) {
        this.group = group;
        this.priority = priority;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
    }
}
