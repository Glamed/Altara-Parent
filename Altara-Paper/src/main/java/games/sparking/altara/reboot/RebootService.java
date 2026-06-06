package games.sparking.altara.reboot;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class RebootService {

    public static final Component CHAT_BAR = Component.text()
            .append(Component.text("⚠ ", CC.RED))
            .append(Component.text("------------------------", CC.DRED, TextDecoration.STRIKETHROUGH))
            .append(Component.text(" ⚠", CC.RED))
            .build();

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
        Bukkit.broadcast(CHAT_BAR);
        Bukkit.broadcast(Component.text("The reboot has been cancelled.", CC.RED));
        Bukkit.broadcast(CHAT_BAR);
    }

    public static boolean isRebooting() {
        return rebootTask != null;
    }

}
