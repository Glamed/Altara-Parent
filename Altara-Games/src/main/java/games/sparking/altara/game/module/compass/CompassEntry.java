package games.sparking.altara.game.module.compass;

import games.sparking.altara.game.team.GameTeam;
import org.bukkit.entity.Entity;

/**
 * Represents one potential target that the {@link CompassModule} may point toward.
 */
public class CompassEntry {

    private final Entity   entity;
    private final String   name;
    private final String   displayName;
    private final GameTeam team;

    public CompassEntry(Entity entity, String name, String displayName, GameTeam team) {
        this.entity      = entity;
        this.name        = name;
        this.displayName = displayName;
        this.team        = team;
    }

    /** The entity that the compass will track. */
    public Entity getEntity() { return entity; }

    /** Raw name (usually player name). */
    public String getName() { return name; }

    /** Display name shown in the action bar (may include colours). */
    public String getDisplayName() { return displayName; }

    /** Team this entry belongs to, or {@code null} for solo games. */
    public GameTeam getTeam() { return team; }
}

