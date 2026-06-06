package games.sparking.altara.punishment;

import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PunishManager {

    private final UUID playerUUID;
    private final UUID staffUUID;
    private final List<RestrictionAction> actions;
    private final InfractionType reason;
    private final String message;

    public PunishManager(CommandSender staff, Player target, PunishmentType type, long duration, InfractionType reason) {
        this(staff, target, type, duration, reason, null);
    }

    public PunishManager(CommandSender staff, Player target, PunishmentType type, long duration, InfractionType reason, String message) {
        this(staff, target, List.of(new RestrictionAction(type, duration)), reason, message);
    }

    public PunishManager(CommandSender staff, Player target, List<RestrictionAction> actions, InfractionType reason, String message) {
        this.playerUUID = target.getUniqueId();
        this.staffUUID = (staff instanceof Player player) ? player.getUniqueId() : UUID.fromString("63644fed-6a20-4c35-bef4-be5e1d785a2e");
        this.actions = new ArrayList<>(actions);
        this.reason = reason;
        this.message = message;
    }

    public void issue() {
        if (actions.isEmpty()) {
            return;
        }

        RestrictionAction suspensionAction = actions.stream()
                .filter(action -> action.getType() == PunishmentType.SUSPENSION)
                .findFirst()
                .orElse(null);

        if (suspensionAction != null) {
            handleBan(suspensionAction.getDuration());
            return;
        }

        handleNoticeActions();
    }

    private void handleBan(long duration) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        String reasonText = reason.getDisplayName();
        boolean permanent = duration == -1;

        String banMessage = "&5Your account has been suspended"
                + "\n&7\"" + reasonText + "&7\""
                + "\n\n&7This suspension " + (permanent
                ? "will never expire"
                : "will expire in &d" + Time.formatDetailed(duration) + "&7")
                + ". Visit &d&ncrystalwars.net/appeal&r&7 to submit an appeal";

        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', banMessage));
    }

    private void handleNoticeActions() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) return;

        player.sendMessage(CC.genLine(NamedTextColor.DARK_GRAY, NamedTextColor.DARK_RED, NamedTextColor.RED, CC.format("Account Action")));
        player.sendMessage(CC.format(" &7Your recent activity violated our Terms of Service"));

        if (message != null) {
            displayMessageContent(player);
        }

        player.sendMessage("");
        player.sendMessage(CC.format(" &7We took these actions&8:"));

        for (String line : buildActionLines()) {
            player.sendMessage(CC.format("  &4x&c " + line));
        }

        sendReasonSection(player);
        player.sendMessage(CC.genLine(NamedTextColor.DARK_GRAY, NamedTextColor.DARK_RED));
    }

    private List<String> buildActionLines() {
        List<String> lines = new ArrayList<>();

        if (message != null) {
            lines.add("This content has been removed so no one can see it.");
        }

        for (RestrictionAction action : actions) {
            lines.add(action.getType().getActionLine(action.getDuration()));
        }

        return lines;
    }

    private void sendReasonSection(Player player) {
        player.sendMessage("");

        if (reason == InfractionType.TEMP_AUTOMATED) {
            player.sendMessage(CC.format(" &7Why this action was taken&8:"));
            player.sendMessage(CC.format("  &7This temporary action was triggered by our"));
            player.sendMessage(CC.format("  &7automated moderation systems and is pending"));
            player.sendMessage(CC.format("  &7manual review by our Trust & Safety team."));
            player.sendMessage("");
            player.sendMessage(CC.format(" &7You cannot appeal this action at this time."));
            player.sendMessage(CC.format(" &7Please review our &b&nCommunity Guidelines&7 while we complete our review."));
        } else {
            player.sendMessage(CC.format(" &7Why we took these actions&8:"));
            player.sendMessage(CC.format("  &7Our trust and safety team uses automation and manual"));
            player.sendMessage(CC.format("  &7review to enforce our rules. We believe that you have"));
            player.sendMessage(CC.format("  &7violated our community guidelines on &d%s&7.", reason.getDisplayName()));
            player.sendMessage("");
            player.sendMessage(CC.format(" &7Please review our &b&nCommunity Guidelines&7."));
            player.sendMessage(CC.format(" &7Did we make a mistake? &b&nLet us know&7!"));
        }
    }

    private void displayMessageContent(Player player) {
        player.sendMessage("");
        player.sendMessage(CC.format("  &8&l→ &8[&7Member&8]&7 " + player.getName() + " &8»&f " + message));
        player.sendMessage("");
    }
}
