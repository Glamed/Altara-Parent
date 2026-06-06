package games.sparking.altara.messaging;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.chat.impl.DirectMessageChannel;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PagedMessage;
import games.sparking.altara.uuid.UUIDCache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class MessageCommands {

    private final Map<UUID, UUID> lastConversation = new HashMap<>();

    // ── /msg ────────────────────────────────────────────────────────────────

    @Command(names = {"message", "msg", "m", "tell", "whisper", "w"}, permission = "player")
    public boolean message(Player sender,
                           @Param(name = "player") Player target,
                           @Param(name = "message", wildcard = true) String message) {

        Profile senderProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        Profile targetProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(target);
        if (senderProfile == null || targetProfile == null) return false;

        handleMessage(senderProfile, targetProfile, message);
        return true;
    }

    // ── /reply ───────────────────────────────────────────────────────────────

    @Command(names = {"reply", "r", "respond"}, permission = "player")
    public boolean reply(Player sender,
                         @Param(name = "message", wildcard = true) String message) {

        UUID targetUuid = lastConversation.get(sender.getUniqueId());
        if (targetUuid == null) {
            sender.sendMessage(CC.format("<red>You are not in a conversation."));
            return false;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(CC.format("<yellow>%s <red>is no longer online.",
                    UUIDCache.getName(targetUuid)));
            return false;
        }

        Profile senderProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        Profile targetProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(target);
        if (senderProfile == null || targetProfile == null) return false;

        handleMessage(senderProfile, targetProfile, message);
        return true;
    }

    // ── /togglemessages ─────────────────────────────────────────────────────

    @Command(names = {"togglemessages", "toggleprivatemessages", "togglepm", "tpm"},
            permission = "player")
    public boolean toggleMessages(Player sender) {

        boolean now = !AltaraSettings.PRIVATE_MESSAGES.get(sender);
        AltaraSettings.PRIVATE_MESSAGES.set(sender, now);

        sender.sendMessage(CC.format("<yellow>Private messages <gray>%s<yellow>.",
                CC.strip(CC.colorBoolean(now))));

        return true;
    }

    // ── /togglesounds ───────────────────────────────────────────────────────

    @Command(names = {"togglesounds", "sounds"}, permission = "player")
    public boolean toggleSounds(Player sender) {

        boolean now = !AltaraSettings.MESSAGING_SOUNDS.get(sender);
        AltaraSettings.MESSAGING_SOUNDS.set(sender, now);

        sender.sendMessage(CC.format("<yellow>Messaging sounds <gray>%s<yellow>.",
                CC.strip(CC.colorBoolean(now))));

        return true;
    }

    // ── /socialspy list ─────────────────────────────────────────────────────

    @Command(names = {"socialspy list"},
            permission = "altara.command.socialspy.list",
            playerOnly = true)
    public boolean socialSpyList(Player sender,
                                 @Param(name = "page", defaultValue = "1") int page) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (profile.getOptions().getSocialSpy().isEmpty()) {
            sender.sendMessage(CC.errorMsg("Spy List", "You are not spying on anyone."));
            return false;
        }

        new PagedMessage<String>() {

            @Override
            public List<String> getHeader(int page, int maxPages) {
                return Arrays.asList(
                        CC.strip(CC.genLine(NamedTextColor.RED, NamedTextColor.DARK_GRAY)),
                        "Spy List (Page " + page + "/" + maxPages + ")"
                );
            }

            @Override
            public List<String> getFooter(int page, int maxPages) {
                return Arrays.asList(
                        " ",
                        "Use /socialspy list <page> for more.",
                        CC.strip(CC.genLine(NamedTextColor.RED, NamedTextColor.DARK_GRAY))
                );
            }

            @Override
            public void send(CommandSender sender, String s) {
                String name = isUuid(s)
                        ? UUIDCache.getName(UUID.fromString(s))
                        : s;

                sender.sendMessage(Component.text(" - " + name, NamedTextColor.YELLOW));
            }

        }.display(sender, profile.getOptions().getSocialSpy(), page);

        return true;
    }

    // ── socialspy add ───────────────────────────────────────────────────────

    @Command(names = {"socialspy add"},
            permission = "altara.command.socialspy.add",
            playerOnly = true,
            async = true)
    public boolean socialSpyAdd(Player sender, @Param(name = "target") Profile target) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (profile.getOptions().getSocialSpy().contains(target.getUuid().toString())) {
            sender.sendMessage(CC.errorMsg("Invalid player", "You are already spying on " + target.getName() + "."));
            return false;
        }

        profile.getOptions().getSocialSpy().add(target.getUuid().toString());
        sender.sendMessage(CC.noticeMsg("", "Now spying on " + target.getName() + "."));
        profile.save(() -> {}, true);

        return true;
    }

    // ── socialspy addall ────────────────────────────────────────────────────

    @Command(names = {"socialspy addall"},
            permission = "altara.command.socialspy.add",
            playerOnly = true)
    public boolean socialSpyAddAll(Player sender) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (profile.getOptions().getSocialSpy().contains("@ALL")) {
            sender.sendMessage(CC.errorMsg("Invalid player.", "Already spying on all players."));
            return false;
        }

        profile.getOptions().getSocialSpy().add("@ALL");
        sender.sendMessage(CC.noticeMsg("", "Now spying on all players."));
        profile.save(() -> {}, true);

        return true;
    }

    // ── socialspy remove ────────────────────────────────────────────────────

    @Command(names = {"socialspy remove"},
            permission = "altara.command.socialspy.add",
            playerOnly = true,
            async = true)
    public boolean socialSpyRemove(Player sender, @Param(name = "target") Profile target) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (!profile.getOptions().getSocialSpy().contains(target.getUuid().toString())) {
            sender.sendMessage(CC.errorMsg("Invalid player.", "Not spying on " + target.getName() + "."));
            return false;
        }

        profile.getOptions().getSocialSpy().remove(target.getUuid().toString());
        sender.sendMessage(CC.noticeMsg("No longer spying on %s.", target.getName()));
        profile.save(() -> {}, true);

        return true;
    }

    // ── socialspy removeall ────────────────────────────────────────────────

    @Command(names = {"socialspy removeall"},
            permission = "altara.command.socialspy.remove",
            playerOnly = true)
    public boolean socialSpyRemoveAll(Player sender) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (!profile.getOptions().getSocialSpy().contains("@ALL")) {
            sender.sendMessage(CC.errorMsg("Spy", "Not spying on all players."));
            return false;
        }

        profile.getOptions().getSocialSpy().remove("@ALL");
        sender.sendMessage(CC.successMsg("Spy", "No longer spying on all players."));
        profile.save(() -> {}, true);

        return true;
    }

    // ── ignore list ────────────────────────────────────────────────────────

    @Command(names = {"ignore list"}, permission = "player")
    public boolean ignoreList(Player sender,
                              @Param(name = "page", defaultValue = "1") int page) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (profile.getOptions().getIgnoring().isEmpty()) {
            sender.sendMessage(CC.noticeMsg("Ignore", "You are not ignoring anyone."));
            return false;
        }

        new PagedMessage<UUID>() {

            @Override
            public List<String> getHeader(int page, int maxPages) {
                return Arrays.asList(
                        CC.strip(CC.genLine(NamedTextColor.RED, NamedTextColor.DARK_GRAY)),
                        "Ignore List (Page " + page + "/" + maxPages + ")"
                );
            }

            @Override
            public List<String> getFooter(int page, int maxPages) {
                return Arrays.asList(
                        " ",
                        "Use /ignore list (page) for more.",
                        CC.strip(CC.genLine(NamedTextColor.RED, NamedTextColor.DARK_GRAY))
                );
            }

            @Override
            public void send(CommandSender sender, UUID uuid) {
                sender.sendMessage(Component.text(" - " + UUIDCache.getName(uuid), NamedTextColor.YELLOW));
            }

        }.display(sender, profile.getOptions().getIgnoring(), page);

        return true;
    }

    @Command(names = {"ignore clear"}, permission = "player")
    public boolean ignoreClear(Player sender) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        int size = profile.getOptions().getIgnoring().size();
        profile.getOptions().getIgnoring().clear();

        sender.sendMessage(CC.noticeMsg(
                "",
                "No longer ignoring " + size + " " + CC.plural(size, "player") + "."
        ));
        profile.save(() -> {}, true);

        return true;
    }

    @Command(names = {"ignore add"}, permission = "player", async = true)
    public boolean ignoreAdd(Player sender, @Param(name = "player") Profile target) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (profile.getOptions().getIgnoring().contains(target.getUuid())) {
            sender.sendMessage(CC.errorMsg("Invalid player.", "You are Already ignoring *" + target.getCurrentName() + "*."));
            return false;
        }

        profile.getOptions().getIgnoring().add(target.getUuid());
        sender.sendMessage(CC.successMsg("", "You are now ignoring *" + target.getCurrentName() + "*."));
        profile.save(() -> {}, true);

        return true;
    }

    @Command(names = {"ignore remove"}, permission = "player", async = true)
    public boolean ignoreRemove(Player sender, @Param(name = "player") Profile target) {

        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);

        if (!profile.getOptions().getIgnoring().contains(target.getUuid())) {
            sender.sendMessage(CC.errorMsg("Invalid player.", "You are not ignoring *" + target.getCurrentName() + "*."));
            return false;
        }

        profile.getOptions().getIgnoring().remove(target.getUuid());
        sender.sendMessage(CC.noticeMsg("", "No longer ignoring *" + target.getCurrentName() + "*."));
        profile.save(() -> {}, true);

        return true;
    }

    // ── core ────────────────────────────────────────────────────────────────

    private void handleMessage(Profile sender, Profile target, String message) {

        Player senderPlayer = sender.player();
        Player targetPlayer = target.player();

        boolean senderStaff = senderPlayer.hasPermission("altara.staff");

        if (!AltaraSettings.PRIVATE_MESSAGES.get(senderPlayer) && !senderStaff) {
            senderPlayer.sendMessage(CC.errorMsg("You have private messages disabled."));
            return;
        }

        if (!AltaraSettings.PRIVATE_MESSAGES.get(targetPlayer) && !senderStaff) {
            senderPlayer.sendMessage(CC.errorMsg("", target.getCurrentName() + " has private messages disabled."
            ));
            return;
        }

        if (sender.getOptions().getIgnoring().contains(target.getUuid()) && !senderStaff) {
            senderPlayer.sendMessage(CC.errorMsg("Invalid player.", "You are ignoring *" + target.getCurrentName() + "*."));
            return;
        }

        if (target.getOptions().getIgnoring().contains(sender.getUuid()) && !senderStaff) {
            senderPlayer.sendMessage(CC.errorMsg("Invalid player.", "*" + target.getCurrentName() + "* is ignoring you."));
            return;
        }

        List<Profile> spies = new ArrayList<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("altara.socialspy")) continue;
            if (online.getUniqueId().equals(sender.getUuid())) continue;
            if (online.getUniqueId().equals(target.getUuid())) continue;

            Profile spyProfile = Altara.getSharedInstance()
                    .getProfileService()
                    .getProfile(online.getUniqueId());

            if (spyProfile == null) continue;

            List<String> spy = spyProfile.getOptions().getSocialSpy();

            if (spy.contains("@ALL")
                    || spy.contains(sender.getUuid().toString())
                    || spy.contains(target.getUuid().toString())) {
                spies.add(spyProfile);
            }
        }

        DirectMessageChannel.getInstance().dispatch(sender, target, message, spies);

        if (AltaraSettings.MESSAGING_SOUNDS.get(targetPlayer)) {
            targetPlayer.playSound(targetPlayer.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.1f);
        }

        lastConversation.put(sender.getUuid(), target.getUuid());
        lastConversation.put(target.getUuid(), sender.getUuid());

        if (!AltaraSettings.PRIVATE_MESSAGES.get(senderPlayer) && senderStaff) {
            senderPlayer.sendMessage(CC.noticeMsg("",
                    "You have private messages disabled - they cannot reply."));
        }
    }

    private static boolean isUuid(String s) {
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}