package games.sparking.altara.grant.packets;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.redis.packet.Packet;
import games.sparking.altara.utils.CC;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Tells the player's server to reload their profile after one of their grants was removed. */
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
        if (player == null) return;

        AltaraPaper.getPaperInstance().getProfileService().refreshProfile(uuid);
        AltaraPaper.getPaperInstance().updatePermissions(uuid);

        Rank rank = AltaraPaper.getPaperInstance().getRankService().getRank(rankUuid);
        player.sendMessage(CC.notice("Rank removed.", "Your *" + (rank != null ? rank.getName() : "rank")
                + "* grant has been removed."));
    }
}
