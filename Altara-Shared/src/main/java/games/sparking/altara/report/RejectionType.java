package games.sparking.altara.report;

import lombok.Getter;
import org.bukkit.Material;

/** Reason a report was closed without action, chosen by staff when rejecting one. */
@Getter
public enum RejectionType {

    INSUFFICIENT_EVIDENCE(false, Material.BOOK,
            "Insufficient Evidence", "Not enough evidence to act on this report."),

    ABUSE(true, Material.REDSTONE_BLOCK,
            "Abusive Report", "The report itself was malicious or in bad faith.");

    private final boolean abusive;
    private final Material material;
    private final String displayName;
    private final String description;

    RejectionType(boolean abusive, Material material, String displayName, String description) {
        this.abusive = abusive;
        this.material = material;
        this.displayName = displayName;
        this.description = description;
    }
}
