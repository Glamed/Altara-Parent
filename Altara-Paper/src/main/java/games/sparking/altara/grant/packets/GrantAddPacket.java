package games.sparking.altara.grant.packets;


import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.rank.Rank;
import games.sparking.blazora.redis.packet.Packet;
import games.sparking.blazora.utils.CC;
import games.sparking.blazora.utils.TimeUtils;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;


@NoArgsConstructor
public class GrantAddPacket implements Packet {

    private static final BlazoraPaper zircon = BlazoraPaper.getPaperInstance();
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
        Rank rank = zircon.getRankService().getRank(rankUuid);
        if (player == null) {
            return;
        }

        zircon.getPermissionService().updatePermissions(player);

        if (duration == -1)
            player.sendMessage(CC.format(
                    "&aYou've been &epermanently &agranted the %s&a rank.",
                    rank.getName()
            ));
        else
            player.sendMessage(CC.format(
                    "&aYou've been granted the %s&a rank for &e%s&a.",
                    rank.getName(),
                    TimeUtils.formatDetailed(duration)
            ));
    }

    @Override
    public String getId() {
        return "";
    }
}
