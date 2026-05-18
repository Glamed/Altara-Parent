package games.sparking.altara.redis.packet;

import games.sparking.altara.Altara;

public abstract class Packet {

    public void receive() {
    }

    public String getClazz() {
        return this.getClass().getName();
    }

    public void publish() {
        Altara.getRedis().publish(this);
    }
}