package games.sparking.altara.task;

import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.Bukkit;

/**
 * Fires {@link UpdateEvent} for each {@link UpdateType} whose interval has elapsed.
 * Scheduled via {@link Tasks#runTimer} every tick on enable.
 */
public class UpdateTask {

    public static void start() {
        Tasks.runTimer(UpdateTask::tick, 0L, 1L);
    }

    private static void tick() {
        for (UpdateType updateType : UpdateType.values()) {
            if (updateType.Elapsed()) {
                Bukkit.getPluginManager().callEvent(new UpdateEvent(updateType));
            }
        }
    }
}
