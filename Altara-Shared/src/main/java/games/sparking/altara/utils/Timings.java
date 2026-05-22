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

    public long calculateDifference() {
        if ((startMillis == -1) && (endMillis == -1)) {
            return 0;
        }
        if (isRunning()) {
            return System.currentTimeMillis() - startMillis;
        }
        return this.endMillis - this.startMillis;
    }
}