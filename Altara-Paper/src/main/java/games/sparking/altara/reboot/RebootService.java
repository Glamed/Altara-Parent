package games.sparking.altara.reboot;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class RebootService {

    public static final String CHAT_BAR = CC.RED + "⚠ " + CC.DRED + CC.STRIKE_THROUGH + "------------------------" + CC.RED + " ⚠";

    @Getter
    private static RebootTask rebootTask = null;

    public static void reboot(long millis) {
        if (rebootTask != null)
            return;

        rebootTask = new RebootTask((int) TimeUnit.MILLISECONDS.toSeconds(millis));
        rebootTask.runTaskTimer(AltaraPaper.getPlugin(), 20L, 20L);
    }

    public static void cancel() {
        if (rebootTask == null)
            return;

        rebootTask.cancel();
        rebootTask = null;
        Bukkit.broadcastMessage(RebootService.CHAT_BAR);
        Bukkit.broadcastMessage(CC.RED + "The reboot has been cancelled.");
        Bukkit.broadcastMessage(RebootService.CHAT_BAR);
    }

    public static boolean isRebooting() {
        return rebootTask != null;
    }

}
