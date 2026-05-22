package games.sparking.altara.grant.input;


import games.sparking.blazora.BlazoraPaper;
import games.sparking.blazora.chatinput.ChatInput;
import games.sparking.blazora.connection.RequestResponse;
import games.sparking.blazora.grant.Grant;
import games.sparking.blazora.profile.Profile;
import games.sparking.blazora.task.Tasks;
import games.sparking.blazora.utils.CC;

public class GrantRemoveInput extends ChatInput<String> {

    public GrantRemoveInput(BlazoraPaper zircon, Profile target, Grant grant) {
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
                RequestResponse response = zircon.getBukkitProfileService().removeGrant(target, grant);
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
