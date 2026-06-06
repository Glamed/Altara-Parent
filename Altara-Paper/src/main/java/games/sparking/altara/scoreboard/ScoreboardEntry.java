package games.sparking.altara.scoreboard;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

public class ScoreboardEntry {

    /**
     * -- SETTER --
     *  Manually force a line update.
     *  Currently package-private, may become public in the future.
     */
    @Setter
    private boolean nameChanged = false;
    /**
     * -- GETTER --
     *  Return the current assigned identifying text for this line.
     * -- SETTER --
     *  Manually set the identifying name.
     *  Currently package-private, may become public in the future.

     */
    @Setter
    @Getter
    private String identifyingName;
    /**
     * -- GETTER --
     *  Return the current assigned left-aligned display text for this line.
     */
    @Getter
    private Component leftDisplayName = null;
    /**
     * -- GETTER --
     *  Return the current assigned right-aligned display text for this line.
     */
    @Getter
    private Component rightDisplayName = null;

    /**
     * Initialize the ScoreBoardEntry object.
     * The identifyingName string only matters on 1.20.3+. It can be any value on other versions.
     */
    public ScoreboardEntry(String identifyingName) {
        this.identifyingName = identifyingName;
    }

    /*
     * Getters.
     */

    /**
     * Return whether any text on this line has been changed.
     */
    public boolean hasNameChanged() {
        return nameChanged;
    }

    /*
     * Setters.
     */

    /*
     * Board Updaters.
     */

    /**
     * Handle a scoreboard update.
     */
    public void update() {
        nameChanged = false;
    }

    /**
     * This method update this line's left-aligned text to the given text.
     */
    public void updateLeftAlignedText(Component text) {
        if (leftDisplayName != null && leftDisplayName.equals(text)) return;
        leftDisplayName = text;
        nameChanged = true;
    }

    /**
     * This method update this line's right-aligned text to the given text.
     * This is only available in 1.20.3+.
     */
    public void updateRightAlignedText(Component text) {
        if (rightDisplayName != null && rightDisplayName.equals(text)) return;
        rightDisplayName = text;
        nameChanged = true;
    }
}