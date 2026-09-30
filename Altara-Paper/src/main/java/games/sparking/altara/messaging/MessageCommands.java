package games.sparking.altara.messaging;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chat.impl.DirectMessageChannel;
import games.sparking.altara.chat.impl.StaffChannel;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentMessages;
import games.sparking.altara.punishment.listeners.PunishmentListener;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.utils.PagedMessage;
import games.sparking.altara.uuid.UUIDCache;
import games.sparking.altara.uuid.UUIDUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageCommands {

    private static final String SPY_ALL = "@ALL";

    private final Map<UUID, UUID> lastConversation = new ConcurrentHashMap<>();

    // ── /msg, /reply ─────────────────────────────────────────────────────────

    @Command(names = {"message", "msg", "m", "tell", "whisper", "w"}, permission = "player",
            description = "Send a private message")
    public boolean message(Player sender,
                           @Param(name = "player") Player target,
                           @Param(name = "message", wildcard = true) String message) {
        if (sender.equals(target)) {
            sender.sendMessage(CC.error("Invalid player.", "You can't message yourself."));
            return false;
        }

        send(sender, target, message);
        return true;
    }

    @Command(names = {"reply", "r", "respond"}, permission = "player",
            description = "Reply to your last conversation")
    public boolean reply(Player sender, @Param(name = "message", wildcard = true) String message) {
        UUID targetUuid = lastConversation.get(sender.getUniqueId());
        if (targetUuid == null) {
            sender.sendMessage(CC.error("No conversation.", "You haven't messaged anyone recently."));
            return false;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            String name = UUIDCache.getName(targetUuid);
            sender.sendMessage(CC.error("Invalid player.", "*" + (name == null ? "That player" : name) + "* is no longer online."));
            return false;
        }

        send(sender, target, message);
        return true;
    }

    // ── Toggles ──────────────────────────────────────────────────────────────

    @Command(names = {"togglemessages", "toggleprivatemessages", "togglepm", "tpm"}, permission = "player",
            description = "Toggle private messages")
    public boolean toggleMessages(Player sender) {
        boolean now = !AltaraSettings.PRIVATE_MESSAGES.get(sender);
        AltaraSettings.PRIVATE_MESSAGES.set(sender, now);
        sender.sendMessage(CC.notice("Settings updated.", "Private messages are now *" + (now ? "enabled" : "disabled") + "*."));
        return true;
    }

    @Command(names = {"togglesounds", "sounds"}, permission = "player",
            description = "Toggle message sounds")
    public boolean toggleSounds(Player sender) {
        boolean now = !AltaraSettings.MESSAGING_SOUNDS.get(sender);
        AltaraSettings.MESSAGING_SOUNDS.set(sender, now);
        sender.sendMessage(CC.notice("Settings updated.", "Message sounds are now *" + (now ? "enabled" : "disabled") + "*."));
        return true;
    }

    // ── Social spy ───────────────────────────────────────────────────────────

    @Command(names = {"socialspy list"}, permission = "altara.command.socialspy.list", playerOnly = true,
            description = "View who you're spying on")
    public boolean socialSpyList(Player sender, @Param(name = "page", defaultValue = "1") int page) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        new PagedMessage<String>() {
            @Override protected String title() { return "Social Spy"; }
            @Override protected Panel panel() { return Panel.STAFF; }
            @Override protected String pageCommand() { return "/socialspy list"; }
            @Override protected String emptyMessage() { return "You aren't spying on anyone."; }

            @Override
            protected Component format(String entry) {
                if (entry.equals(SPY_ALL)) return CC.item("All players");
                String name = UUIDUtils.isUUID(entry) ? UUIDCache.getName(UUID.fromString(entry)) : entry;
                return CC.item(name == null ? entry : name);
            }
        }.display(sender, profile.getOptions().getSocialSpy(), page);
        return true;
    }

    @Command(names = {"socialspy add"}, permission = "altara.command.socialspy.add", playerOnly = true, async = true,
            description = "Spy on a player's messages")
    public boolean socialSpyAdd(Player sender, @Param(name = "player") Profile target) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        List<String> spy = profile.getOptions().getSocialSpy();
        if (spy.contains(target.getUuid().toString())) {
            sender.sendMessage(CC.error("Already spying.", "You're already spying on *" + target.getName() + "*."));
            return false;
        }

        spy.add(target.getUuid().toString());
        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Spy added.", "You're now spying on *" + target.getName() + "*."));
        return true;
    }

    @Command(names = {"socialspy addall"}, permission = "altara.command.socialspy.add", playerOnly = true,
            description = "Spy on everyone's messages")
    public boolean socialSpyAddAll(Player sender) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        if (profile.getOptions().getSocialSpy().contains(SPY_ALL)) {
            sender.sendMessage(CC.error("Already spying.", "You're already spying on all players."));
            return false;
        }

        profile.getOptions().getSocialSpy().add(SPY_ALL);
        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Spy added.", "You're now spying on all players."));
        return true;
    }

    @Command(names = {"socialspy remove"}, permission = "altara.command.socialspy.remove", playerOnly = true, async = true,
            description = "Stop spying on a player")
    public boolean socialSpyRemove(Player sender, @Param(name = "player") Profile target) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        if (!profile.getOptions().getSocialSpy().remove(target.getUuid().toString())) {
            sender.sendMessage(CC.error("Not spying.", "You aren't spying on *" + target.getName() + "*."));
            return false;
        }

        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Spy removed.", "You're no longer spying on *" + target.getName() + "*."));
        return true;
    }

    @Command(names = {"socialspy removeall"}, permission = "altara.command.socialspy.remove", playerOnly = true,
            description = "Stop spying on everyone")
    public boolean socialSpyRemoveAll(Player sender) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        if (!profile.getOptions().getSocialSpy().remove(SPY_ALL)) {
            sender.sendMessage(CC.error("Not spying.", "You aren't spying on all players."));
            return false;
        }

        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Spy removed.", "You're no longer spying on all players."));
        return true;
    }

    // ── Ignore ───────────────────────────────────────────────────────────────

    @Command(names = {"ignore list"}, permission = "player", description = "View players you're ignoring")
    public boolean ignoreList(Player sender, @Param(name = "page", defaultValue = "1") int page) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        new PagedMessage<UUID>() {
            @Override protected String title() { return "Ignored Players"; }
            @Override protected String pageCommand() { return "/ignore list"; }
            @Override protected String emptyMessage() { return "You aren't ignoring anyone."; }

            @Override
            protected Component format(UUID uuid) {
                String name = UUIDCache.getName(uuid);
                return CC.item(name == null ? uuid.toString() : name);
            }
        }.display(sender, profile.getOptions().getIgnoring(), page);
        return true;
    }

    @Command(names = {"ignore clear"}, permission = "player", description = "Stop ignoring everyone")
    public boolean ignoreClear(Player sender) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        int size = profile.getOptions().getIgnoring().size();
        if (size == 0) {
            sender.sendMessage(CC.info("You aren't ignoring anyone."));
            return false;
        }

        profile.getOptions().getIgnoring().clear();
        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Ignore list cleared.", "You're no longer ignoring *" + size + "* " + CC.plural(size, "player") + "."));
        return true;
    }

    @Command(names = {"ignore add"}, permission = "player", async = true, description = "Ignore a player")
    public boolean ignoreAdd(Player sender, @Param(name = "player") Profile target) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        if (target.getUuid().equals(sender.getUniqueId())) {
            sender.sendMessage(CC.error("Invalid player.", "You can't ignore yourself."));
            return false;
        }

        if (profile.getOptions().getIgnoring().contains(target.getUuid())) {
            sender.sendMessage(CC.error("Already ignored.", "You're already ignoring *" + target.getCurrentName() + "*."));
            return false;
        }

        profile.getOptions().getIgnoring().add(target.getUuid());
        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Player ignored.", "You'll no longer receive messages from *" + target.getCurrentName() + "*."));
        return true;
    }

    @Command(names = {"ignore remove"}, permission = "player", async = true, description = "Stop ignoring a player")
    public boolean ignoreRemove(Player sender, @Param(name = "player") Profile target) {
        Profile profile = profile(sender);
        if (profile == null) return true;

        if (!profile.getOptions().getIgnoring().remove(target.getUuid())) {
            sender.sendMessage(CC.error("Not ignored.", "You aren't ignoring *" + target.getCurrentName() + "*."));
            return false;
        }

        profile.save(() -> {}, true);
        sender.sendMessage(CC.success("Player unignored.", "You'll receive messages from *" + target.getCurrentName() + "* again."));
        return true;
    }

    // ── Core ─────────────────────────────────────────────────────────────────

    private void send(Player senderPlayer, Player targetPlayer, String message) {
        Profile sender = profile(senderPlayer);
        Profile target = Altara.getSharedInstance().getProfileService().getProfile(targetPlayer);
        if (sender == null) return;
        if (target == null) {
            senderPlayer.sendMessage(CC.error(Messages.PLAYER_OFFLINE));
            return;
        }

        Punishment mute = PunishmentListener.activeMute(sender.getUuid());
        if (mute != null) {
            PunishmentMessages.chatRestricted(mute, senderPlayer.getName()).forEach(senderPlayer::sendMessage);
            return;
        }

        boolean senderStaff = senderPlayer.hasPermission(StaffChannel.PERMISSION);
        String targetName = target.getCurrentName();

        if (!senderStaff) {
            if (!AltaraSettings.PRIVATE_MESSAGES.get(senderPlayer)) {
                senderPlayer.sendMessage(CC.error("Messages disabled.", "Use */tpm* to turn private messages back on."));
                return;
            }
            if (!AltaraSettings.PRIVATE_MESSAGES.get(targetPlayer)) {
                senderPlayer.sendMessage(CC.error("Messages disabled.", "*" + targetName + "* isn't accepting private messages."));
                return;
            }
            if (sender.getOptions().getIgnoring().contains(target.getUuid())) {
                senderPlayer.sendMessage(CC.error("Player ignored.", "Use */ignore remove " + targetName + "* to message them."));
                return;
            }
            if (target.getOptions().getIgnoring().contains(sender.getUuid())) {
                senderPlayer.sendMessage(CC.error("Unable to message.", "*" + targetName + "* isn't accepting your messages."));
                return;
            }
        }

        if (Altara.getSharedInstance().getProfilerService().isShadowMuted(sender.getUuid())) {
            DirectMessageChannel.getInstance().dispatchShadowMuted(sender, target, message);
            return;
        }

        DirectMessageChannel.getInstance().dispatch(sender, target, message, findSpies(sender, target));

        if (AltaraSettings.MESSAGING_SOUNDS.get(targetPlayer)) {
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.1f);
        }

        lastConversation.put(sender.getUuid(), target.getUuid());
        lastConversation.put(target.getUuid(), sender.getUuid());

        if (senderStaff && !AltaraSettings.PRIVATE_MESSAGES.get(senderPlayer)) {
            senderPlayer.sendMessage(CC.info("Your private messages are disabled, so *" + targetName + "* can't reply."));
        }
    }

    private List<Profile> findSpies(Profile sender, Profile target) {
        List<Profile> spies = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("altara.socialspy")) continue;
            if (online.getUniqueId().equals(sender.getUuid()) || online.getUniqueId().equals(target.getUuid())) continue;

            Profile spyProfile = Altara.getSharedInstance().getProfileService().getProfile(online.getUniqueId());
            if (spyProfile == null) continue;

            List<String> spy = spyProfile.getOptions().getSocialSpy();
            if (spy.contains(SPY_ALL)
                    || spy.contains(sender.getUuid().toString())
                    || spy.contains(target.getUuid().toString())) {
                spies.add(spyProfile);
            }
        }
        return spies;
    }

    private static Profile profile(Player player) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(player);
        if (profile == null) {
            player.sendMessage(CC.error(Messages.API_ERROR));
        }
        return profile;
    }
}
