package games.sparking.altara.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.disguise.DisguiseData;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.punishment.PunishmentType;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.IllegalSystemTypeException;
import games.sparking.altara.utils.Timings;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class Profile {

    public static final Comparator<Profile> WEIGHT_COMPARATOR =
            Collections.reverseOrder(Comparator.comparingInt(profile
                    -> profile.getCurrentGrant().asRank().getWeight()));

    public static final Comparator<Profile> REAL_WEIGHT_COMPARATOR =
            Collections.reverseOrder(Comparator.comparingInt(profile
                    -> profile.getRealCurrentGrant().asRank().getWeight()));
    
    private final UUID uuid;
    private final Lock lock = new ReentrantLock();
    private String name;
    private String lastIp = "N/A";
    private List<String> knownIps = new ArrayList<>();

    private ProfileOptions options;

    private CopyOnWriteArrayList<Grant> activeGrants = new CopyOnWriteArrayList<>();
    private List<Punishment> punishments = new ArrayList<>();
    private List<Profile> alts = null;
    private List<String> permissions = new ArrayList<>();

    private long firstLogin = System.currentTimeMillis();
    private long lastSeen = System.currentTimeMillis();
    private long joinTime = -1;
    private long lastSpeakMillis;

    private Timings session;
    private long playTime = 0;

    private String lastServer = null;
    private boolean nitroBoosted = false;
    private boolean frozen = false;
    private boolean devMode = false;
    private boolean requiresAuthentication = false;
    private int authenticationFailures = 0;

    private DisguiseData disguiseData;
    private boolean isDisguised = false;
    private String disguiseName = "N/A";

    private GrantProcedure grantProcedure = null;

    public Profile(JsonObject object) {
        this.uuid = UUID.fromString(object.get("uuid").getAsString());
        this.session = new Timings(name + "-session");
        update(object);
    }

    public Profile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.session = new Timings(name + "-session");
        this.disguiseData = new DisguiseData(uuid);
        this.options = new ProfileOptions();
    }

    /**
     * Folds the session's elapsed time into {@link #playTime} and restarts the session
     * clock, so each moment of play is counted exactly once no matter how often the
     * profile is saved.
     */
    public void commitSession() {
        this.lock.lock();
        try {
            this.playTime += session.calculateDifference();
            session.restart();
        } finally {
            this.lock.unlock();
        }
    }

    /** {@link #commitSession()} then {@link #toJson()} — use for every PUT to the API. */
    public JsonObject toSaveJson() {
        commitSession();
        return toJson();
    }

    public JsonObject toJson() {
        JsonBuilder builder = new JsonBuilder();

        builder.add("uuid", uuid);
        builder.add("name", name);
        builder.add("lastIp", lastIp);

        JsonArray knownIpsArray = new JsonArray();
        knownIps.forEach(knownIpsArray::add);
        builder.add("knownIps", knownIpsArray);

        builder.add("options", options.toJson());

        JsonArray permissionsArray = new JsonArray();
        permissions.forEach(permissionsArray::add);
        builder.add("permissions", permissionsArray);

        builder.add("firstLogin", firstLogin);
        builder.add("lastSeen", lastSeen);
        builder.add("joinTime", joinTime);
        builder.add("playTime", playTime + session.calculateDifference());
        builder.add("lastServer", lastServer);
        return builder.build();
    }

    /**
     * Persists this profile to the Web API and refreshes it from the response.
     * {@code callback} runs afterwards whether or not the save succeeded.
     */
    public void save(Runnable callback, boolean async) {
        if (async) {
            Tasks.runAsync(() -> save(callback, false));
            return;
        }

        if (this.isDisguised && this.disguiseData != null) {
            this.disguiseData.save(() -> {}, false);
        }

        // The API publishes a ProfileUpdatePacket for this save itself.
        RequestResponse response = RequestHandler.put("api/profile/%s", toSaveJson(), uuid.toString());
        if (response.wasSuccessful()) {
            update(response.asObject());
        } else {
            Altara.getSharedInstance().getLogger().warn(String.format(
                    "Could not save profile of %s (%s): %s (%d)",
                    uuid, name, response.getErrorMessage(), response.getCode()));
        }
        callback.run();
    }

    public void update(JsonObject object) {
        this.lock.lock();
        try {
            this.name = string(object, "name", this.name);
            this.lastIp = string(object, "lastIp", "N/A");

            List<String> ips = new ArrayList<>();
            array(object, "knownIps").forEach(element -> ips.add(element.getAsString()));
            this.knownIps = ips;

            this.options = object.has("options") && object.get("options").isJsonObject()
                    ? new ProfileOptions(object.get("options").getAsJsonObject())
                    : new ProfileOptions();

            List<Grant> grants = new ArrayList<>();
            array(object, "activeGrants").forEach(element -> grants.add(new Grant(element.getAsJsonObject())));
            this.activeGrants = new CopyOnWriteArrayList<>(grants);

            List<String> perms = new ArrayList<>();
            array(object, "permissions").forEach(element -> perms.add(element.getAsString()));
            this.permissions = perms;

            this.punishments = new ArrayList<>(Altara.getSharedInstance().getPunishmentService().getPunishments(uuid));

            DisguiseData disguise = Altara.getSharedInstance().getDisguiseService().getDisguiseData(this.uuid);
            this.disguiseData = disguise != null ? disguise : new DisguiseData(this.uuid);
            this.isDisguised = !"N/A".equals(disguiseData.getDisguiseName());
            this.disguiseName = disguiseData.getDisguiseName();

            this.firstLogin = number(object, "firstLogin", this.firstLogin);
            this.lastSeen = number(object, "lastSeen", this.lastSeen);
            this.joinTime = number(object, "joinTime", -1);
            this.lastServer = string(object, "lastServer", null);

            // While the player is online the local play time is authoritative (the session is
            // committed on every save), so only adopt the stored value for offline profiles.
            if (!session.isRunning()) {
                this.playTime = number(object, "playTime", this.playTime);
            }

            if (Altara.getSystemType() == SystemType.PAPER) {
                Player player = Bukkit.getPlayer(this.uuid);
                if (player != null) player.displayName(CC.format(getDisplayName()));
            }
        } finally {
            this.lock.unlock();
        }
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static long number(JsonObject object, String key, long fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsLong();
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    public boolean canInteract(Profile other) {
        long weight = this.getRealCurrentGrant().asRank().getWeight();
        long otherWeight = other.getRealCurrentGrant().asRank().getWeight();

        if (weight >= Altara.getSharedInstance().getMainConfig().getOwnerWeight())
            return true;

        if (weight >= Altara.getSharedInstance().getMainConfig().getAdminWeight()
                && otherWeight < Altara.getSharedInstance().getMainConfig().getAdminWeight())
            return true;

        return otherWeight < Altara.getSharedInstance().getMainConfig().getStaffWeight();
    }

    public List<Grant> getAllActiveGrants() {
        List<Grant> list = new ArrayList<>();
        for (Grant grant : activeGrants) {
            if (grant.isActive() && !grant.isRemoved() && grant.asRank() != null)
                list.add(grant);
        }

        list.sort(Grant.COMPARATOR.reversed());
        return list;
    }

    public List<Grant> getActiveGrants() {
        List<Grant> activeGrants = this.getAllActiveGrants();
        activeGrants.removeIf(grant -> !grant.isActiveOnScope());
        return activeGrants;
    }

    public List<Grant> getActiveGrantsOn(String scope) {
        List<Grant> activeGrants = getAllActiveGrants();
        activeGrants.removeIf(grant -> !grant.isActiveOn(scope));
        return activeGrants;
    }

    public boolean hasGrantOf(Rank rank) {
        for (Grant grant : getActiveGrants()) {
            if (rank.getUuid().equals(grant.getRank()))
                return true;
        }

        return false;
    }

    public boolean hasGrantOf(String rank) {
        for (Grant grant : getActiveGrants()) {
            if (grant.asRank().getName().equalsIgnoreCase(rank))
                return true;
        }

        return false;
    }

    public Grant getCurrentGrant() {
        if (this.isDisguised) {
            return new Grant(
                    this.uuid,
                    this.disguiseData.getDisguiseRank(),
                    "Console",
                    System.currentTimeMillis(),
                    "Disguised",
                    -1,
                    Collections.singletonList("GLOBAL")
            );
        }

        return this.getRealCurrentGrant();
    }

    public Grant getRealCurrentGrant() {
        Grant grant = null;

        for (Grant current : this.getActiveGrants()) {
            // Team grants represent memberships, not display ranks — exclude them here
            if (current.asRank().isTeam()) continue;
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

    public Grant getCurrentGrantOn(String scope) {
        if (this.isDisguised) {
            return new Grant(
                    this.uuid,
                    this.disguiseData.getDisguiseRank(),
                    "Console",
                    System.currentTimeMillis(),
                    "Disguised",
                    -1,
                    Collections.singletonList("GLOBAL")
            );
        }

        return this.getRealCurrentGrantOn(scope);
    }

    public Grant getRealCurrentGrantOn(String scope) {
        Grant grant = null;

        for (Grant current : this.getActiveGrantsOn(scope)) {
            // Team grants represent memberships, not display ranks — exclude them here
            if (current.asRank().isTeam()) continue;
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

    public List<Grant> getActiveTeams() {
        List<Grant> activeTeams = this.getAllActiveGrants();
        activeTeams.removeIf(grant -> !grant.isActiveOnScope());
        activeTeams.removeIf(grant -> !grant.asRank().isTeam());
        return activeTeams;
    }

    public int getQueuePriority(String scope) {
        Grant grant = null;

        for (Grant current : this.getActiveGrantsOn(scope)) {
            if (grant == null) {
                grant = current;
                continue;
            }
            if (current.asRank().getQueuePriority() > grant.asRank().getQueuePriority()) {
                grant = current;
            }
        }

        return (grant == null ? 0 : grant.asRank().getQueuePriority()) + (hasPrimeStatus() ? 1 : 0);
    }

    /** Returns all punishments that contain at least one action of the given type. */
    public List<Punishment> getPunishments(PunishmentType type) {
        List<Punishment> list = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment.getActions() != null &&
                    punishment.getActions().stream().anyMatch(a -> a.getType() == type)) {
                list.add(punishment);
            }
        }
        return list;
    }

    /**
     * Returns the first active punishment that has a non-expired restriction of the
     * given type, or {@code null} if none exist.
     */
    public Punishment getActivePunishment(PunishmentType type) {
        for (Punishment punishment : punishments) {
            if (punishment.hasActiveRestriction(type)) {
                return punishment;
            }
        }
        return null;
    }

    /**
     * Returns all active punishments that have a non-expired restriction of the given type.
     */
    public List<Punishment> getActivePunishments(PunishmentType type) {
        List<Punishment> list = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (punishment.hasActiveRestriction(type)) {
                list.add(punishment);
            }
        }
        return list;
    }

    public String getCurrentName() {
        return this.isDisguised ? this.disguiseName : this.name;
    }

    public String getDisplayName() {
        return this.getCurrentGrant().asRank().getColor() + (this.isDisguised ? this.disguiseName : this.name);
    }

    public String getDisplayName(CommandSender target) {
        IllegalSystemTypeException.checkOrThrow(SystemType.PAPER);

        return this.getDisplayName() +
                (((target == null || target.hasPermission("altara.disguise.bypass")) && this.isDisguised) ?
                        " <gray>(" + this.name + ")" : "");
    }


    public String getRealDisplayName() {
        return this.getRealCurrentGrant().asRank().getColor() + this.name;
    }

    public Player player() {
        IllegalSystemTypeException.checkOrThrow(SystemType.PAPER);

        return Bukkit.getPlayer(this.uuid);
    }

//    public ProxiedPlayer proxiedPlayer() {
//        IllegalSystemTypeException.checkOrThrow(SystemType.BUNGEE);
//
//        return ProxyServer.getInstance().getPlayer(this.uuid);
//    }

    public long getTotalPlayTime() {
        return playTime + session.calculateDifference();
    }

    public boolean hasPrimeStatus() {
        return hasGrantOf("prime");
    }

}