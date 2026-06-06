package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.command.parameter.defaults.Duration;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GrantDurationInput extends ChatInput<Duration> {

    public GrantDurationInput() {
        super(Duration.class);
        text(
                CC.noticeMsg("", "Please enter the duration for this grant (<gray>\"perm\"</gray> for permanent)."),
                CC.noticeMsg("", "You can type *cancel* at any time to exit this process.")
        );
        escapeMessage(CC.errorMsg("You cancelled the grant creation process."));

        accept((player, duration) -> {
            Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);
            if (profile.getGrantProcedure() == null) {
                player.sendMessage(Component.text("You're not in a granting process.", NamedTextColor.RED));
                return true;
            }
            profile.getGrantProcedure().setDuration(duration.getDuration());
            return true;
        });
    }
}
