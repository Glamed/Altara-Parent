package games.sparking.altara.game.module;

/**
 * Holds per-player kill and assist counts for a single game session.
 * Obtained via {@link CombatTrackerModule#getCombatData(org.bukkit.entity.Player)}.
 */
public class CombatData {

    private int kills;
    private int assists;

    public void incrementKills()   { kills++;   }
    public void incrementAssists() { assists++; }

    public int getKills()   { return kills;   }
    public int getAssists() { return assists; }
}

