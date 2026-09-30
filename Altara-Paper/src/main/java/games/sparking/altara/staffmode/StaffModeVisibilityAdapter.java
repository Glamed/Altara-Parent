package games.sparking.altara.staffmode;

import games.sparking.altara.visibility.VisibilityAction;
import games.sparking.altara.visibility.VisibilityAdapter;
import org.bukkit.entity.Player;

/** Hides vanished staff from everyone except other staff. */
public class StaffModeVisibilityAdapter extends VisibilityAdapter {

    public StaffModeVisibilityAdapter() {
        super("Staff Mode Adapter", 10);
    }

    @Override
    public VisibilityAction canSee(Player player, Player target) {
        if (!StaffMode.isVanished(target)) return VisibilityAction.NEUTRAL;
        return player.hasPermission(StaffMode.PERMISSION) ? VisibilityAction.SHOW : VisibilityAction.HIDE;
    }
}
