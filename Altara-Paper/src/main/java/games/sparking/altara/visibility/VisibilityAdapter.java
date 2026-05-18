package games.sparking.altara.visibility;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@AllArgsConstructor
@Getter
public abstract class VisibilityAdapter {

    public static final VisibilityAdapter DEFAULT = new VisibilityAdapter("Default Adapter", 0) {
        @Override
        public VisibilityAction canSee(Player player, Player target) {
            return VisibilityAction.SHOW;
        }
    };

    private final String name;
    private final int priority;

    public abstract VisibilityAction canSee(Player player, Player target);
}
