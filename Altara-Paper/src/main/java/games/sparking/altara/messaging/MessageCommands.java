package games.sparking.altara.messaging;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.Altara;
import games.sparking.altara.chat.impl.DirectMessageChannel;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.playersetting.AltaraSettings;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.PagedMessage;
import games.sparking.altara.uuid.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class MessageCommands {

    /** Tracks the last conversation partner per player for /reply. */
    private final Map<UUID, UUID> lastConversation = new HashMap<>();

    // ── /msg ──────────────────────────────────────────────────────────────────

    @Command(names = {"message", "msg", "m", "tell", "whisper", "w"},
            permission = "player")
    public boolean message(Player sender,
                           @Param(name = "player") Player target,
                           @Param(name = "message", wildcard = true) String message) {
        Profile senderProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        Profile targetProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(target);
        if (senderProfile == null || targetProfile == null) return false;

        handleMessage(senderProfile, targetProfile, message);
        return true;
    }

    // ── /reply ────────────────────────────────────────────────────────────────

    @Command(names = {"reply", "r", "respond"},
            permission = "player")
    public boolean reply(Player sender,
                         @Param(name = "message", wildcard = true) String message) {
        UUID targetUuid = lastConversation.get(sender.getUniqueId());
        if (targetUuid == null) {
            sender.sendMessage(CC.format("<red>You are not in a conversation."));
            return false;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(CC.format("<yellow>" + UUIDCache.getName(targetUuid) + " <red>is no longer online."));
            return false;
        }

        Profile senderProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        Profile targetProfile = AltaraPaper.getPaperInstance().getProfileService().getProfile(target);
        if (senderProfile == null || targetProfile == null) return false;

        handleMessage(senderProfile, targetProfile, message);
        return true;
    }

    // ── /togglemessages ───────────────────────────────────────────────────────

    @Command(names = {"togglemessages", "toggleprivatemessages", "togglepm", "tpm"},
            permission = "player")
    public boolean toggleMessages(Player sender) {
        boolean now = !AltaraSettings.PRIVATE_MESSAGES.get(sender);
        AltaraSettings.PRIVATE_MESSAGES.set(sender, now);
        sender.sendMessage(CC.format("<yellow>Private messages <reset>" +
                CC.colorBoolean(now) + "<yellow>."));
        return true;
    }

    // ── /togglesounds ─────────────────────────────────────────────────────────

    @Command(names = {"togglesounds", "sounds"},
            permission = "player")
    public boolean toggleSounds(Player sender) {
        boolean now = !AltaraSettings.MESSAGING_SOUNDS.get(sender);
        AltaraSettings.MESSAGING_SOUNDS.set(sender, now);
        sender.sendMessage(CC.format("<yellow>Messaging sounds <reset>" +
                CC.colorBoolean(now) + "<yellow>."));
        return true;
    }

    // ── /socialspy ────────────────────────────────────────────────────────────

    @Command(names = {"socialspy list"},
            permission = "altara.command.socialspy.list",
            playerOnly = true)
    public boolean socialSpyList(Player sender,
                                  @Param(name = "page", defaultValue = "1") int page) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (profile.getOptions().getSocialSpy().isEmpty()) {
            sender.sendMessage(CC.format("<red>You are not spying on anyone."));
            return false;
        }

        new PagedMessage<String>() {
            @Override public List<String> getHeader(int page, int maxPages) {
                return Arrays.asList(CC.SMALL_CHAT_BAR.toString(),
                        CC.RED + "Spy List " + CC.GRAY + "(Page " + page + "/" + maxPages + ")");
            }
            @Override public List<String> getFooter(int page, int maxPages) {
                return Arrays.asList(" ",
                        CC.YELLOW + "Use " + CC.RED + "/socialspy list <page> " + CC.YELLOW + "for more.",
                        CC.SMALL_CHAT_BAR.toString());
            }
            @Override public void send(CommandSender sender, String s) {
                String name = isUuid(s) ? UUIDCache.getName(UUID.fromString(s)) : s;
                sender.sendMessage(CC.WHITE + " - " + CC.YELLOW + name);
            }
        }.display(sender, profile.getOptions().getSocialSpy(), page);
        return true;
    }

    @Command(names = {"socialspy add"},
            permission = "altara.command.socialspy.add",
            playerOnly = true,
            async = true)
    public boolean socialSpyAdd(Player sender, @Param(name = "target") Profile target) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (profile.getOptions().getSocialSpy().contains(target.getUuid().toString())) {
            sender.sendMessage(CC.format("<red>You are already spying on <yellow>" + target.getName() + "<red>."));
            return false;
        }
        profile.getOptions().getSocialSpy().add(target.getUuid().toString());
        sender.sendMessage(CC.format("<yellow>Now spying on <red>" + target.getName() + "<yellow>."));
        profile.save(() -> {}, true);
        return true;
    }

    @Command(names = {"socialspy addall"},
            permission = "altara.command.socialspy.add",
            playerOnly = true)
    public boolean socialSpyAddAll(Player sender) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (profile.getOptions().getSocialSpy().contains("@ALL")) {
            sender.sendMessage(CC.format("<red>You are already spying on <yellow>all players<red>."));
            return false;
        }
        profile.getOptions().getSocialSpy().add("@ALL");
        sender.sendMessage(CC.format("<yellow>Now spying on <red>all players<yellow>."));
        profile.save(() -> {}, true);
        return true;
    }

    @Command(names = {"socialspy remove"},
            permission = "altara.command.socialspy.add",
            playerOnly = true,
            async = true)
    public boolean socialSpyRemove(Player sender, @Param(name = "target") Profile target) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (!profile.getOptions().getSocialSpy().contains(target.getUuid().toString())) {
            sender.sendMessage(CC.format("<red>You are not spying on <yellow>" + target.getName() + "<red>."));
            return false;
        }
        profile.getOptions().getSocialSpy().remove(target.getUuid().toString());
        sender.sendMessage(CC.format("<yellow>No longer spying on <red>" + target.getName() + "<yellow>."));
        profile.save(() -> {}, true);
        return true;
    }

    @Command(names = {"socialspy removeall"},
            permission = "altara.command.socialspy.remove",
            playerOnly = true)
    public boolean socialSpyRemoveAll(Player sender) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (!profile.getOptions().getSocialSpy().contains("@ALL")) {
            sender.sendMessage(CC.format("<red>You are not spying on <yellow>all players<red>."));
            return false;
        }
        profile.getOptions().getSocialSpy().remove("@ALL");
        sender.sendMessage(CC.format("<yellow>No longer spying on <red>all players<yellow>."));
        profile.save(() -> {}, true);
        return true;
    }

    // ── /ignore ───────────────────────────────────────────────────────────────

    @Command(names = {"ignore list"}, permission = "player")
    public boolean ignoreList(Player sender,
                               @Param(name = "page", defaultValue = "1") int page) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (profile.getOptions().getIgnoring().isEmpty()) {
            sender.sendMessage(CC.format("<yellow>You are not ignoring anyone."));
            return false;
        }
        new PagedMessage<UUID>() {
            @Override public List<String> getHeader(int page, int maxPages) {
                return Arrays.asList(CC.SMALL_CHAT_BAR.toString(),
                        CC.RED + "Ignore List " + CC.GRAY + "(Page " + page + "/" + maxPages + ")");
            }
            @Override public List<String> getFooter(int page, int maxPages) {
                return Arrays.asList(" ",
                        CC.YELLOW + "Use " + CC.RED + "/ignore list <page> " + CC.YELLOW + "for more.",
                        CC.SMALL_CHAT_BAR.toString());
            }
            @Override public void send(CommandSender sender, UUID uuid) {
                sender.sendMessage(CC.WHITE + " - " + CC.YELLOW + UUIDCache.getName(uuid));
            }
        }.display(sender, profile.getOptions().getIgnoring(), page);
        return true;
    }

    @Command(names = {"ignore clear"}, permission = "player")
    public boolean ignoreClear(Player sender) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        int size = profile.getOptions().getIgnoring().size();
        profile.getOptions().getIgnoring().clear();
        sender.sendMessage(CC.format("<yellow>Cleared <red>" + size + " <yellow>ignored players."));
        profile.save(() -> {}, true);
        return true;
    }

    @Command(names = {"ignore add"}, permission = "player", async = true)
    public boolean ignoreAdd(Player sender, @Param(name = "player") Profile target) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (profile.getOptions().getIgnoring().contains(target.getUuid())) {
            sender.sendMessage(CC.format("<yellow>You are already ignoring <red>" + target.getCurrentName() + "<yellow>."));
            return false;
        }
        profile.getOptions().getIgnoring().add(target.getUuid());
        sender.sendMessage(CC.format("<yellow>Now ignoring <red>" + target.getCurrentName() + "<yellow>."));
        profile.save(() -> {}, true);
        return true;
    }

    @Command(names = {"ignore remove"}, permission = "player", async = true)
    public boolean ignoreRemove(Player sender, @Param(name = "player") Profile target) {
        Profile profile = AltaraPaper.getPaperInstance().getProfileService().getProfile(sender);
        if (!profile.getOptions().getIgnoring().contains(target.getUuid())) {
            sender.sendMessage(CC.format("<red>You are not ignoring <yellow>" + target.getCurrentName() + "<red>."));
            return false;
        }
        profile.getOptions().getIgnoring().remove(target.getUuid());
        sender.sendMessage(CC.format("<yellow>No longer ignoring <red>" + target.getCurrentName() + "<yellow>."));
        profile.save(() -> {}, true);
        return true;
    }

    // ── Core dispatch ─────────────────────────────────────────────────────────

    private void handleMessage(Profile sender, Profile target, String message) {
        Player senderPlayer = sender.player();
        Player targetPlayer = target.player();

        // ── Guards ─────────────────────────────────────────────────────────────
        boolean senderStaff = senderPlayer.hasPermission("altara.staff");

        if (!AltaraSettings.PRIVATE_MESSAGES.get(senderPlayer) && !senderStaff) {
            senderPlayer.sendMessage(CC.format("<red>You have private messages disabled."));
            return;
        }
        if (!AltaraSettings.PRIVATE_MESSAGES.get(targetPlayer) && !senderStaff) {
            senderPlayer.sendMessage(CC.format("<yellow>" + target.getCurrentName() +
                    " <red>has private messages disabled."));
            return;
        }
        if (sender.getOptions().getIgnoring().contains(target.getUuid()) && !senderStaff) {
            senderPlayer.sendMessage(CC.format("<red>You are ignoring <yellow>" + target.getCurrentName() + "<red>."));
            return;
        }
        if (target.getOptions().getIgnoring().contains(sender.getUuid()) && !senderStaff) {
            senderPlayer.sendMessage(CC.format("<yellow>" + target.getCurrentName() +
                    " <red>is ignoring you."));
            return;
        }

        // ── Collect social spies ───────────────────────────────────────────────
        List<Profile> spies = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("altara.socialspy")) continue;
            if (online.getUniqueId().equals(sender.getUuid())) continue;
            if (online.getUniqueId().equals(target.getUuid())) continue;

            Profile spyProfile = Altara.getSharedInstance().getProfileService().getProfile(online.getUniqueId());
            if (spyProfile == null) continue;

            List<String> spy = spyProfile.getOptions().getSocialSpy();
            if (spy.contains("@ALL")
                    || spy.contains(sender.getUuid().toString())
                    || spy.contains(target.getUuid().toString())) {
                spies.add(spyProfile);
            }
        }

        // ── Route through DirectMessageChannel ────────────────────────────────
        DirectMessageChannel.getInstance().dispatch(sender, target, message, spies);

        // ── Sound ──────────────────────────────────────────────────────────────
        if (AltaraSettings.MESSAGING_SOUNDS.get(targetPlayer))
            targetPlayer.playSound(targetPlayer.getLocation(),
                    Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.1f);

        // ── Update reply map ───────────────────────────────────────────────────
        lastConversation.put(sender.getUuid(), target.getUuid());
        lastConversation.put(target.getUuid(), sender.getUuid());

        // ── Warn sender if they have PMs off (staff override) ─────────────────
        if (!AltaraSettings.PRIVATE_MESSAGES.get(senderPlayer) && senderStaff) {
            senderPlayer.sendMessage(CC.format(
                    "<red>You have private messages disabled — they cannot reply."));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isUuid(String s) {
        try { UUID.fromString(s); return true; }
        catch (IllegalArgumentException e) { return false; }
    }
}




