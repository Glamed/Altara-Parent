package games.sparking.altara.task;


public interface TaskImplementor {

    void run(Runnable runnable);

    void runAsync(Runnable runnable);

    void runLater(Runnable runnable, long delay);

    void runLaterAsync(Runnable runnable, long delay);

    void runTimer(Runnable runnable, long delay, long interval);

    void runTimerAsync(Runnable runnable, long delay, long interval);

}
