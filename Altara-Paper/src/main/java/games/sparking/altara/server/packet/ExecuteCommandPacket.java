package games.sparking.altara.server.packet;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.task.Tasks;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ExecuteCommandPacket extends Packet {

    private String executor = "Console";
    private String server = null;
    private String scope = null;
    private String command = "";

    @Override
    public void receive() {
        Tasks.run(() -> {
            if (server == null && scope == null && !AltaraPaper.getPaperInstance().getServerGroup().equals("proxy")) {
                AltaraPaper.getPaperInstance().getLogger().info("Executing command '" + command + "' on all servers by " + executor + ".");
                AltaraPaper.getPaperInstance().dispatchConsoleCommand(command);
                return;
            }

            if (AltaraPaper.getPaperInstance().getLocalServerName().equalsIgnoreCase(server)) {
                AltaraPaper.getPaperInstance().getLogger().info("Executing command '" + command + "' by " + executor + ".");
                AltaraPaper.getPaperInstance().dispatchConsoleCommand(command);
                return;
            }

            if (AltaraPaper.getPaperInstance().getServerGroup().equalsIgnoreCase(scope)) {
                AltaraPaper.getPaperInstance().getLogger().info("Executing command '" + command + "' on all " + scope + " servers by " + executor + ".");
                AltaraPaper.getPaperInstance().dispatchConsoleCommand(command);
                return;
            }
        });
    }
}
