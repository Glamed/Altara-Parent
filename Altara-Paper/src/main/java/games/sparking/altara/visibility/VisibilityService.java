package games.sparking.altara.visibility;

import games.sparking.altara.AltaraPaper;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * Decides which players can see each other.  Adapters are asked in descending priority
 * until one returns something other than {@link VisibilityAction#NEUTRAL};
 * {@link VisibilityAdapter#DEFAULT} (priority 0) shows everyone.
 */
public class VisibilityService {

    private static final List<VisibilityAdapter> VISIBILITY_ADAPTERS =
            new CopyOnWriteArrayList<>(List.of(VisibilityAdapter.DEFAULT));

    /** Whether {@code sender} should treat {@code player} as online (e.g. hides vanished staff). */
    @Getter
    @Setter
    private static BiFunction<Player, CommandSender, Boolean> onlineTreatProvider = (player, sender) -> true;

    private static boolean initialized = false;

    public static void init() {
        if (initialized) {
            throw new IllegalStateException("VisibilityService has already been initialized");
        }

        initialized = true;
        Bukkit.getPluginManager().registerEvents(new VisibilityListener(), AltaraPaper.getPlugin());
    }

    public static void registerVisibilityAdapter(VisibilityAdapter adapter) {
        VISIBILITY_ADAPTERS.add(adapter);
        VISIBILITY_ADAPTERS.sort(Comparator.comparingInt(VisibilityAdapter::getPriority).reversed());
        AltaraPaper.getPlugin().getLogger().info(String.format(
                "[VisibilityService] Registered %s with priority %d.", adapter.getName(), adapter.getPriority()));
    }

    public static void update(Player player) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            update(player, target);
            update(target, player);
        }
    }

    public static void update(Player player, Player target) {
        if (resolve(player, target) == VisibilityAction.HIDE)
            player.hidePlayer(AltaraPaper.getPlugin(), target);
        else
            player.showPlayer(AltaraPaper.getPlugin(), target);
    }

    private static VisibilityAction resolve(Player player, Player target) {
        for (VisibilityAdapter adapter : VISIBILITY_ADAPTERS) {
            VisibilityAction action = adapter.canSee(player, target);
            if (action != VisibilityAction.NEUTRAL) return action;
        }
        return VisibilityAction.SHOW;
    }
}
