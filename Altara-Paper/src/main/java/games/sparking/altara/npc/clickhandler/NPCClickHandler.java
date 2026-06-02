package games.sparking.altara.npc.clickhandler;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Functional interface for handling NPC interaction events.
 *
 * <p>The built-in {@link #COMMAND} handler dispatches the NPC's configured command
 * on the main server thread (click events arrive on the Netty I/O thread via
 * PacketEvents, so direct dispatch would violate Paper's async-catcher).
 */
@FunctionalInterface
public interface NPCClickHandler {

    NPCClickHandler COMMAND = (npc, player) -> {
        if (npc.getCommand() == null) return;
        String cmd = String.format(npc.getCommand(), player.getName());
        // dispatchCommand must run on the main thread.
        Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(), () ->
                Bukkit.dispatchCommand(
                        npc.isConsoleCommand() ? Bukkit.getConsoleSender() : player,
                        cmd));
    };

    void click(NPC npc, Player player);
}
