package games.sparking.altara.hologram.clickhandler;

import games.sparking.altara.hologram.Hologram;
import org.bukkit.entity.Player;

/**
 * Callback fired when a player left-clicks or right-clicks a {@link Hologram}.
 *
 * <p>Click detection works because each hologram line is an invisible armor-stand entity
 * (not a Marker, so it retains its hitbox).  Incoming {@code INTERACT_ENTITY} packets are
 * intercepted by PacketEvents and routed here by {@link games.sparking.altara.hologram.HologramService}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * new HologramBuilder()
 *     .at(location)
 *     .lines("&e[Click to warp]")
 *     .clickHandler((player, hologram, clickType) -> {
 *         if (clickType == HologramClickHandler.ClickType.RIGHT_CLICK) {
 *             player.teleport(warpLocation);
 *         }
 *     })
 *     .buildAndSpawn();
 * }</pre>
 */
@FunctionalInterface
public interface HologramClickHandler {

    /**
     * Called when {@code player} interacts with {@code hologram}.
     *
     * @param player    the player who clicked
     * @param hologram  the hologram that was clicked
     * @param clickType whether it was a left-click (attack) or right-click (interact)
     */
    void click(Player player, Hologram hologram, ClickType clickType);

    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }

}