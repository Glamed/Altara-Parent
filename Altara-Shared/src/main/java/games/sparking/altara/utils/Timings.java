package games.sparking.altara.utils;

import lombok.Getter;

@Getter
public class Timings {

    private final String name;
    private long startMillis = -1;
    private long endMillis = -1;
    private boolean running = false;

    public Timings(String name) {
        this.name = name;
    }

    public Timings startTimings() {
        this.startMillis = System.currentTimeMillis();
        this.running = true;
        return this;
    }

    public Timings stopTimings() {
        this.endMillis = System.currentTimeMillis();
        this.running = false;
        return this;
    }

    /** Discards the elapsed time: a running timer restarts from now, a stopped one is cleared. */
    public void restart() {
        if (running) {
            this.startMillis = System.currentTimeMillis();
        } else {
            this.startMillis = -1;
            this.endMillis = -1;
        }
    }

    public long calculateDifference() {
        if (startMillis == -1) {
            return 0;
        }
        if (running) {
            return System.currentTimeMillis() - startMillis;
        }
        return Math.max(0, this.endMillis - this.startMillis);
    }
}
