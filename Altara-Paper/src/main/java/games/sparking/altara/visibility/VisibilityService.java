package games.sparking.altara.visibility;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;


public class VisibilityService {

    private static final List<VisibilityAdapter> VISIBILITY_ADAPTERS =
            new ArrayList<>(Collections.singletonList(VisibilityAdapter.DEFAULT));
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
                "[VisibilityService] Registered %s with priority %d.",
                adapter.getName(), adapter.getPriority()));
    }

    public static void update(Player player) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            update(player, target);
            update(target, player);
        }
    }

    public static void update(Player player, Player target) {
        VisibilityAction action = VisibilityAction.NEUTRAL;
        int index = 0;
        while (action == VisibilityAction.NEUTRAL) {
            action = VISIBILITY_ADAPTERS.get(index++).canSee(player, target);
        }

        if (action == VisibilityAction.HIDE)
            player.hidePlayer(target);
        else
            player.showPlayer(target);
    }

    public static List<Component> getDebugInfo(Player player, Player target) {
        List<Component> debugs = new ArrayList<>();

        VisibilityAction finalAction = VisibilityAction.NEUTRAL;
        for (VisibilityAdapter adapter : VISIBILITY_ADAPTERS) {
            VisibilityAction provided = adapter.canSee(player, target);
            String color = provided == VisibilityAction.NEUTRAL ? "<gray>"
                    : (provided == VisibilityAction.SHOW ? "<green>" : "<red>");

            if (provided != VisibilityAction.NEUTRAL && finalAction == VisibilityAction.NEUTRAL)
                finalAction = provided;

            debugs.add(CC.format("<blue>%s (%d): %s%s",
                    adapter.getName(), adapter.getPriority(), color, provided.name()));
        }

        debugs.add(CC.translate("<blue>Result: <yellow>" + player.getName() + " " +
                (finalAction != VisibilityAction.HIDE ? "<green>can" : "<red>cannot") +
                " <blue>see <yellow>" + target.getName() + "<blue>."));
        return debugs;
    }

}
