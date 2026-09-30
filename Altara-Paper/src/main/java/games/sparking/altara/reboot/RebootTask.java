package games.sparking.altara.reboot;

import games.sparking.altara.utils.Theme;
import games.sparking.altara.utils.Time;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Counts down once per second, announcing at fixed marks, then restarts the server. */
@Getter
public class RebootTask extends BukkitRunnable {

    private static final Set<Integer> ANNOUNCE_AT = Set.of(10 * 60, 5 * 60, 4 * 60, 3 * 60, 2 * 60, 60, 30, 15, 10, 5, 4, 3, 2, 1);

    private int secondsRemaining;
    private boolean announced;

    public RebootTask(int secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }

    @Override
    public void run() {
        if (secondsRemaining <= 0) {
            cancel();
            Bukkit.getServer().restart();
            return;
        }

        // Always announce the first tick so a short or unusual countdown is still seen.
        if (!announced || ANNOUNCE_AT.contains(secondsRemaining)) {
            announced = true;
            RebootService.broadcast(Component.text()
                    .append(Component.text("This realm reboots in ", Theme.ERROR))
                    .append(Component.text(Time.formatDetailed(secondsRemaining, TimeUnit.SECONDS), Theme.TEXT_STRONG))
                    .append(Component.text(".", Theme.ERROR))
                    .build());
        }

        secondsRemaining--;
    }
}
