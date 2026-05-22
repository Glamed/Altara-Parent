package games.sparking.altara.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.IllegalSystemTypeException;
import games.sparking.altara.utils.Timings;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Profile {

    private UUID uuid;
    private String name;

    @BsonIgnore
    private final Lock lock = new ReentrantLock();

    private String status;
    private String lastServer;
    private String lastIp = "N/A";
    private List<String> knownIps = new ArrayList<>();
    private long firstLogin = System.currentTimeMillis();
    private long lastlogin = System.currentTimeMillis();
    private long lastlogout;

    @BsonIgnore
    private CopyOnWriteArrayList<Grant> activeGrants = new CopyOnWriteArrayList<>();
    @BsonIgnore
    private GrantProcedure grantProcedure = null;

    @BsonIgnore
    private List<Profile> alts = null;
    @BsonIgnore
    private List<String> permissions = new ArrayList<>();

    @BsonIgnore
    private Timings session;
    private long playTime = 0;

    private String authToken;
    private long authTokenExpiry;

    private String discordID;
    private String discordEmail;
    private String discordAccessToken;
    private String discordRefreshToken;

    public Profile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.session = new Timings(name + "-session");
    }

    public Profile(JsonObject object) {
        if (object.has("uuid") && !object.get("uuid").isJsonNull()) {
            this.uuid = UUID.fromString(object.get("uuid").getAsString());
        } else {
            throw new IllegalArgumentException("Profile JSON is missing a valid UUID.");
        }
        this.session = new Timings(name + "-session");
        update(object);
    }

    public void update(JsonObject object) {
        this.lock.lock();
        try {
            if (object.has("name") && !object.get("name").isJsonNull()) {
                this.name = object.get("name").getAsString();
            }

            if (object.has("status") && !object.get("status").isJsonNull()) {
                this.status = object.get("status").getAsString();
            }

            if (object.has("lastIp") && !object.get("lastIp").isJsonNull()) {
                this.lastIp = object.get("lastIp").getAsString();
            }

            if (object.has("knownIps")) {
                this.knownIps.clear();
                object.get("knownIps").getAsJsonArray().forEach(element ->
                        this.knownIps.add(element.getAsString()));
            }

            if (object.has("firstLogin") && !object.get("firstLogin").isJsonNull()) {
                this.firstLogin = object.get("firstLogin").getAsLong();
            }

            if (object.has("lastlogin") && !object.get("lastlogin").isJsonNull()) {
                this.lastlogin = object.get("lastlogin").getAsLong();
            }

            if (object.has("lastlogout") && !object.get("lastlogout").isJsonNull()) {
                this.lastlogout = object.get("lastlogout").getAsLong();
            }

            if (object.has("activeGrants")) {
                this.activeGrants.clear();
                object.get("activeGrants").getAsJsonArray().forEach(element ->
                        activeGrants.add(new Grant(element.getAsJsonObject())));
            }

            if (object.has("playTime") && !object.get("playTime").isJsonNull()) {
                this.playTime = object.get("playTime").getAsLong();
            }

            if (object.has("authToken") && !object.get("authToken").isJsonNull()) {
                this.authToken = object.get("authToken").getAsString();
            }

            if (object.has("authTokenExpiry") && !object.get("authTokenExpiry").isJsonNull()) {
                this.authTokenExpiry = object.get("authTokenExpiry").getAsLong();
            }

            if (object.has("discordID") && !object.get("discordID").isJsonNull()) {
                this.discordID = object.get("discordID").getAsString();
            }

            if (object.has("discordEmail") && !object.get("discordEmail").isJsonNull()) {
                this.discordEmail = object.get("discordEmail").getAsString();
            }

            if (object.has("discordAccessToken") && !object.get("discordAccessToken").isJsonNull()) {
                this.discordAccessToken = object.get("discordAccessToken").getAsString();
            }

            if (object.has("discordRefreshToken") && !object.get("discordRefreshToken").isJsonNull()) {
                this.discordRefreshToken = object.get("discordRefreshToken").getAsString();
            }

            if (object.has("lastServer") && !object.get("lastServer").isJsonNull()) {
                this.lastServer = object.get("lastServer").getAsString();
            } else {
                this.lastServer = null;
            }

            // ✅ Null-safe Bukkit UUID usage
            if (Altara.getSystemType() == SystemType.PAPER && this.uuid != null) {
                Player player = Bukkit.getPlayer(this.uuid);
                if (player != null) {
                    player.setDisplayName(this.name);
                }
            }

        } finally {
            this.lock.unlock();
        }
    }

    public long getTotalPlayTime() {
        return session != null ? playTime + session.calculateDifference() : playTime;
    }

    public boolean hasPrimeStatus() {
        return hasGrantOf("prime");
    }

    public JsonObject toJson() {
        JsonBuilder builder = new JsonBuilder();

        builder.add("uuid", uuid);
        builder.add("name", name);
        builder.add("status", status);
        builder.add("lastServer", lastServer);
        builder.add("lastIp", lastIp);

        JsonArray knownIpsArray = new JsonArray();
        knownIps.forEach(knownIpsArray::add);
        builder.add("knownIps", knownIpsArray);

        builder.add("firstLogin", firstLogin);
        builder.add("lastlogin", lastlogin);
        builder.add("lastlogout", lastlogout);

        if (options != null) {
            builder.add("options", options.toJson());
        }

        long totalPlayTime = playTime;
        if (session != null) {
            totalPlayTime += session.calculateDifference();
        }
        builder.add("playTime", totalPlayTime);

        builder.add("authToken", authToken);
        builder.add("authTokenExpiry", authTokenExpiry);

        builder.add("discordID", discordID);
        builder.add("discordEmail", discordEmail);
        builder.add("discordAccessToken", discordAccessToken);
        builder.add("discordRefreshToken", discordRefreshToken);

        return builder.build();
    }

    public void save(Runnable callable, boolean async) {
        if (!async)
            this.lock.lock();
        try {
            if (async) {
                Tasks.runAsync(() -> save(callable, false));
                return;
            }

            RequestResponse response = RequestHandler.put("api/profile", toJson());
            if (response.wasSuccessful()) {
                new ProfileUpdatePacket(this.uuid).publish();
            }
            callable.run();
        } finally {
            if (!async)
                this.lock.unlock();
        }
    }

    public Grant getRealCurrentGrant() {
        Grant grant = null;

        for (Grant current : this.getActiveGrants()) {
            if (grant == null) {
                grant = current;
                continue;
            }
            if (current.asRank().getWeight() > grant.asRank().getWeight()) {
                grant = current;
            }
        }

        if (grant == null) {
            grant = new Grant(
                    this.uuid,
                    Altara.getSharedInstance().getRankService().getDefaultRank(),
                    "Console",
                    System.currentTimeMillis(),
                    "Default Grant",
                    -1,
                    Collections.singletonList("GLOBAL")
            );
        }

        return grant;
    }

    @BsonIgnore
    public List<Grant> getAllActiveGrants() {
        List<Grant> list = new ArrayList<>();
        for (Grant grant : activeGrants) {
            if (grant.isActive() && !grant.isRemoved() && grant.getRank() != null)
                list.add(grant);
        }
        list.sort(Grant.COMPARATOR.reversed());
        return list;
    }

    @BsonIgnore
    public List<Grant> getActiveGrants() {
        return getActiveGrants(null);
    }

    @BsonIgnore
    public List<Grant> getActiveGrants(String scope) {
        List<Grant> activeGrants = getAllActiveGrants();
        if (scope == null) {
            activeGrants.removeIf(grant -> !grant.isActiveOnScope());
        } else {
            activeGrants.removeIf(grant -> !grant.isActiveOn(scope));
        }
        return activeGrants;
    }

    @BsonIgnore
    public boolean hasGrantOf(Rank rank) {
        for (Grant grant : getActiveGrants()) {
            if (grant.getUuid().equals(rank.getUuid()))
                return true;
        }
        return false;
    }

    @BsonIgnore
    public boolean hasGrantOf(String rank) {
        for (Grant grant : getActiveGrants()) {
            if (grant.asRank().getName().equalsIgnoreCase(rank))
                return true;
        }
        return false;
    }

    public Player player() {
        IllegalSystemTypeException.checkOrThrow(SystemType.BUKKIT);

        return Bukkit.getPlayer(this.uuid);
    }
}