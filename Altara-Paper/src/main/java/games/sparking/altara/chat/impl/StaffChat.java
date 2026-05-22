package games.sparking.altara.chat.impl;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.command.playersetting.AltaraSettings;
import games.sparking.altara.server.NetworkBroadcastPacket;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import games.sparking.altara.profile.Profile;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class StaffChat  extends ChatChannel {

    @Getter
    private static final StaffChat instance = new StaffChat();

    public StaffChat() {
        super("staff",
                CC.BLUE + "Staff",
                "zircon.command.staffchat",
                Collections.singletonList("sc"),
                '?',
                10);
    }

    @Override
    public String getFormat(Player player, CommandSender commandSender) {
        return null;
    }

    @Override
    public boolean onChat(Player player, String s) {
        return false;
    }

    @Override
    public void chat(Player player, String message) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        if (!AltaraSettings.STAFF_MESSAGES.get(player))
            player.sendMessage(CC.YELLOW + "Your message has been sent.");

        new NetworkBroadcastPacket(CC.format("&8[&cStaff Chat&8]&7 %s &8&l»&c %s", profile.getRealCurrentGrant().asRank().getPrefix() + profile.getName(), message),
                "zircon.staff",
                true
        ).publish();
    }
}