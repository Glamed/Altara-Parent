package games.sparking.altara.grant.input;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;

public class GrantRemoveInput extends ChatInput<String> {

    public GrantRemoveInput(Profile target, Grant grant) {
        super(String.class);
        text(
                CC.notice("Enter a reason.", "Explain why this grant is being removed."),
                CC.info("Type *cancel* to keep the grant.")
        );
        escapeMessage(CC.info("Grant removal cancelled."));

        accept((player, input) -> {
            grant.setRemovedAt(System.currentTimeMillis());
            grant.setRemovedBy(player.getUniqueId().toString());
            grant.setRemovedReason(input);
            grant.setRemoved(true);

            String rankName = grant.asRank() != null ? grant.asRank().getName() : "Unknown";
            Tasks.runAsync(() -> {
                RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().removeGrant(target, grant);
                if (response.couldNotConnect()) {
                    player.sendMessage(CC.notice("Removal queued.", "The API is unreachable, so the grant will be removed when it's back."));
                } else if (!response.wasSuccessful()) {
                    player.sendMessage(CC.error("Unable to remove grant.", response.getErrorMessage() + " (" + response.getCode() + ")"));
                } else {
                    player.sendMessage(CC.success("Grant removed.", "*" + target.getName() + "* no longer has *" + rankName + "*."));
                }
            });
            return true;
        });
    }
}
