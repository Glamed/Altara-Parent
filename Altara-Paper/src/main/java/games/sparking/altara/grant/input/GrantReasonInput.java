package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.grant.menu.GrantScopesMenu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;

public class GrantReasonInput extends ChatInput<String> {

    public GrantReasonInput() {
        super(String.class);
        text(CC.translate("&ePlease enter the reason for this grant, or say &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the grant procedure.");

        accept((player, input) -> {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            if (profile.getGrantProcedure() == null) {
                player.sendMessage(CC.RED + "You're not in a granting process, idk how you even got this prompt");
                return true;
            }

            profile.getGrantProcedure().setReason(input);
            new GrantScopesMenu(profile).openMenu(player);
            return true;
        });
    }
}
