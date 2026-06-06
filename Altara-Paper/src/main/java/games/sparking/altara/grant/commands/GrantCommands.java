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
import games.sparking.altara.profile.packet.ProfileUpdatePacket;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class GrantCommands {

    @Command(names = {"grant"},
            permission = "zircon.command.grant",
            description = "Grant a rank to a player",
            playerOnly = true,
            async = true)
    public boolean grant(Player sender, @Param(name = "player") Profile target) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender);
        GrantProcedure grantProcedure = new GrantProcedure(profile, target);
        Tasks.run(() -> new GrantRankMenu(grantProcedure).openMenu(sender));
        return true;
    }

    @Command(names = {"grants"},
            permission = "zircon.command.grants",
            description = "Check a players grants",
            playerOnly = true,
            async = true)
    public boolean grants(Player sender, @Param(name = "player") Profile target) {
        sender.sendMessage(CC.format("<yellow>Loading grants of " + target.getName() + "..."));
        RequestResponse response = RequestHandler.get("api/profile/%s/grants", target.getUuid().toString());
        if (!response.wasSuccessful()) {
            sender.sendMessage(CC.format("<red>Could not load grants: %s (%d)",
                    response.getErrorMessage(), response.getCode()));
            return true;
        }

        List<Grant> grants = new ArrayList<>();
        response.asArray().forEach(element -> Bukkit.broadcastMessage(String.valueOf(element.getAsJsonObject())));
        response.asArray().forEach(element -> grants.add(new Grant(element.getAsJsonObject())));
        grants.removeIf(grant -> grant.getRank() == null);
        Tasks.run(() -> new GrantsMenu(target, grants).openMenu(sender));
        return true;
    }

    @Command(names = {"consolegrant", "cgrant"},
            permission = "console",
            description = "Grant a rank to a player",
            async = true)
    public boolean consolegrant(CommandSender sender,
                                @Param(name = "player") Profile target,
                                @Param(name = "rank") Rank rank,
                                @Param(name = "duration") Duration duration,
                                @Param(name = "scopes") String scope,
                                @Param(name = "reason", wildcard = true) String reason) {
        List<String> scopes = new ArrayList<>();
        if (!scope.contains(",")) {
            if (scope.equalsIgnoreCase("global")) {
                scopes.add("GLOBAL");
            } else {
                scopes.add(scope.toLowerCase());
            }
        } else {
            for (String s : scope.split(",")) {
                if (s.equalsIgnoreCase("global")) {
                    scopes.add("GLOBAL");
                } else {
                    scopes.add(s.toLowerCase());
                }
            }
        }

        Grant grant = new Grant(
                target.getUuid(),
                rank,
                sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "Console",
                System.currentTimeMillis(),
                reason,
                duration.getDuration(),
                scopes
        );

        //Packet packet = new GrantAddPacket(target.getUuid(), rank.getUuid(), duration.getDuration());
        RequestResponse response = AltaraPaper.getPaperInstance().getBukkitProfileService().addGrant(target, grant);
        if (response.couldNotConnect()) {
            sender.sendMessage(CC.format("<red>Could not connect to API to create grant. " +
                            "Adding grant to the queue. Error: %s (%d)",
                    response.getErrorMessage(), response.getCode()));
        } else if (!response.wasSuccessful()) {
            sender.sendMessage(CC.format("<red>Could not create grant: %s (%d)",
                    response.getErrorMessage(), response.getCode()));
            return false;
        }

        if (grant.getDuration() == -1)
            sender.sendMessage(CC.format(
                    "<green>You've <yellow>permanently <green>granted %s<green> the %s<green> rank.",
                    target.getName(),
                    rank.getName()
            ));
        else
            sender.sendMessage(CC.format(
                    "<green>You've granted %s<green> the %s<green> rank for <yellow>%s<green>.",
                    target.getName(),
                    rank.getName(),
                    Time.formatDetailed(grant.getDuration())
            ));
        return true;
    }

    @Command(names = {"cleargrants"},
            permission = "console",
            description = "Clear all active grants of a player",
            async = true)
    public boolean clearGrants(CommandSender sender,
                               @Param(name = "player") Profile target,
                               @Param(name = "reason", wildcard = true) String reason) {
        JsonBuilder body = new JsonBuilder();
        body.add("removedAt", System.currentTimeMillis());
        body.add("removedBy", sender instanceof Player ? ((Player) sender).getUniqueId().toString() : "Console");
        body.add("removedReason", reason);
        RequestResponse response = RequestHandler.post("api/profile/%s/grants/clear",
                body.build(), target.getUuid().toString());

        if (response.couldNotConnect()) {
            sender.sendMessage(CC.format("<red>Could not connect to API to clear grants. " +
                            "Adding request to the queue. Error: %s (%d)",
                    response.getErrorMessage(), response.getCode()));
            RequestHandler.addToBackLog(new GrantClearBackLogEntry(
                    target.getUuid(),
                    sender instanceof Player ? ((Player) sender).getUniqueId() : null,
                    response.getRequestBuilder()
            ));
            return true;
        } else if (!response.wasSuccessful()) {
            sender.sendMessage(CC.format("<red>Could not clear grants: %s (%d)",
                    response.getErrorMessage(), response.getCode()));
            return false;
        }

        sender.sendMessage(CC.format("<green>Removed <yellow>%d <green>grants of %s<green>.",
                response.asObject().get("removed").getAsInt(), target.getName()));

       new ProfileUpdatePacket(target.getUuid()).publish();
        return true;
    }

    public boolean canGrant(CommandSender sender, Rank rank) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(player);

        if (rank.isDefaultRank()) {
            return false;
        }

        if (profile.getRealCurrentGrant().asRank().getWeight() >= Altara.getSharedInstance().getMainConfig().getOwnerWeight()
                || profile.getUuid().equals(UUID.fromString("c7d53cda-a00d-465b-ba55-c2f684ad4ae3"))) {
            return true;
        }

        return profile.getRealCurrentGrant().asRank().getWeight() > rank.getWeight() && player.hasPermission(
                "invicuts.grant." + rank.getName());
    }
}
