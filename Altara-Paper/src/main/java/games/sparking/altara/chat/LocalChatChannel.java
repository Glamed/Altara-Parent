package games.sparking.altara.chat;

/**
 * Marker interface for chat channels that are scoped to the local server only.
 * {@link ChatService} uses {@code instanceof LocalChatChannel} to determine which
 * Redis key ({@code ChatChannel:<serverName>} vs. the global {@code ChatChannel})
 * to read from and write to when persisting a player's active channel.
 */
public interface LocalChatChannel {
}