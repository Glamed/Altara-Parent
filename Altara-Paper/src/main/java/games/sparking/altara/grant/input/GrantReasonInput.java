package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.grant.menu.GrantScopesMenu;
import games.sparking.altara.profile.Profile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GrantReasonInput extends ChatInput<String> {

    public GrantReasonInput() {
        super(String.class);
        text("<yellow>Please enter the reason for this grant, or say <red>cancel</red> to cancel.");
        escapeMessage("<red>You cancelled the grant procedure.");

        accept((player, input) -> {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            if (profile.getGrantProcedure() == null) {
                player.sendMessage(Component.text("You're not in a granting process.", NamedTextColor.RED));
                return true;
            }
            profile.getGrantProcedure().setReason(input);
            new GrantScopesMenu(profile).openMenu(player);
            return true;
        });
    }
}
