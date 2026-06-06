package games.sparking.altara.punishment;

public class RestrictionAction {

    private final PunishmentType type;
    private final long duration;

    public RestrictionAction(PunishmentType type, long duration) {
        this.type = type;
        this.duration = duration;
    }

    public static RestrictionAction permanent(PunishmentType type) {
        return new RestrictionAction(type, -1);
    }

    public static RestrictionAction temporary(PunishmentType type, long duration) {
        return new RestrictionAction(type, duration);
    }

    public PunishmentType getType() {
        return type;
    }

    public long getDuration() {
        return duration;
    }
}