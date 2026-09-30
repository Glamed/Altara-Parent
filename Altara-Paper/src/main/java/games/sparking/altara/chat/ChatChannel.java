package games.sparking.altara.chat;

import games.sparking.altara.Altara;
import games.sparking.altara.chat.log.ChatLogEntry;
import games.sparking.altara.chat.packet.ChatMessagePacket;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for every chat channel in Altara.
 *
 * <p>Subclass this (or {@link FilteredChatChannel} for permission-gated channels)
 * and register the instance via {@link ChatChannelRegistry#register(ChatChannel)} to add a
 * new channel.  The only things you <em>must</em> implement are
 * {@link #format(Profile, String)} and {@link #getAudience()}.
 *
 * <h3>Options</h3>
 * <ul>
 *   <li>{@code log}    — every message is printed to the console, its recipients are
 *       recorded, and the delivery record is persisted to Redis for
 *       {@value ChatLogEntry#TTL_SECONDS} seconds.</li>
 *   <li>{@code global} — a {@link ChatMessagePacket} relays the formatted message to
 *       every other server, where {@link ChannelAudience#canSeeRemote} decides delivery.</li>
 *   <li>{@code selectable} — players may switch into this channel with {@code /channel}
 *       and it is saved as their preference.  System channels (shadow mute) are not.</li>
 * </ul>
 *
 * <p><b>Security:</b> chat text is player input.  Always insert it with
 * {@link #messageComponent} (or another unparsed placeholder) — never concatenate it
 * into a MiniMessage template.
 */
@Getter
public abstract class ChatChannel {

    private final String name;

    /**
     * Prefix a player can type before their message to send it through this channel
     * without switching, or {@code null} for none.
     */
    private final String prefix;

    private final boolean log;
    private final boolean global;
    private final boolean selectable;

    protected ChatChannel(String name, String prefix, boolean log, boolean global, boolean selectable) {
        this.name       = name;
        this.prefix     = prefix;
        this.log        = log;
        this.global     = global;
        this.selectable = selectable;
    }

    // ── Abstract contract ──────────────────────────────────────────────────────

    /**
     * Builds the final {@link Component} that gets sent to each recipient.
     *
     * @param sender  the sender's profile (for rank prefix / colour)
     * @param message the raw chat message (already stripped of any channel prefix)
     */
    public abstract Component format(Profile sender, String message);

    /** Returns the audience rules for this channel. */
    public abstract ChannelAudience getAudience();

    /** Whether {@code player} may send messages in (and switch to) this channel. */
    public boolean canUse(Player player) {
        return true;
    }

    // ── Dispatch ───────────────────────────────────────────────────────────────

    /**
     * Formats and delivers {@code rawMessage} from {@code sender} through this
     * channel, then publishes a cross-server packet if {@link #isGlobal()}.
     */
    public void dispatch(Player sender, String rawMessage) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender.getUniqueId());
        Component formatted = format(profile != null ? profile : fallbackProfile(sender), rawMessage);

        String messageId = UUID.randomUUID().toString();
        String origin = Altara.getSharedInstance().getLocalServerName();
        List<String> recipientNames = new ArrayList<>();

        Bukkit.getConsoleSender().sendMessage(formatted);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (getAudience().canSee(viewer, sender, this)) {
                viewer.sendMessage(formatted);
                recipientNames.add(viewer.getName());
            }
        }

        if (log) {
            Altara.getSharedInstance().getLogger().info(
                    "[" + name + "] " + sender.getName() + " -> [" + String.join(", ", recipientNames) + "]: " + rawMessage);

            new ChatLogEntry(messageId, name, sender.getUniqueId().toString(), sender.getName(),
                    MiniMessage.miniMessage().serialize(formatted), rawMessage, recipientNames, origin).save();
        }

        if (global) {
            new ChatMessagePacket(formatted, name, sender.getUniqueId(), origin, messageId, log).publish();
        }
    }

    // ── Formatting helpers ─────────────────────────────────────────────────────

    /** The sender's rank prefix followed by their name in rank colour. */
    protected static Component senderName(Profile sender) {
        Rank rank = sender.getCurrentGrant().asRank();
        return CC.format(rank.getPrefix() + rank.getColor() + "<name>",
                Placeholder.unparsed("name", sender.getCurrentName()));
    }

    /** The raw message in the sender's rank chat colour, never parsed as MiniMessage. */
    protected static Component messageComponent(Profile sender, String message) {
        return CC.format(sender.getCurrentGrant().asRank().getChatColor() + "<message>",
                Placeholder.unparsed("message", message));
    }

    /** Minimal profile stand-in used when the real profile isn't loaded yet. */
    protected static Profile fallbackProfile(Player player) {
        return new Profile(player.getUniqueId(), player.getName());
    }
}
