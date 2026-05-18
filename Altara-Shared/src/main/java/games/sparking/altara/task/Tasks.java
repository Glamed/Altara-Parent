package games.sparking.altara.task;

import lombok.Setter;


public class Tasks {

    @Setter
    private static TaskImplementor taskImplementor;

    public static void run(Runnable runnable) {
        taskImplementor.run(runnable);
    }

    public static void runAsync(Runnable runnable) {
        taskImplementor.runAsync(runnable);
    }

    public static void runLater(Runnable runnable, long delay) {
        taskImplementor.runLater(runnable, delay);
    }

    public static void runLaterAsync(Runnable runnable, long delay) {
        taskImplementor.runLaterAsync(runnable, delay);
    }

    public static void runTimer(Runnable runnable, long delay, long interval) {
        taskImplementor.runTimer(runnable, delay, interval);
    }

    public static void runTimerAsync(Runnable runnable, long delay, long interval) {
        taskImplementor.runTimerAsync(runnable, delay, interval);
    }

}
