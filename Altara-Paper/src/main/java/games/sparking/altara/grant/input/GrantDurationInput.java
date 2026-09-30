package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.command.parameter.defaults.Duration;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;

public class GrantDurationInput extends ChatInput<Duration> {

    public GrantDurationInput() {
        super(Duration.class);
        text(
                CC.notice("Enter a duration.", "For example *30d* or *1w2d*, or *perm* for permanent."),
                CC.info("Type *cancel* to stop granting.")
        );
        escapeMessage(CC.info("Grant cancelled."));
        onCancel(GrantReasonInput::clearProcedure);

        accept((player, duration) -> {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            if (profile == null || profile.getGrantProcedure() == null) {
                player.sendMessage(CC.error("Grant expired.", "Run */grant* again to start over."));
                return true;
            }
            profile.getGrantProcedure().setDuration(duration.getDuration());
            return true;
        });
    }
}
