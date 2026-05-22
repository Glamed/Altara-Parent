package games.sparking.altara.grant.input;

import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.chatinput.ChatInput;
import games.sparking.blazora.grant.menu.GrantScopesMenu;
import games.sparking.blazora.profile.Profile;
import games.sparking.blazora.utils.CC;

public class GrantReasonInput extends ChatInput<String> {

    public GrantReasonInput(BlazoraPaper zircon) {
        super(String.class);
        text(CC.translate("&ePlease enter the reason for this grant, or say &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the grant procedure.");

        accept((player, input) -> {
            Profile profile = zircon.getProfileService().getProfile(player);
            if (profile.getGrantProcedure() == null) {
                player.sendMessage(CC.RED + "You're not in a granting process, idk how you even got this prompt");
                return true;
            }

            profile.getGrantProcedure().setReason(input);
            new GrantScopesMenu(zircon, profile).openMenu(player);
            return true;
        });
    }
}
