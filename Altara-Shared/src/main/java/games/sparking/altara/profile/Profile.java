package games.sparking.altara.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.SystemType;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.disguise.DisguiseData;
import games.sparking.altara.grant.Grant;
import games.sparking.altara.grant.GrantProcedure;
import games.sparking.altara.profile.packet.ProfileUpdatePacket;
import games.sparking.altara.punishment.Punishment;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.IllegalSystemTypeException;
import games.sparking.altara.utils.Timings;
import games.sparking.altara.utils.json.JsonBuilder;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    public void save(Runnable callable, boolean async) {
        if (!async)
            this.lock.lock();
        try {
            if (async) {
                Tasks.runAsync(() -> save(callable, false));
                return;
            }

            this.disguiseData.save(() -> {}, false);

            RequestResponse response = RequestHandler.put("profile", toJson());
            if (response.wasSuccessful())
                new ProfileUpdatePacket(this.uuid).publish();
            else Altara.getSharedInstance().getLogger().warn(String.format(
                    "Could not save profile of %s (%s): %s (%d)",
                    uuid.toString(),
                    name,
                    response.getErrorMessage(),
                    response.getCode()
            ));
            callable.run();
        } finally {
            if (!async)
                this.lock.unlock();
        }
    }

    public void update(JsonObject object) {
        this.lock.lock();
        try {
            this.name = object.get("name").getAsString();
            this.lastIp = object.get("lastIp").getAsString();

            this.knownIps.clear();
            object.get("knownIps").getAsJsonArray().forEach(element ->
                    this.knownIps.add(element.getAsString()));

            this.options = new ProfileOptions(object.get("options").getAsJsonObject());

            this.activeGrants.clear();
            object.get("activeGrants").getAsJsonArray().forEach(element ->
                    activeGrants.add(new Grant(element.getAsJsonObject())));

            this.permissions.clear();
            if (object.has("permissions")) {
                object.get("permissions").getAsJsonArray().forEach(element ->
                        permissions.add(element.getAsString()));
            }

            this.punishments.clear();
            punishments.addAll(Altara.getSharedInstance().getPunishmentService().getPunishments(uuid));

            this.disguiseData = Altara.getSharedInstance().getDisguiseService().getDisguiseData(this.uuid);
            this.isDisguised = !disguiseData.getDisguiseName().equals("N/A");
            this.disguiseName = disguiseData.getDisguiseName();
            
            this.firstLogin = object.get("firstLogin").getAsLong();
            this.lastSeen = object.get("lastSeen").getAsLong();
            this.playTime = object.get("playTime").getAsLong();
            this.joinTime = object.get("joinTime").getAsLong();

            if (object.has("lastServer"))
                this.lastServer = object.get("lastServer").getAsString();
            else this.lastServer = null;

            if (Altara.getSystemType() == SystemType.PAPER) {
                if (Bukkit.getPlayer(this.uuid) != null)
                    Objects.requireNonNull(Bukkit.getPlayer(this.uuid)).setDisplayName(this.getDisplayName());
            }
        } finally {
            this.lock.unlock();
        }
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
            if (grant.getUuid().equals(rank.getUuid()))
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
                    "Disgused",
                    -1,
                    Collections.singletonList("GLOBAL")
            );
        }

        return this.getRealCurrentGrant();
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

    public Grant getCurrentGrantOn(String scope) {
        if (this.isDisguised) {
            return new Grant(
                    this.uuid,
                    this.disguiseData.getDisguiseRank(),
                    "Console",
                    System.currentTimeMillis(),
                    "Disgused",
                    -1,
                    Collections.singletonList("GLOBAL")
            );
        }

        return this.getRealCurrentGrantOn(scope);
    }

    public Grant getRealCurrentGrantOn(String scope) {
        Grant grant = null;

        for (Grant current : this.getActiveGrantsOn(scope)) {
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

    public List<Punishment> getPunishments(Punishment.LegacyType type) {
        List<Punishment> list = new ArrayList<>();
        for (Punishment punishment : punishments) {
            if (type.name().equals(punishment.getInfractionType()))
                list.add(punishment);
        }
        return list;
    }

    public Punishment getActivePunishment(Punishment.LegacyType type) {
        for (Punishment punishment : punishments) {
            if (punishment.isActive() && !punishment.isRemoved()
                    && type.name().equals(punishment.getInfractionType()))
                return punishment;
        }
        return null;
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
                        ChatColor.GRAY + "(" + this.name + ")" : "");
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