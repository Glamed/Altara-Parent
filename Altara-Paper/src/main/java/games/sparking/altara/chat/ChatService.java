package games.sparking.altara.chat;


import games.sparking.altara.Altara;
import games.sparking.altara.chat.impl.PublicChat;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiFunction;

public class ChatService {

    private static final Map<UUID, ChatChannel> playerChatChannels = new HashMap<>();
    private static final Map<String, ChatChannel> chatChannels = new HashMap<>();
    @Getter
    @Setter
    private static BiFunction<Player, CommandSender, String> prefixGetter = (player, sender) -> player.getDisplayName();
    @Getter
    @Setter
    private static DefaultChannelProvider defaultChannelProvider = DefaultChannelProvider.DEFAULT;
    @Getter
    @Setter
    private static ChatChannel defaultChannel = new PublicChat();

    public static void registerChatChannel(ChatChannel channel) {
        chatChannels.put(channel.getName().toLowerCase(), channel);
    }

    public static ChatChannel fromPlayer(Player player) {
        ChatChannel chatChannel = playerChatChannels.get(player.getUniqueId());
        if (chatChannel == null) {
            chatChannel = defaultChannelProvider.getDefaultChannel(player);
            setChatChannel(player, chatChannel, true);
        }

        return chatChannel;
    }

    /** Looks up a channel by name or alias. Name matching is case-insensitive. */
    public static ChatChannel fromName(String name) {
        String lower = name.toLowerCase();
        ChatChannel channel = chatChannels.get(lower);
        if (channel != null) return channel;

        for (ChatChannel chatChannel : chatChannels.values()) {
            if (chatChannel.getAliases().contains(lower))
                return chatChannel;
        }
        return null;
    }

    public static ChatChannel fromPrefix(char prefix) {
        for (ChatChannel chatChannel : chatChannels.values()) {
            if (chatChannel.getPrefix() == prefix)
                return chatChannel;
        }

        return null;
    }

    public static void setChatChannel(Player player, ChatChannel channel, boolean silent) {
        if (channel.getPriority() >= 0)
            Altara.getRedisService().executeCommand(redis ->
                    redis.hset("ChatChannel" + (channel instanceof LocalChatChannel
                                    ? ":" + Altara.getSharedInstance().getMainConfig().getServerConfig().getLocalServerName() : ""),
                            player.getUniqueId().toString(), channel.getName().toLowerCase()));

        if (!(channel instanceof LocalChatChannel) && channel.getPriority() >= 0)
            Altara.getRedisService().executeCommand(redis ->
                    redis.hdel("ChatChannel:" + Altara.getSharedInstance().getMainConfig().getServerConfig().getLocalServerName(),
                            player.getUniqueId().toString()));

        if (channel.getPriority() >= 0)
            loadChatChannel(player.getUniqueId());
        else playerChatChannels.put(player.getUniqueId(), channel);

        if (!silent)
            player.sendMessage(CC.YELLOW + "You are now talking in "
                    + channel.getDisplayName().toLowerCase() + CC.YELLOW + " chat.");
    }

    public static void loadChatChannel(UUID uuid) {
        playerChatChannels.put(uuid,
                Altara.getRedisService().executeCommand(redis -> {
                    ChatChannel globalChannel = null;
                    ChatChannel localChannel = null;

                    if (redis.hexists("ChatChannel", uuid.toString()))
                        globalChannel = fromName(redis.hget("ChatChannel", uuid.toString()));

                    if (redis.hexists("ChatChannel:" + Altara.getSharedInstance().getMainConfig().getServerConfig().getLocalServerName(),
                            uuid.toString()))
                        localChannel = fromName(redis.hget("ChatChannel:" + Altara.getSharedInstance().getMainConfig().getServerConfig().getLocalServerName(),
                                uuid.toString()));

                    // Prefer the local channel if it has equal or higher priority.
                    if (globalChannel == null || (localChannel != null
                            && localChannel.getPriority() >= globalChannel.getPriority()))
                        globalChannel = localChannel;

                    return globalChannel;
                }));
    }

    public static void removePlayer(Player player) {
        playerChatChannels.remove(player.getUniqueId());
    }

    /** Returns all registered channels sorted by priority (highest first). */
    public static List<ChatChannel> getChannels() {
        List<ChatChannel> sorted = new ArrayList<>(chatChannels.values());
        sorted.sort(Comparator.comparingInt(ChatChannel::getPriority).reversed());
        return sorted;
    }


}