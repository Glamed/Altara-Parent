package games.sparking.altara.grant.packets;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.CC;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@NoArgsConstructor
public class GrantRemovePacket extends Packet {

    private UUID uuid;
    private UUID rankUuid;

    public GrantRemovePacket(UUID uuid, UUID rankUuid) {
        this.uuid = uuid;
        this.rankUuid = rankUuid;
    }

    @Override
    public void receive() {
        Player player = Bukkit.getPlayer(uuid);
        Rank rank = AltaraPaper.getPaperInstance().getRankService().getRank(rankUuid);
        if (player == null) {
            return;
        }

        AltaraPaper.getPaperInstance().getPermissionService().updatePermissions(player);

        player.sendMessage(CC.format(
                "<green>Your %s<green> grant has been removed.",
                rank.getName()
        ));
    }
}
