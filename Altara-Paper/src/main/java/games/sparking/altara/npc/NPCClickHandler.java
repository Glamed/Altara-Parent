package games.sparking.altara.npc;

import org.bukkit.entity.Player;

/**
 * Callback fired when a player left-clicks or right-clicks an {@link NPC}.
 *
 * <p>Click detection is driven by the PacketEvents {@code INTERACT_ENTITY} listener
 * in {@link NPCService}.  Both the main NPC body entity and any nametag armor-stand entities
 * route clicks to the same handler.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * new NPCBuilder()
 *     .at(location)
 *     .skin(skin)
 *     .nametag("&6NPC Name")
 *     .clickHandler((player, npc, type) -> {
 *         if (type == NPCClickHandler.ClickType.RIGHT_CLICK)
 *             player.sendMessage("&aYou right-clicked the NPC!");
 *     })
 *     .build()
 *     .spawn();
 * }</pre>
 */
@FunctionalInterface
public interface NPCClickHandler {

    /**
     * Called when {@code player} interacts with {@code npc}.
     *
     * @param player    the player who clicked
     * @param npc       the NPC that was clicked
     * @param clickType whether it was a left-click (attack) or right-click (interact)
     */
    void click(Player player, NPC npc, ClickType clickType);

    enum ClickType {
        LEFT_CLICK,
        RIGHT_CLICK
    }
}

