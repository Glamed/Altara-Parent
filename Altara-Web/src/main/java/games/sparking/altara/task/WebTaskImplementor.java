package games.sparking.altara.task;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TaskImplementor backed by a ScheduledExecutorService for use in the Web (Spring Boot) environment.
 */
public class WebTaskImplementor implements TaskImplementor {

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    @Override
    public void run(Runnable runnable) {
        executor.execute(runnable);
    }

    @Override
    public void runAsync(Runnable runnable) {
        executor.execute(runnable);
    }

    @Override
    public void runLater(Runnable runnable, long delayMillis) {
        executor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runLaterAsync(Runnable runnable, long delayMillis) {
        executor.schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runTimer(Runnable runnable, long delayMillis, long intervalMillis) {
        executor.scheduleAtFixedRate(runnable, delayMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runTimerAsync(Runnable runnable, long delayMillis, long intervalMillis) {
        executor.scheduleAtFixedRate(runnable, delayMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }
}
