package games.sparking.altara.grant.input;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chatinput.ChatInput;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GrantRemoveInput extends ChatInput<String> {

    public GrantRemoveInput(Profile target, Grant grant) {
        super(String.class);
        text(
                CC.noticeMsg("", "Please enter the reason for removing this grant."),
                CC.noticeMsg("", "You can type *cancel* at any time to exit this process.")
        );
        escapeMessage(CC.errorMsg("You cancelled the grant removal."));

        accept((player, input) -> {
            grant.setRemovedAt(System.currentTimeMillis());
            grant.setRemovedBy(player.getUniqueId().toString());
            grant.setRemovedReason(input);
            grant.setRemoved(true);

            Tasks.runAsync(() -> {
                RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().removeGrant(target, grant);
                if (response.couldNotConnect()) {
                    player.sendMessage(CC.format(
                            "<red>Could not connect to API to remove grant. Adding to queue. Error: %s (%d)</red>",
                            response.getErrorMessage(), response.getCode()));
                } else if (!response.wasSuccessful()) {
                    player.sendMessage(CC.format(
                            "<red>Could not remove grant: %s (%d)</red>",
                            response.getErrorMessage(), response.getCode()));
                    return;
                }

                player.sendMessage(Component.text()
                        .append(Component.text("You've removed a ", NamedTextColor.GREEN))
                        .append(Component.text(grant.asRank().getName(), NamedTextColor.WHITE))
                        .append(Component.text(" grant from ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.WHITE))
                        .append(Component.text(".", NamedTextColor.GREEN))
                        .build());
            });
            return true;
        });
    }
}
