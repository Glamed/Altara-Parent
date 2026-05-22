package games.sparking.altara.server;

import games.sparking.altara.redis.packet.Packet;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UpdateServerPacket extends Packet {

    private final ServerInfo serverInfo;

    @Override
    public void receive() {
        ServerState previousState = serverInfo.getState();

        ServerInfo.updateServerInfo(serverInfo);

        if (previousState != serverInfo.getState()) {
            new NetworkBroadcastPacket("Server Updated").publish();
        }
    }
}
