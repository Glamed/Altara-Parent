package games.sparking.altara.constant;

import com.google.common.collect.Sets;
import games.sparking.altara.Altara;

import java.util.Set;

public abstract class Constant<T> {

    public static final Constant<Integer> STAFF_WEIGHT = new Constant<Integer>("staff_weight", 0) {
        @Override
        public Integer parse(String input) {
            Integer parsed = null;
            try {
                parsed = Integer.parseInt(input);
            } catch (NumberFormatException ignored) { }
            return parsed;
        }
    };

    public static final Constant<Integer> ADMIN_WEIGHT = new Constant<Integer>("admin_weight", 0) {
        @Override
        public Integer parse(String input) {
            Integer parsed = null;
            try {
                parsed = Integer.parseInt(input);
            } catch (NumberFormatException ignored) { }
            return parsed;
        }
    };

    public static final Constant<Integer> OWNER_WEIGHT = new Constant<Integer>("owner_weight", 0) {
        @Override
        public Integer parse(String input) {
            Integer parsed = null;
            try {
                parsed = Integer.parseInt(input);
            } catch (NumberFormatException ignored) { }
            return parsed;
        }
    };

    public static final Set<Constant> CONSTANTS = Sets.newHashSet(
            STAFF_WEIGHT,
            ADMIN_WEIGHT,
            OWNER_WEIGHT
    );

    private final String key;
    private T value;

    public Constant(String key, T initialValue) {
        this.key = key;
        this.value = initialValue;
    }

    public void loadValue() {
        String redisValue = Altara.getRedisService().executeCommand(redis -> {
            if (!redis.hexists("altara:constant", key))
                return null;

            return redis.hget("altara:constant", key);
        });

        if (redisValue == null)
            saveValue();
        else value = parse(redisValue);
    }

    public void saveValue() {
        Altara.getRedisService().executeCommand(redis ->
                redis.hset("altara:constant", key, value.toString()));
    }

    public abstract T parse(String input);

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }


}
