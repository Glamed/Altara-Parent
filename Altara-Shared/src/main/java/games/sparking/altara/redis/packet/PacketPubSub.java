package games.sparking.altara.redis.packet;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import games.sparking.altara.utils.Statics;
import redis.clients.jedis.JedisPubSub;

public class PacketPubSub extends JedisPubSub {

    @Override
    public void onMessage(String channel, String redisMessage) {
        JsonObject redisJson = JsonParser.parseString(redisMessage).getAsJsonObject();
        String packetClassName = redisJson.get("packet").getAsString();
        String packetJson = redisJson.get("data").getAsString();
        Class<?> packetClass;
        try {
            packetClass = Class.forName(packetClassName);
        } catch (ClassNotFoundException e) {
            return;
        }
        Packet packet = (Packet) Statics.PLAIN_GSON.fromJson(packetJson, packetClass);
        try {
            packet.receive();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}