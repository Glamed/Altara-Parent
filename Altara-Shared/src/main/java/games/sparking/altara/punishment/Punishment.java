package games.sparking.altara.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A single punishment record that may bundle multiple {@link RestrictionAction}s,
 * each with its own independent duration.
 *
 * <p>Example: a 30-day suspension + 90-day competitive gameplay restriction issued at once.
 *
 * <p>Durations inside each {@link RestrictionAction} are <b>relative offsets</b> from
 * {@link #issuedAt} (in milliseconds). A value of {@code -1} means permanent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Punishment {

    private String id;

    /** UUID of the punished player, stored as a String for JSON compatibility. */
    private String playerUuid;

    /** UUID of the issuing staff member, stored as a String. May be null for console. */
    private String staffUuid;

    /** {@link InfractionType} name (enum constant name), e.g. {@code "PROFANITY"}. */
    private String infractionType;

    /** One or more restrictions, each with its own type and independent duration. */
    private List<RestrictionAction> actions = new ArrayList<>();

    /** Optional chat message that triggered the infraction. */
    private String message;

    /** Optional internal staff notes, never shown to the player. */
    private String notes;

    /** Unix epoch millis when this punishment was issued. */
    private long issuedAt;

    /** Whether this punishment has been soft-deleted (revoked). */
    private boolean removed;

    /** Unix epoch millis when it was revoked, or {@code -1} if not revoked. */
    private long removedAt = -1L;

    /** UUID string of the staff member who revoked this, or null. */
    private String removedBy;

    // ── Constructors ───────────────────────────────────────────────────────────

    /**
     * Convenience constructor used by {@code PunishmentService} when building a new
     * punishment to POST to the Web API.
     */
    public Punishment(UUID playerUuid,
                      UUID staffUuid,
                      InfractionType infractionType,
                      List<RestrictionAction> actions,
                      String message) {
        this.id             = UUID.randomUUID().toString();
        this.playerUuid     = playerUuid.toString();
        this.staffUuid      = staffUuid != null ? staffUuid.toString() : null;
        this.infractionType = infractionType.name();
        this.actions        = new ArrayList<>(actions);
        this.message        = message;
        this.issuedAt       = System.currentTimeMillis();
        this.removed        = false;
        this.removedAt      = -1L;
    }

    // ── Activity checks ────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this punishment is currently active — i.e. not removed
     * and at least one restriction has not yet expired.
     */
    public boolean isActive() {
        if (removed || actions == null || actions.isEmpty()) return false;
        for (RestrictionAction a : actions) {
            if (!a.hasExpired(issuedAt)) return true;
        }
        return false;
    }

    /** Alias kept for backward compatibility. Delegates to {@link #isActive()}. */
    public boolean isCurrentlyActive() {
        return isActive();
    }

    /** Returns {@code true} if this punishment has an active SUSPENSION action. */
    public boolean isBan() {
        return hasActiveRestriction(PunishmentType.SUSPENSION);
    }

    /** Returns {@code true} if any non-expired restriction of the given type exists. */
    public boolean hasActiveRestriction(PunishmentType type) {
        if (removed || actions == null) return false;
        for (RestrictionAction a : actions) {
            if (a.getType() == type && !a.hasExpired(issuedAt)) return true;
        }
        return false;
    }

    /**
     * Returns the first active (non-expired) {@link RestrictionAction} of the given type,
     * or {@code null} if none.
     */
    public RestrictionAction getActiveRestriction(PunishmentType type) {
        if (removed || actions == null) return null;
        for (RestrictionAction a : actions) {
            if (a.getType() == type && !a.hasExpired(issuedAt)) return a;
        }
        return null;
    }

    /**
     * Remaining millis for the given restriction type.
     * Returns {@code -1} if the restriction is permanent, {@code 0} if not active.
     */
    public long getRemainingDuration(PunishmentType type) {
        RestrictionAction action = getActiveRestriction(type);
        if (action == null) return 0L;
        if (action.getDuration() == -1L) return -1L;
        return Math.max(0L, (issuedAt + action.getDuration()) - System.currentTimeMillis());
    }

    // ── Infraction type helper ─────────────────────────────────────────────────

    /**
     * Parses {@link #infractionType} back to the enum.
     * Returns {@code null} if the field is blank or unrecognised.
     */
    public InfractionType getReason() {
        if (infractionType == null || infractionType.isBlank()) return null;
        try {
            return InfractionType.valueOf(infractionType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ── Backward-compat helpers ────────────────────────────────────────────────

    /**
     * Returns the type of the <em>first</em> action, or {@code null}.
     * Useful for menus that still display a primary type badge.
     */
    public PunishmentType getPrimaryType() {
        return (actions != null && !actions.isEmpty()) ? actions.get(0).getType() : null;
    }

    /**
     * Absolute expiry timestamp (millis) of the <em>first</em> action.
     * Returns {@code -1} if permanent or no actions exist.
     */
    public long getPrimaryExpiresAt() {
        if (actions == null || actions.isEmpty()) return -1L;
        RestrictionAction first = actions.get(0);
        return first.getDuration() == -1L ? -1L : issuedAt + first.getDuration();
    }

    /** @deprecated Use {@link #getPrimaryType()} */
    @Deprecated public PunishmentType getType()      { return getPrimaryType(); }

    /** @deprecated Use {@link #getPrimaryExpiresAt()} */
    @Deprecated public long           getExpiresAt() { return getPrimaryExpiresAt(); }

    /** Parses {@link #staffUuid} to a {@link UUID}, or {@code null}. */
    public UUID getStaffUUID() {
        return staffUuid != null ? UUID.fromString(staffUuid) : null;
    }

    // ── JSON ───────────────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id",             id);
        obj.addProperty("playerUuid",     playerUuid);
        obj.addProperty("staffUuid",      staffUuid);
        obj.addProperty("infractionType", infractionType);

        JsonArray actArr = new JsonArray();
        if (actions != null) {
            for (RestrictionAction a : actions) {
                JsonObject ao = new JsonObject();
                ao.addProperty("type",     a.getType().name());
                ao.addProperty("duration", a.getDuration());
                actArr.add(ao);
            }
        }
        obj.add("actions", actArr);

        obj.addProperty("message",   message);
        obj.addProperty("notes",     notes);
        obj.addProperty("issuedAt",  issuedAt);
        obj.addProperty("removed",   removed);
        obj.addProperty("removedAt", removedAt);
        obj.addProperty("removedBy", removedBy);
        return obj;
    }

    public static Punishment fromJson(JsonObject obj) {
        Punishment p = new Punishment();
        p.id             = str(obj, "id");
        p.playerUuid     = str(obj, "playerUuid");
        p.staffUuid      = str(obj, "staffUuid");
        p.infractionType = str(obj, "infractionType");

        p.actions = new ArrayList<>();
        if (obj.has("actions") && obj.get("actions").isJsonArray()) {
            for (JsonElement el : obj.get("actions").getAsJsonArray()) {
                JsonObject ao       = el.getAsJsonObject();
                PunishmentType type = PunishmentType.valueOf(ao.get("type").getAsString());
                long duration       = ao.get("duration").getAsLong();
                p.actions.add(new RestrictionAction(type, duration));
            }
        }

        p.message      = str(obj, "message");
        p.notes        = str(obj, "notes");
        p.issuedAt     = longVal(obj, "issuedAt",  0L);
        p.removed      = boolVal(obj, "removed",   false);
        p.removedAt    = longVal(obj, "removedAt", -1L);
        p.removedBy    = str(obj, "removedBy");
        return p;
    }

    // ── JSON helpers ───────────────────────────────────────────────────────────

    private static String str(JsonObject o, String key) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsString() : null;
    }

    private static long longVal(JsonObject o, String key, long def) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsLong() : def;
    }

    private static boolean boolVal(JsonObject o, String key, boolean def) {
        return (o.has(key) && !o.get(key).isJsonNull()) ? o.get(key).getAsBoolean() : def;
    }
}