package games.sparking.altara.games.duels.module;

import games.sparking.altara.framework.Game;
import games.sparking.altara.framework.GameState;
import games.sparking.altara.framework.annotation.GameEvent;
import games.sparking.altara.framework.annotation.RegisterModule;
import games.sparking.altara.games.duels.DuelGame;
import games.sparking.altara.games.duels.DuelMatch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles all combat-related events during a duel.
 *
 * <p>Every method here is compiled into a {@link games.sparking.altara.framework.GameHandler}
 * lambda at startup — zero reflection, zero annotation reads at runtime.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Prevent damage from players outside the match (arena isolation)</li>
 *   <li>Track combos and display them as action-bar messages</li>
 *   <li>Kill detection: when a hit would reduce health to ≤ 0, cancel the vanilla
 *       death and call {@link DuelGame#endMatch} immediately</li>
 *   <li>Void detection via fall damage below y = 0 for Sumo kit</li>
 * </ul>
 */
@RegisterModule(game = "duels")
public class DuelCombatModule {

    /**
     * Core damage handler.
     *
     * <p>The framework extracts the §bvictim§r player and calls this method only
     * when they are actively playing.  We then verify the damager is their opponent
     * and handle combo / kill logic.
     */
    @GameEvent(value = EntityDamageByEntityEvent.class, states = {GameState.PLAYING})
    public void onDamage(EntityDamageByEntityEvent event, Game game, Player victim, GameState state) {

        // Only interested in player-vs-player
        if (!(event.getDamager() instanceof Player damager)) {
            event.setCancelled(true);
            return;
        }

        DuelGame duelGame = (DuelGame) game;
        DuelMatch match = duelGame.getMatch(victim.getUniqueId());

        // Ensure the damager is this player's actual opponent
        if (!damager.getUniqueId().equals(match.getOpponent(victim.getUniqueId()))) {
            event.setCancelled(true);
            return;
        }

        double remainingHealth = victim.getHealth() - event.getFinalDamage();

        // ── Kill detection ──────────────────────────────────────────────────
        if (remainingHealth <= 0) {
            event.setCancelled(true);
            duelGame.endMatch(match, damager.getUniqueId(), "");
            return;
        }

        // ── Combo tracking ──────────────────────────────────────────────────
        int damagerCombo = match.incrementCombo(damager.getUniqueId());
        match.resetCombo(victim.getUniqueId());

        // ── Action-bar feedback ─────────────────────────────────────────────
        double victimHearts = Math.max(0, remainingHealth) / 2.0;
        damager.sendActionBar(Component.text(
                String.format("%.1f\u2764  | Combo %d", victimHearts, damagerCombo),
                NamedTextColor.RED
        ));
        victim.sendActionBar(Component.text(
                damager.getName() + " HP: " + String.format("%.1f\u2764", damager.getHealth() / 2.0),
                NamedTextColor.YELLOW
        ));
    }

    /**
     * Void / environmental damage handler.
     *
     * <p>For Sumo kit, falling into the void should end the match.
     * For Classic/Boxing/Archer, we suppress all non-player damage during the duel.
     */
    @GameEvent(value = EntityDamageEvent.class, states = {GameState.PLAYING})
    public void onEnvironmentalDamage(EntityDamageEvent event, Game game, Player player, GameState state) {
        // Player-vs-Player damage is handled by onDamage above — skip it here
        if (event instanceof EntityDamageByEntityEvent) return;

        DuelGame duelGame = (DuelGame) game;
        DuelMatch match = duelGame.getMatch(player.getUniqueId());

        // Void fall → instant kill (important for Sumo)
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            Player opponent = match.getOpponentOnline(player.getUniqueId());
            duelGame.endMatch(match, opponent != null ? opponent.getUniqueId() : null, "");
            return;
        }

        // All other environmental damage (fire, fall, etc.) is suppressed during a duel
        event.setCancelled(true);
    }
}
