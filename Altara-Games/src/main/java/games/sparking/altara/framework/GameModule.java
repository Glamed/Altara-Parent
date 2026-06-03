package games.sparking.altara.framework;

/**
 * Base class for all game modules.
 *
 * <p>A module is a focused slice of game behaviour — combat rules, lifecycle
 * guards, scoreboards, etc.  Each game declares the modules it owns via
 * {@link Game#modules()}, and the framework wires everything up automatically
 * when the game is registered:
 *
 * <ol>
 *   <li>{@link #setup()} is called once so the module can initialise resources.</li>
 *   <li>Every method annotated with {@link games.sparking.altara.framework.annotation.GameEvent}
 *       is compiled into a zero-reflection {@link GameHandler} lambda and
 *       registered with the {@link EventBus}.</li>
 *   <li>{@link #cleanup()} is called when the owning plugin disables.</li>
 * </ol>
 *
 * <p>Modules receive a typed reference to their owning game at construction
 * time — no static casts, no singletons needed.
 *
 * <p><b>Example:</b>
 * <pre>
 * public class MyCombatModule extends GameModule {
 *
 *     private final MyGame game;
 *
 *     public MyCombatModule(MyGame game) { this.game = game; }
 *
 *     {@literal @}GameEvent(value = EntityDamageByEntityEvent.class)
 *     public void onDamage(EntityDamageByEntityEvent event, Game g, Player victim, GameState state) {
 *         // game is already the right type — no cast needed
 *     }
 * }
 * </pre>
 */
public abstract class GameModule {

    /**
     * Called once when the owning game is registered with the {@link GameManager}.
     * Override to allocate timers, data structures, external connections, etc.
     */
    public void setup() {}

    /**
     * Called once when the owning plugin shuts down.
     * Override to cancel tasks, close connections, or flush state.
     */
    public void cleanup() {}
}

