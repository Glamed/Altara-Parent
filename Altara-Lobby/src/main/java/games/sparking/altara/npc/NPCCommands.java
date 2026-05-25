package games.sparking.altara.npc;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.configuration.defaults.LocationConfig;
import games.sparking.altara.selector.ServerSelectorEntry;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

@Header(
        primaryColor = "&5",
        secondaryColor = "&8",
        tertiaryColor = "&d",
        header = "NPC"
)
public class NPCCommands {

    @Command(
            names = "npc setlocation",
            playerOnly = true,
            permission = "altara.command.npc.setlocation",
            description = "Set the NPC location for a server selector entry"
    )
    public boolean setNpcLocation(Player player, @Param(name = "serverName") String serverName) {
        AltaraLobby lobby = AltaraLobby.getLobbyInstance();
        List<ServerSelectorEntry> entries = lobby.getLobbyConfig().getServerSelector();

        ServerSelectorEntry target = entries.stream()
                .filter(e -> e.getServerName().equalsIgnoreCase(serverName))
                .findFirst()
                .orElse(null);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No server selector entry found for \"" + serverName + "\".");
            return true;
        }

        target.setNpcLocation(new LocationConfig(player.getLocation()));
        lobby.getLobbyConfig().saveConfig();

        // Re-spawn the NPC at its new location
        lobby.getLobbyNPC().spawnForEntry(target);

        player.sendMessage(ChatColor.GREEN + "NPC location for \"" + target.getServerName()
                + "\" set to your current position and respawned.");
        return true;
    }
}
