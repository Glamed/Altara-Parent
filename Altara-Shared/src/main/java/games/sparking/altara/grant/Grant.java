package games.sparking.altara.grant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.configuration.JsonConfigurationService;
import games.sparking.altara.rank.Rank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor(force = true)
@Data
public class Grant {

    public static final Comparator<Grant> COMPARATOR = Comparator.comparingInt(grant -> grant.asRank().getWeight());

    private UUID id;
    private UUID uuid;
    private UUID rank;

    private String grantedBy = "Unknown";
    private long grantedAt = System.currentTimeMillis();
    private String grantedReason = "Unknown";

    private String removedBy = "Unknown";
    private long removedAt = -1;
    private String removedReason = "Unknown";

    private List<String> scopes;
    private long duration = -1;
    private long end = -1;
    private boolean removed = false;

    public Grant(UUID uuid, Rank rank, String grantedBy, long grantedAt, String grantedReason,
                 long duration, List<String> scopes) {
        this.id = UUID.randomUUID();
        this.uuid = uuid;
        this.rank = rank.getUuid();
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.grantedReason = grantedReason;
        this.duration = duration;
        this.end = duration == -1 ? -1 : grantedAt + duration;
        this.scopes = scopes;
    }

    public Grant(JsonElement element) {
        Grant temp = JsonConfigurationService.gson.fromJson(element, Grant.class);

        this.id = temp.id;
        this.uuid = temp.uuid;
        this.rank = temp.rank;
        this.grantedBy = temp.grantedBy;
        this.grantedAt = temp.grantedAt;
        this.grantedReason = temp.grantedReason;
        this.removedBy = temp.removedBy;
        this.removedAt = temp.removedAt;
        this.removedReason = temp.removedReason;
        this.scopes = temp.scopes;
        this.duration = temp.duration;
        this.end = temp.end;
        this.removed = temp.removed;
    }

    public boolean isActive() {
        if (end == -1) {
            return true;
        }

        return end >= System.currentTimeMillis();
    }

    public boolean isActiveOnScope() {
        return isActiveOn(Altara.getSharedInstance().getServerGroup());
    }

    public boolean isActiveOn(String scope) {
        if (scope.equalsIgnoreCase("GLOBAL") || scopes.contains("GLOBAL"))
            return true;

        return scopes.contains(scope.toLowerCase());
    }

    public long getRemainingTime() {
        return this.end - System.currentTimeMillis();
    }

    public Rank asRank() {
        return Altara.getSharedInstance().getRankService().getRank(rank);
    }

    public JsonObject toJson() {
        return JsonConfigurationService.gson.toJsonTree(this).getAsJsonObject();
    }


}