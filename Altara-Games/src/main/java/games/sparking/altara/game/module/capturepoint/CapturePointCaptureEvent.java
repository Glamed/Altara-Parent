package games.sparking.altara.game.module.capturepoint;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a team successfully captures a {@link CapturePoint}.
 */
public class CapturePointCaptureEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CapturePoint point;

    CapturePointCaptureEvent(CapturePoint point) {
        this.point = point;
    }

    /** The point that was captured. */
    public CapturePoint getPoint() { return point; }

    @Override public HandlerList getHandlers()    { return HANDLERS; }
    public static HandlerList    getHandlerList() { return HANDLERS; }
}

