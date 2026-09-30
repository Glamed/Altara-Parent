package games.sparking.altara.redis.subscriber;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Method;

@RequiredArgsConstructor
public class ListenerPubSub extends JedisPubSub {

    private final RedisListener listener;
    private final Method method;

    @Override
    public void onMessage(String channel, String message) {
        try {
            Object payload = message;
            if (JsonElement.class.isAssignableFrom(method.getParameterTypes()[method.getParameterCount() - 1])) {
                JsonElement json = JsonParser.parseString(message);
                payload = json;
            }

            if (method.getParameterCount() == 1)
                method.invoke(listener, payload);
            else method.invoke(listener, channel, payload);
        } catch (Exception e) {
            // Never let a listener failure end the subscription.
            e.printStackTrace();
        }
    }
}
