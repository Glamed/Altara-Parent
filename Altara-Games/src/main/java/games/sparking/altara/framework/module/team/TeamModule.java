package games.sparking.altara.framework.module.team;

import games.sparking.altara.framework.*;
import games.sparking.altara.framework.annotation.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Drop-in {@link GameModule} that prevents teammates from damaging each other
 * in {@link GameType#TEAM} games.
 *
 * <p>To enable friendly-fire protection, add this module to your game's
 * {@link Game#modules()} list:
 * <pre>
 * {@literal @}Override
 * public List{@literal <GameModule>} modules() {
 *     return List.of(
 *         new TeamModule(this),
 *         new MyCombatModule(this)
 *     );
 * }
 * </pre>
 *
 * <p>The handler is a no-op in {@link GameType#SOLO} games; it is safe to include
 * regardless of game type.
 */
public class TeamModule extends GameModule {

    private final AbstractGame game;

    /**
     * @param game the owning {@link AbstractGame} — must be the same instance
     *             returned by {@link Game#modules()} so team lookups are correct
     */
    public TeamModule(AbstractGame game) {
        this.game = game;
    }

    /**
     * Cancels damage events where the attacker and victim are on the same team.
     */
    @GameEvent(value = EntityDamageByEntityEvent.class, states = {GameState.PLAYING})
    public void onFriendlyFire(EntityDamageByEntityEvent event, Game g, Player victim, GameState state) {
        if (game.getGameType() != GameType.TEAM) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        if (game.areTeammates(victim.getUniqueId(), attacker.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}

