package games.sparking.altara.game.spectator;

import games.sparking.altara.game.GameManager;
import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.player.GamePlayer;
import games.sparking.altara.visibility.VisibilityAction;
import games.sparking.altara.visibility.VisibilityAdapter;
import org.bukkit.entity.Player;

/**
 * A {@link VisibilityAdapter} that hides spectating players from alive players
 * who are in the same game.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>If the <em>target</em> is spectating/eliminated in a game and the
 *       <em>viewer</em> is <b>alive</b> in the same game → {@link VisibilityAction#HIDE}.</li>
 *   <li>If the <em>viewer</em> is also spectating in the same game →
 *       {@link VisibilityAction#SHOW} (spectators can see each other).</li>
 *   <li>All other combinations → {@link VisibilityAction#NEUTRAL} (fall through
 *       to the next adapter).</li>
 * </ul>
 *
 * <p>Register this adapter once at plugin startup:
 * <pre>{@code
 * VisibilityService.registerVisibilityAdapter(new SpectatorVisibilityAdapter());
 * }</pre>
 */
public class SpectatorVisibilityAdapter extends VisibilityAdapter {

    /**
     * Priority 10 – evaluated before the {@code DEFAULT} adapter (priority 0)
     * so that spectators are hidden before the default SHOW applies.
     */
    public SpectatorVisibilityAdapter() {
        super("Spectator Adapter", 10);
    }

    @Override
    public VisibilityAction canSee(Player viewer, Player target) {
        GameManager gm = GameManager.getInstance();

        // We only care if the target is in a game
        Game targetGame = gm.getPlayerGame(target);
        if (targetGame == null) return VisibilityAction.NEUTRAL;

        GamePlayer targetGp = targetGame.getGamePlayer(target).orElse(null);
        if (targetGp == null) return VisibilityAction.NEUTRAL;

        // Target is not spectating – this adapter has no opinion
        if (!targetGp.isSpectating()) return VisibilityAction.NEUTRAL;

        // Target IS spectating. Check the viewer's relationship to the target's game.
        Game viewerGame = gm.getPlayerGame(viewer);

        // Viewer is in the same game
        if (viewerGame != null && viewerGame.getInstanceId().equals(targetGame.getInstanceId())) {
            GamePlayer viewerGp = viewerGame.getGamePlayer(viewer).orElse(null);
            if (viewerGp != null && viewerGp.isSpectating()) {
                // Spectators can see each other
                return VisibilityAction.SHOW;
            }
            // Alive player in the same game cannot see spectators
            return VisibilityAction.HIDE;
        }

        // Viewer is in a different game or not in any game – let the default adapter decide
        return VisibilityAction.NEUTRAL;
    }
}

