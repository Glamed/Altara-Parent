package games.sparking.altara.chat;

import games.sparking.altara.chat.packet.ChatMessagePacket;
import org.bukkit.entity.Player;

/**
 * Determines which players may receive messages from a {@link ChatChannel}.
 *
 * <p>There are two delivery contexts:
 * <ul>
 *   <li><b>Local</b> ({@link #canSee}) — same server as the sender.</li>
 *   <li><b>Remote</b> ({@link #canSeeRemote}) — a different server that received
 *       a {@link ChatMessagePacket} over
 *       Redis.  The sender's {@link Player} object is not available here.</li>
 * </ul>
 */
public interface ChannelAudience {

    /**
     * Returns {@code true} if {@code viewer} should receive a message that was
     * sent on {@code channel} on the <em>same</em> server.
     *
     * @param viewer  the candidate recipient
     * @param sender  the player who sent the message
     * @param channel the channel the message was sent in
     */
    boolean canSee(Player viewer, Player sender, ChatChannel channel);

    /**
     * Returns {@code true} if {@code viewer} should receive a message that was
     * relayed from a <em>different</em> server via Redis.
     *
     * @param viewer  the candidate recipient
     * @param channel the channel the message was sent in
     */
    boolean canSeeRemote(Player viewer, ChatChannel channel);
}

