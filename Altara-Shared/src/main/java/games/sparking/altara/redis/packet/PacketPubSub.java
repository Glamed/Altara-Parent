package games.sparking.altara.redis.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.utils.Statics;
import redis.clients.jedis.JedisPubSub;

/**
 * Deserialises and dispatches {@link Packet}s.  Any failure is contained to the one
 * message — an exception escaping {@code onMessage} would end the subscription.
 */
public class PacketPubSub extends JedisPubSub {

    @Override
    public void onMessage(String channel, String redisMessage) {
        try {
            JsonObject redisJson = JsonParser.parseString(redisMessage).getAsJsonObject();
            String packetClassName = redisJson.get("packet").getAsString();
            String packetJson = redisJson.get("data").getAsString();

            Class<?> packetClass;
            try {
                packetClass = Class.forName(packetClassName);
            } catch (ClassNotFoundException e) {
                return; // packet belongs to a module not loaded on this server
            }

            // Only ever instantiate real packets from network input.
            if (!Packet.class.isAssignableFrom(packetClass)) {
                System.out.println("[Redis] Ignoring non-packet class " + packetClassName);
                return;
            }

            Packet packet = (Packet) Statics.PLAIN_GSON.fromJson(packetJson, packetClass);
            packet.receive();
        } catch (Exception e) {
            System.out.println("[Redis] Failed to handle packet on '" + channel + "': " + e);
            e.printStackTrace();
        }
    }
}
