package games.sparking.altara.npc.clickhandler;

import games.sparking.altara.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Handles an NPC click.  Always invoked on the main thread.
 *
 * <p>The built-in {@link #COMMAND} handler runs the NPC's configured command, with
 * {@code %player%} (or the legacy {@code %s}) replaced by the clicker's name.
 */
@FunctionalInterface
public interface NPCClickHandler {

    NPCClickHandler COMMAND = (npc, player) -> {
        if (npc.getCommand() == null) return;
        String cmd = npc.getCommand().replace("%player%", player.getName()).replace("%s", player.getName());
        Bukkit.dispatchCommand(npc.isConsoleCommand() ? Bukkit.getConsoleSender() : player, cmd);
    };

    void click(NPC npc, Player player);
}
