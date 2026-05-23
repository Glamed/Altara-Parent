package games.sparking.altara.grant.input;

import games.sparking.altara.Altara;
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
        text(CC.translate("&ePlease enter the reason for the removal of this grant, or say &ccancel &eto cancel."));
        escapeMessage(CC.RED + "You cancelled the grant removal.");

        accept((player, input) -> {
            grant.setRemovedAt(System.currentTimeMillis());
            grant.setRemovedBy(player.getUniqueId().toString());
            grant.setRemovedReason(input);
            grant.setRemoved(true);

            Tasks.runAsync(() -> {
                //Packet packet = new GrantRemovePacket(target.getUuid(), grant.getRank().getUuid());
                RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().removeGrant(target, grant);
                if (response.couldNotConnect()) {
                    player.sendMessage(CC.format("&cCould not connect to API to remove grant. " +
                                    "Adding grant to the queue. Error: %s (%d)",
                            response.getErrorMessage(), response.getCode()));
                } else if (!response.wasSuccessful()) {
                    player.sendMessage(CC.format("&cCould not remove grant: %s (%d)",
                            response.getErrorMessage(), response.getCode()));
                    return;
                }

                player.sendMessage(CC.format(
                        "&aYou've removed a %s&a grant from %s&a.",
                        grant.asRank().getName(),
                        target.getName()
                ));
            });
            return true;
        });
    }
}
