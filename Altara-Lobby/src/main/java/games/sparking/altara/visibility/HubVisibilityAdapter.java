package games.sparking.altara.visibility;

import games.sparking.altara.AltaraLobby;
import org.bukkit.entity.Player;

public class HubVisibilityAdapter extends VisibilityAdapter {

    public HubVisibilityAdapter() {
        super("Hub Visibility Adapter", 5);
    }

    @Override
    public VisibilityAction canSee(Player player, Player player1) {
        return (AltaraLobby.getLobbyInstance().getLobbyConfig().isHidePlayers() ? VisibilityAction.HIDE : VisibilityAction.NEUTRAL);
    }
}
