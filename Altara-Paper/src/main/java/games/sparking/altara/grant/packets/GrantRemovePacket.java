package games.sparking.altara.grant.packets;

import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.rank.Rank;
import games.sparking.blazora.redis.packet.Packet;
import games.sparking.blazora.utils.CC;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@NoArgsConstructor
public class GrantRemovePacket implements Packet {

    private static final BlazoraPaper zircon = BlazoraPaper.getPaperInstance();
    private UUID uuid;
    private UUID rankUuid;

    public GrantRemovePacket(UUID uuid, UUID rankUuid) {
        this.uuid = uuid;
        this.rankUuid = rankUuid;
    }

    @Override
    public void receive() {
        Player player = Bukkit.getPlayer(uuid);
        Rank rank = zircon.getRankService().getRank(rankUuid);
        if (player == null) {
            return;
        }

        zircon.getPermissionService().updatePermissions(player);

        player.sendMessage(CC.format(
                "&aYour %s&a grant has been removed.",
                rank.getName()
        ));
    }

    @Override
    public String getId() {
        return "";
    }
}
