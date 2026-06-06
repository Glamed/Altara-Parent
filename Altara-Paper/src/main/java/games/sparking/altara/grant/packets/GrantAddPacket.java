package games.sparking.altara.grant.packets;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;


@NoArgsConstructor
public class GrantAddPacket extends Packet {

    private UUID uuid;
    private UUID rankUuid;
    private long duration;

    public GrantAddPacket(UUID uuid, UUID rankUuid, long duration) {
        this.uuid = uuid;
        this.rankUuid = rankUuid;
        this.duration = duration;
    }

    @Override
    public void receive() {
        Player player = Bukkit.getPlayer(uuid);
        Rank rank = AltaraPaper.getPaperInstance().getRankService().getRank(rankUuid);
        if (player == null) {
            return;
        }

        AltaraPaper.getPaperInstance().getPermissionService().updatePermissions(player);

        if (duration == -1)
            player.sendMessage(CC.format(
                    "<green>You've been <yellow>permanently <green>granted the %s<green> rank.",
                    rank.getName()
            ));
        else
            player.sendMessage(CC.format(
                    "<green>You've been granted the %s<green> rank for <yellow>%s<green>.",
                    rank.getName(),
                    Time.formatDetailed(duration)
            ));
    }
}
