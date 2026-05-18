package games.sparking.altara.task.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.task.TaskImplementor;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class BukkitTaskImplementor implements TaskImplementor {

    private final JavaPlugin plugin;

    @Override
    public void run(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void runAsync(Runnable runnable) {
        Altara.TASK_CHAIN.run(runnable);
    }

    @Override
    public void runLater(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
    }

    @Override
    public void runLaterAsync(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    @Override
    public void runTimer(Runnable runnable, long delay, long interval) {
        Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, interval);
    }

    @Override
    public void runTimerAsync(Runnable runnable, long delay, long interval) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, interval);
    }
}
