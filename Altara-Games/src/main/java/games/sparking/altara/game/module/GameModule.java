package games.sparking.altara.game.module;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.impl.Game;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

/**
 * Pluggable behaviour component that attaches to a {@link Game} instance.
 *
 * <p>Register a module during {@link Game#onStart()} via {@link Game#addModule(GameModule)}.
 * The framework will automatically:
 * <ul>
 *   <li><b>enable</b> it (register listener + call {@link #onEnable()}) after {@code onStart()} finishes</li>
 *   <li><b>disable</b> it (call {@link #onDisable()} + unregister listener) when the game goes {@code Dead}</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // inside onStart():
 * addModule(new MapCrumbleModule(3 * 60_000L).startMessage("§cThe world crumbles!"));
 * }</pre>
 */
public abstract class GameModule implements Listener {

    private Game game;

    /** Called by {@link Game} when the game goes {@code Live}. Not part of the public API. */
    public final void attach(Game owner) {
        this.game = owner;
        Bukkit.getPluginManager().registerEvents(this, AltaraPaper.getPaperInstance());
        onEnable();
    }

    /** Called by {@link Game} when the game goes {@code Dead}. Not part of the public API. */
    public final void detach() {
        onDisable();
        HandlerList.unregisterAll(this);
    }

    /** @return the {@link Game} this module is attached to */
    protected Game getGame() { return game; }

    /** Called once when the module activates. Override to initialise state. */
    protected void onEnable() {}

    /** Called once when the module deactivates. Override to release resources. */
    protected void onDisable() {}
}



