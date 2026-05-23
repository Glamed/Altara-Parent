package games.sparking.altara.punishment;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class Punishment {

    public boolean isActive() {
        return false;
    }

    public boolean isRemoved() {
        return false;
    }

    public Object getPunishmentType() {
        return null;
    }

    @AllArgsConstructor
    @Getter
    public enum PunishmentType {
        WARN("Warn", "Warned", ""),
        KICK("Kick", "Kicked", ""),
        MUTE("Mute", "Muted", "Unmuted"),
        BAN("Ban", "Banned", "Unbanned"),
        BLACKLIST("Blacklist", "Blacklisted", "Unblacklisted");

        private final String name;
        private final String context;
        private final String removeContext;

        public boolean isOverwritable() {
            return this == BAN
                    || this == MUTE
                    || this == BLACKLIST;
        }
    }
}
