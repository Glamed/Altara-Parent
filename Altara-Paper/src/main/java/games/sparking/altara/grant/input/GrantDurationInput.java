package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.command.parameter.defaults.Duration;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;

public class GrantDurationInput extends ChatInput<Duration> {

    public GrantDurationInput() {
        super(Duration.class);
        text(CC.translate("&ePlease enter the duration for this grant (\"perm\" for permanent), " +
                "or say &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the grant procedure");

        accept((player, duration) -> {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            if (profile.getGrantProcedure() == null) {
                player.sendMessage(CC.RED + "You're not in a granting process, idk how you even got this prompt");
                return true;
            }

            profile.getGrantProcedure().setDuration(duration.getDuration());
            return true;
        });
    }
}
