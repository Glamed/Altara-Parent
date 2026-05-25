package games.sparking.altara.chat.impl;

import games.sparking.altara.chat.ChatChannel;
import games.sparking.altara.chat.ChatService;
import games.sparking.altara.profiler.ProfilerService;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

/**
 * A special internal chat channel automatically assigned to accounts that have
 * been flagged as potentially compromised by the Profiler system.
 *
 * <p>Behaviour per recipient:
 * <ul>
 *   <li><b>The sender themselves</b> — receives a normal-looking public chat format so
 *       they believe their message went through successfully.</li>
 *   <li><b>Staff</b> (players with {@code altara.profiler}) — see a dimmed copy prefixed
 *       with {@code [SHADOW]} so they can monitor the account's activity.</li>
 *   <li><b>Everyone else</b> — {@code getFormat} returns {@code null}, so the base
 *       {@link ChatChannel#chat} loop skips them entirely.</li>
 *   <li><b>Console</b> — also receives the {@code [SHADOW]} copy for logging.</li>
 * </ul>
 *
 * <p>The channel is stored with {@code priority = -1} which prevents
 * {@link ChatService#setChatChannel} from persisting it to Redis — it lives only
 * in the local {@code playerChatChannels} map.  Calling
 * {@link ChatService#loadChatChannel} for the player at any point will evict this
 * channel and restore whatever was previously saved in Redis.
 */
public class ShadowMuteChannel extends ChatChannel {

    @Getter
    private static final ShadowMuteChannel instance = new ShadowMuteChannel();

    private ShadowMuteChannel() {
        super(
                "shadowmute",
                CC.DGRAY + "Shadow Mute",
                null,                       // no access permission — internal only
                Collections.emptyList(),    // no aliases
                (char) 0,                   // no prefix trigger
                -1                          // negative priority → not persisted to Redis
        );
    }

    /**
     * Returns the format string for a given recipient, or {@code null} to suppress delivery.
     *
     * <p>The format uses {@code String.format} placeholders:
     * {@code %1$s} = the sender's display prefix, {@code %2$s} = the raw message.
     */
    @Override
    public String getFormat(Player sender, CommandSender recipient) {
        // The sender sees their own message formatted like normal public chat.
        if (recipient instanceof Player recipientPlayer && recipientPlayer.equals(sender)) {
            return "%1$s" + CC.format(" &8&l» &f") + "%2$s";
        }

        // Staff and console see a dimmed shadow copy.
        if (recipient.hasPermission(ProfilerService.PERMISSION)) {
            return CC.format("&8[&7SHADOW&8] ") + "%1$s" + CC.format(" &8&l» &7") + "%2$s";
        }

        // Everyone else: suppress — produce no output.
        return null;
    }

    @Override
    public boolean onChat(Player player, String message) {
        // Always allow the message to continue to the format/delivery phase.
        return true;
    }
}

