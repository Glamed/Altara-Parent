package games.sparking.altara.grant.commands;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.command.parameter.defaults.Duration;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.GrantClearBackLogEntry;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.grant.menu.GrantRankMenu;
import games.sparking.altara.grant.menu.GrantsMenu;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.utils.Time;
import games.sparking.altara.utils.json.JsonBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GrantCommands {

    @Command(names = {"grant"},
            permission = "zircon.command.grant",
            description = "Grant a rank to a player",
            playerOnly = true,
            async = true)
    public boolean grant(Player sender, @Param(name = "player") Profile target) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender);
        if (profile == null) {
            sender.sendMessage(CC.error(Messages.API_ERROR));
            return true;
        }

        GrantProcedure procedure = new GrantProcedure(profile, target);
        Tasks.run(() -> new GrantRankMenu(procedure).openMenu(sender));
        return true;
    }

    @Command(names = {"grants"},
            permission = "zircon.command.grants",
            description = "View a player's grants",
            playerOnly = true,
            async = true)
    public boolean grants(Player sender, @Param(name = "player") Profile target) {
        RequestResponse response = RequestHandler.get("api/profile/%s/grants", target.getUuid().toString());
        if (!response.wasSuccessful()) {
            sender.sendMessage(CC.error("Unable to load grants.", response.getErrorMessage() + " (" + response.getCode() + ")"));
            return true;
        }

        List<Grant> grants = new ArrayList<>();
        response.asArray().forEach(element -> grants.add(new Grant(element.getAsJsonObject())));
        grants.removeIf(grant -> grant.asRank() == null);

        Tasks.run(() -> new GrantsMenu(target, grants).openMenu(sender));
        return true;
    }

    @Command(names = {"consolegrant", "cgrant"},
            permission = "console",
            description = "Grant a rank without the menu",
            async = true)
    public boolean consoleGrant(CommandSender sender,
                                @Param(name = "player") Profile target,
                                @Param(name = "rank") Rank rank,
                                @Param(name = "duration") Duration duration,
                                @Param(name = "scopes") String scope,
                                @Param(name = "reason", wildcard = true) String reason) {
        List<String> scopes = new ArrayList<>();
        for (String s : scope.split(",")) {
            if (s.isBlank()) continue;
            scopes.add(s.trim().equalsIgnoreCase("global") ? "GLOBAL" : s.trim().toLowerCase());
        }
        if (scopes.isEmpty()) scopes.add("GLOBAL");

        Grant grant = new Grant(target.getUuid(), rank, "Console", System.currentTimeMillis(),
                reason, duration.getDuration(), scopes);

        RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().addGrant(target, grant);
        if (response.couldNotConnect()) {
            sender.sendMessage(CC.notice("Grant queued.", "The API is unreachable, so the grant will be sent when it's back."));
            return true;
        }
        if (!response.wasSuccessful()) {
            sender.sendMessage(CC.error("Unable to grant rank.", response.getErrorMessage() + " (" + response.getCode() + ")"));
            return false;
        }

        sender.sendMessage(CC.success("Rank granted.", "*" + target.getName() + "* now has *" + rank.getName() + "* "
                + (grant.getDuration() == -1 ? "permanently" : "for *" + Time.formatDetailed(grant.getDuration()) + "*") + "."));
        return true;
    }

    @Command(names = {"cleargrants"},
            permission = "console",
            description = "Remove all of a player's grants",
            async = true)
    public boolean clearGrants(CommandSender sender,
                               @Param(name = "player") Profile target,
                               @Param(name = "reason", wildcard = true) String reason) {
        JsonBuilder body = new JsonBuilder()
                .add("removedAt", System.currentTimeMillis())
                .add("removedBy", sender instanceof Player player ? player.getUniqueId().toString() : "Console")
                .add("removedReason", reason);
        RequestResponse response = RequestHandler.post("api/profile/%s/grants/clear", body.build(), target.getUuid().toString());

        if (response.couldNotConnect()) {
            RequestHandler.addToBackLog(new GrantClearBackLogEntry(
                    target.getUuid(),
                    sender instanceof Player player ? player.getUniqueId() : null,
                    response.getRequestBuilder()));
            sender.sendMessage(CC.notice("Clear queued.", "The API is unreachable, so the grants will be cleared when it's back."));
            return true;
        }
        if (!response.wasSuccessful()) {
            sender.sendMessage(CC.error("Unable to clear grants.", response.getErrorMessage() + " (" + response.getCode() + ")"));
            return false;
        }

        int removed = response.asObject().get("removed").getAsInt();
        sender.sendMessage(CC.success("Grants cleared.", "Removed *" + removed + "* " + CC.plural(removed, "grant")
                + " from *" + target.getName() + "*."));
        return true;
    }
}
