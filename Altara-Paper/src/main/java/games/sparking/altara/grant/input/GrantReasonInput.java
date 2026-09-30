package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.grant.menu.GrantScopesMenu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import org.bukkit.entity.Player;

public class GrantReasonInput extends ChatInput<String> {

    public GrantReasonInput() {
        super(String.class);
        text(
                CC.notice("Enter a reason.", "Explain why this rank is being granted."),
                CC.info("Type *cancel* to stop granting.")
        );
        escapeMessage(CC.info("Grant cancelled."));
        onCancel(GrantReasonInput::clearProcedure);

        accept((player, input) -> {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            if (profile == null || profile.getGrantProcedure() == null) {
                player.sendMessage(CC.error("Grant expired.", "Run */grant* again to start over."));
                return true;
            }
            profile.getGrantProcedure().setReason(input);
            new GrantScopesMenu(profile).openMenu(player);
            return true;
        });
    }

    static void clearProcedure(Player player) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
        if (profile != null) profile.setGrantProcedure(null);
    }
}
