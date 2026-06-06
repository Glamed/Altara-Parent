package games.sparking.altara.chat;

import games.sparking.altara.Altara;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.chat.packet.ChatMessagePacket;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
 *   <li>{@code log}    — when {@code true} every message is printed to the
 *       server console along with the list of online recipients.</li>
 *   <li>{@code global} — when {@code true} a {@link ChatMessagePacket} is
 *       published over Redis so other servers on the network receive the
 *       formatted message.  Remote delivery uses
 *       {@link ChannelAudience#canSeeRemote}.</li>
 *   <li>{@code persistable} — when {@code false} (e.g. {@code ShadowMuteChannel})
 *       switching into this channel will <em>not</em> overwrite the player's
 *       saved channel preference.</li>
 * </ul>
 */
@Getter
public abstract class ChatChannel {

    private final String name;

    /**
     * Single-character prefix a player can type before their message to send it
     * through this channel without switching.  {@code null} means no prefix
     * (players must use {@code /channel} to switch).
     */
    private final String prefix;

    /** If {@code true}, messages are echoed to the server console with recipients. */
    private final boolean log;

    /** If {@code true}, a cross-server Redis packet is published after local delivery. */
    private final boolean global;

    /**
     * If {@code false} the player's saved channel preference is not overwritten
     * when they are moved into this channel (used for shadow-mute enforcement).
     */
    private final boolean persistable;

    protected ChatChannel(String name, String prefix, boolean log, boolean global, boolean persistable) {
        this.name       = name;
        this.prefix     = prefix;
        this.log        = log;
        this.global     = global;
        this.persistable = persistable;
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

    // ── Dispatch ───────────────────────────────────────────────────────────────

    /**
     * Formats and delivers {@code rawMessage} from {@code sender} through this
     * channel, then publishes a cross-server packet if {@link #isGlobal()}.
     *
     * @param sender     the player sending the message
     * @param rawMessage message text (channel prefix already removed)
     */
    public void dispatch(Player sender, String rawMessage) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender.getUniqueId());
        Component formatted = format(profile != null ? profile : fallbackProfile(sender), rawMessage);

        List<String> recipientNames = new ArrayList<>();

        // ── Local delivery ─────────────────────────────────────────────────────
        Bukkit.getConsoleSender().sendMessage(formatted);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (getAudience().canSee(viewer, sender, this)) {
                viewer.sendMessage(formatted);
                if (log) recipientNames.add(viewer.getName());
            }
        }

        // ── Logging ────────────────────────────────────────────────────────────
        if (log) {
            Altara.getSharedInstance().getLogger().info(
                    "[" + name + "] " + sender.getName() + " -> [" +
                    String.join(", ", recipientNames) + "]: " + rawMessage);
        }

        // ── Cross-server relay ─────────────────────────────────────────────────
        if (global) {
            String origin = Altara.getSharedInstance().getLocalServerName();
            new ChatMessagePacket(formatted, name, sender.getUniqueId(), origin).publish();
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Converts a legacy {@code &}-color-coded rank string (e.g. {@code "&c[ADMIN] "})
     * into a {@link Component} using Adventure's legacy serialiser.
     */
    protected static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /** Minimal profile stand-in used when the real profile isn't loaded yet. */
    private static Profile fallbackProfile(Player player) {
        return new Profile(player.getUniqueId(), player.getName());
    }
}



