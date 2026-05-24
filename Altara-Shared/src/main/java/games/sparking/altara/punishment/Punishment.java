package games.sparking.altara.punishment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an issued punishment record persisted in MongoDB and propagated via Redis.
 *
 * <p>Duration semantics (per {@link RestrictionAction#getDuration()}):
 * <ul>
 *   <li>{@code -1} → permanent</li>
 *   <li>{@code 0}  → immediate / non-persistent (e.g. WARN)</li>
 *   <li>{@code > 0} → millisecond period from {@link #issuedAt}</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class Punishment {

    /** UUID used as the executor UUID for console / automated actions. */
    public static final String CONSOLE_UUID = "63644fed-6a20-4c35-bef4-be5e1d785a2e";

    // ── Identity ───────────────────────────────────────────────────────────────
    private String id;
    private String playerUuid;
    private String staffUuid;

    // ── Violation ──────────────────────────────────────────────────────────────
    /** Name of the {@link InfractionType} enum constant. */
    private String infractionType;
    private List<RestrictionAction> actions = new ArrayList<>();
    /** The chat message that triggered the infraction (nullable). */
    private String message;
    private String notes;

    // ── Timestamps ─────────────────────────────────────────────────────────────
    private long issuedAt;
    private boolean removed;
    private long removedAt = -1L;
    private String removedBy;

    // ── Constructors ───────────────────────────────────────────────────────────

    public Punishment(UUID playerUuid, UUID staffUuid, InfractionType infractionType,
                      List<RestrictionAction> actions, String message) {
        this.id             = UUID.randomUUID().toString();
        this.playerUuid     = playerUuid.toString();
        this.staffUuid      = (staffUuid != null) ? staffUuid.toString() : CONSOLE_UUID;
        this.infractionType = infractionType.name();
        this.actions        = new ArrayList<>(actions);
        this.message        = message;
        this.issuedAt       = System.currentTimeMillis();
        this.removed        = false;
        this.removedAt      = -1L;
    }

    // ── Computed state ─────────────────────────────────────────────────────────

    /**
     * A punishment is "active" when it hasn't been manually removed AND at least
     * one of its actions has not yet expired.
     */
    public boolean isActive() {
        if (removed) return false;
        return actions.stream().anyMatch(a -> !a.hasExpired(issuedAt));
    }

    /** Returns {@code true} if this punishment contains a SUSPENSION action. */
    public boolean hasSuspension() {
        return actions.stream().anyMatch(a -> a.getType() == PunishmentType.SUSPENSION);
    }

    /** Returns {@code true} if the player is currently banned (active SUSPENSION). */
    public boolean isBan() {
        if (!hasSuspension()) return false;
        return actions.stream()
                .filter(a -> a.getType() == PunishmentType.SUSPENSION)
                .anyMatch(a -> !a.hasExpired(issuedAt));
    }

    /** Returns the SUSPENSION action, or {@code null} if none is present. */
    public RestrictionAction getSuspensionAction() {
        return actions.stream()
                .filter(a -> a.getType() == PunishmentType.SUSPENSION)
                .findFirst()
                .orElse(null);
    }

    public InfractionType getInfractionTypeEnum() {
        try {
            return InfractionType.valueOf(infractionType);
        } catch (Exception e) {
            return null;
        }
    }

    // ── JSON serialisation ─────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("id",             id);
        obj.addProperty("playerUuid",     playerUuid);
        obj.addProperty("staffUuid",      staffUuid);
        obj.addProperty("infractionType", infractionType);
        obj.addProperty("message",        message);
        obj.addProperty("notes",          notes);
        obj.addProperty("issuedAt",       issuedAt);
        obj.addProperty("removed",        removed);
        obj.addProperty("removedAt",      removedAt);
        obj.addProperty("removedBy",      removedBy);

        JsonArray actionsArr = new JsonArray();
        for (RestrictionAction action : actions) {
            JsonObject a = new JsonObject();
            a.addProperty("type",     action.getType().name());
            a.addProperty("duration", action.getDuration());
            actionsArr.add(a);
        }
        obj.add("actions", actionsArr);
        return obj;
    }

    public static Punishment fromJson(JsonObject obj) {
        Punishment p   = new Punishment();
        p.id            = jsonString(obj, "id");
        p.playerUuid    = jsonString(obj, "playerUuid");
        p.staffUuid     = jsonString(obj, "staffUuid");
        p.infractionType = jsonString(obj, "infractionType");
        p.message       = jsonString(obj, "message");
        p.notes         = jsonString(obj, "notes");
        p.issuedAt      = obj.has("issuedAt") ? obj.get("issuedAt").getAsLong() : 0L;
        p.removed       = obj.has("removed")  && obj.get("removed").getAsBoolean();
        p.removedAt     = (obj.has("removedAt") && !obj.get("removedAt").isJsonNull())
                ? obj.get("removedAt").getAsLong() : -1L;
        p.removedBy     = jsonString(obj, "removedBy");

        p.actions = new ArrayList<>();
        if (obj.has("actions") && obj.get("actions").isJsonArray()) {
            for (JsonElement el : obj.get("actions").getAsJsonArray()) {
                JsonObject a = el.getAsJsonObject();
                PunishmentType type     = PunishmentType.valueOf(a.get("type").getAsString());
                long           duration = a.get("duration").getAsLong();
                p.actions.add(new RestrictionAction(type, duration));
            }
        }
        return p;
    }

    private static String jsonString(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : null;
    }

    // ── Legacy compat ──────────────────────────────────────────────────────────

    @AllArgsConstructor
    @Getter
    public enum LegacyType {
        WARN("Warn", "Warned", ""),
        KICK("Kick", "Kicked", ""),
        MUTE("Mute", "Muted", "Unmuted"),
        BAN("Ban", "Banned", "Unbanned"),
        BLACKLIST("Blacklist", "Blacklisted", "Unblacklisted");

        private final String name;
        private final String context;
        private final String removeContext;

        public boolean isOverwritable() {
            return this == BAN || this == MUTE || this == BLACKLIST;
        }
    }
}
