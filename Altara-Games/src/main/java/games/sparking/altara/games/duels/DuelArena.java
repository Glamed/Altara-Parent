package games.sparking.altara.games.duels;

import lombok.Getter;
import org.bukkit.Location;

/**
 * Represents a single duel arena with two spawn points.
 * Arenas are loaded from config and tracked by {@link DuelGame}.
 */
@Getter
public class DuelArena {

    private final String name;
    private Location spawn1;
    private Location spawn2;

    public DuelArena(String name, Location spawn1, Location spawn2) {
        this.name = name;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
    }

    public DuelArena(String name) {
        this.name = name;
    }

    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }

    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }

    /** Returns true if both spawn points have been configured. */
    public boolean isReady() {
        return spawn1 != null && spawn2 != null;
    }
}

