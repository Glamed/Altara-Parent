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

/** Tells the grantee's server to reload their profile and let them know about the new rank. */
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
        if (player == null) return;

        AltaraPaper.getPaperInstance().getProfileService().refreshProfile(uuid);
        AltaraPaper.getPaperInstance().updatePermissions(uuid);

        Rank rank = AltaraPaper.getPaperInstance().getRankService().getRank(rankUuid);
        String rankName = rank != null ? rank.getName() : "a new";
        player.sendMessage(CC.success("Rank granted.", "You now have the *" + rankName + "* rank "
                + (duration == -1 ? "permanently" : "for *" + Time.formatDetailed(duration) + "*") + "."));
    }
}
