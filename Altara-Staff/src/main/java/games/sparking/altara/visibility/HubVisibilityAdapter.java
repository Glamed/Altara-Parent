package games.sparking.altara.visibility;

import games.sparking.altara.AltaraLobby;
import org.bukkit.entity.Player;

/**
 * Hides everyone when {@code hidePlayers} is on, and always hides players in the
 * {@value #HIDDEN_WORLD} world from those outside it.
 */
public class HubVisibilityAdapter extends VisibilityAdapter {

    public static final String HIDDEN_WORLD = "VOID";

    public HubVisibilityAdapter() {
        super("Hub Visibility Adapter", 5);
    }

    @Override
    public VisibilityAction canSee(Player player, Player target) {
        if (AltaraLobby.getLobbyInstance().getStaffConfig().isHidePlayers()) return VisibilityAction.HIDE;
        boolean targetHidden = target.getWorld().getName().equalsIgnoreCase(HIDDEN_WORLD);
        boolean viewerHidden = player.getWorld().getName().equalsIgnoreCase(HIDDEN_WORLD);
        return targetHidden && !viewerHidden ? VisibilityAction.HIDE : VisibilityAction.NEUTRAL;
    }
}
