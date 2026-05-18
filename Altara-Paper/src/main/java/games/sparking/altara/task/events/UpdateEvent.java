package games.sparking.altara.task.events;

import games.sparking.altara.task.UpdateType;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.NonNull;

public class UpdateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final UpdateType _type;

    public UpdateEvent(UpdateType example)
    {
        _type = example;
    }

    public UpdateType getType()
    {
        return _type;
    }

    public int getTick() {
        return Bukkit.getServer().getCurrentTick();
    }

    public @NonNull HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}