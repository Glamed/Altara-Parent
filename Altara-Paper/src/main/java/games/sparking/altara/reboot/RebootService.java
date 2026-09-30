package games.sparking.altara.reboot;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.utils.Theme;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class RebootService {

    @Getter
    private static RebootTask rebootTask = null;

    public static void reboot(long millis) {
        if (rebootTask != null) return;

        rebootTask = new RebootTask((int) TimeUnit.MILLISECONDS.toSeconds(millis));
        rebootTask.runTaskTimer(AltaraPaper.getPlugin(), 0L, 20L);
    }

    public static void cancel() {
        if (rebootTask == null) return;

        rebootTask.cancel();
        rebootTask = null;
        broadcast(Component.text("The reboot has been cancelled.", Theme.TEXT_STRONG));
    }

    public static boolean isRebooting() {
        return rebootTask != null;
    }

    /** {@code [!] <message>} to everyone — the alert format for realm-wide warnings. */
    static void broadcast(Component message) {
        Bukkit.broadcast(Component.text()
                .append(Component.text("[", Theme.STRUCTURE))
                .append(Component.text("!", Theme.ERROR_DARK, TextDecoration.BOLD))
                .append(Component.text("] ", Theme.STRUCTURE))
                .append(message)
                .build());
    }
}
