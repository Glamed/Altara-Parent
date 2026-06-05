package games.sparking.altara.server.packet;

import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.server.ServerInfo;
import games.sparking.altara.server.ServerState;
import games.sparking.altara.utils.CC;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateServerPacket extends Packet {

    private final ServerInfo serverInfo;

    @Override
    public void receive() {
        ServerState previousState = serverInfo.getState();

        ServerInfo.updateServerInfo(serverInfo);

        if (previousState != serverInfo.getState()) {
            new NetworkBroadcastPacket(
                    CC.format("<dark_gray>[</dark_gray><dark_red>!</dark_red><dark_gray>]</dark_gray><gray> Status of <white>%s/white> has changed to %s.",
                            serverInfo.getName(),
                            serverInfo.getState().getInternalName()
                            ))
                    .publish();
        }
    }
}
