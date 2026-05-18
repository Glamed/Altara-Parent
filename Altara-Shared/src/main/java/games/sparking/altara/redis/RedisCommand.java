package games.sparking.altara.redis;

import redis.clients.jedis.UnifiedJedis;

public interface RedisCommand<T> {

    T execute(UnifiedJedis redis);

}