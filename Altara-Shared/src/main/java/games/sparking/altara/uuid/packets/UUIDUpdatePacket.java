package games.sparking.altara.uuid.packets;

import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.uuid.UUIDCache;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
public class UUIDUpdatePacket extends Packet {

    public UUIDUpdatePacket(UUID uuid, String oldName, String newName) {
        this.uuid = uuid;
        this.oldName = oldName;
        this.newName = newName;
    }

    private UUID uuid;
    private String oldName;
    private String newName;

    @Override
    public void receive() {
        UUIDCache.updateLocally(uuid, oldName, newName);
    }

}
