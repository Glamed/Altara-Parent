package games.sparking.altara.profile;

import com.google.gson.JsonObject;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.task.Tasks;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ProfileService {

    @Getter
    private static final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();

    public void loadProfile(UUID uuid, Consumer<Profile> callback, boolean async) {
        if (uuid == null) {
            callback.accept(null);
            return;
        }

        if (getProfile(uuid) != null) {
            callback.accept(getProfile(uuid));
            return;
        }

        if (async) {
            Tasks.runAsync(() -> loadProfile(uuid, callback, false));
            return;
        }

        RequestResponse response = RequestHandler.get("api/profile/%s", uuid.toString());
        if (!response.wasSuccessful()) {
            callback.accept(null);
            return;
        }

        Profile profile = new Profile(response.asObject());
        if (getProfile(uuid) != null) {
            callback.accept(getProfile(uuid));
            return;
        }

        profiles.put(uuid, profile);
        callback.accept(profile);
    }

    public Profile loadProfile(UUID uuid) {
        Profile profile = getProfile(uuid);
        if (profile != null)
            return profile;

        RequestResponse response = RequestHandler.get("api/profile/%s", uuid.toString());
        if (!response.wasSuccessful()) {
            return null;
        }

        profile = new Profile(response.asObject());
        profiles.put(uuid, profile);
        return profile;
    }

    private void createProfile(UUID uuid, String name, String ip, Consumer<Profile> callback, boolean async) {
        if (async) {
            Tasks.runAsync(() -> createProfile(uuid, name, ip, callback, false));
            return;
        }

        Profile profile = new Profile(uuid, name);
        profile.setLastIp(ip);
        profile.getKnownIps().add(ip);

        RequestResponse response = RequestHandler.post("api/profile", profile.toJson());
        if (response.wasSuccessful())
            loadProfile(uuid, callback, false);
        else
            callback.accept(null);
    }

    public void getProfileOrCreate(UUID uuid, String name, String ip, Consumer<Profile> callback, boolean async) {
        loadProfile(uuid, profile -> {
            if (profile != null) {
                callback.accept(profile);
                return;
            }
            createProfile(uuid, name, ip, callback, async);
        }, async);
    }

    public Profile getProfile(Player player) {
        return profiles.getOrDefault(player.getUniqueId(), null);
    }

    public Profile getProfile(UUID uuid) {
        if (uuid == null) return null;
        return profiles.get(uuid);
    }

    public void removeProfile(UUID uuid) {
        if (uuid == null) return;
        profiles.remove(uuid);
    }

    /** Saves the cached profile to the API and refreshes it from the response. */
    public void updateProfile(UUID uuid, Consumer<Profile> callback, boolean async) {
        Profile profile = getProfile(uuid);
        if (profile == null) {
            if (callback != null) callback.accept(null);
            return;
        }

        JsonObject object = profile.toSaveJson();
        Runnable save = () -> {
            RequestResponse response = RequestHandler.put("api/profile/%s", object, uuid.toString());
            if (response.wasSuccessful()) {
                profile.update(response.asObject());
            }
            if (callback != null) callback.accept(response.wasSuccessful() ? profile : null);
        };

        if (async) Tasks.runAsync(save);
        else save.run();
    }

    /**
     * The cached profile, or a fresh read from the API that is <em>not</em> cached (for
     * players on other servers).  Returns {@code null} if it can't be loaded.  Blocking.
     */
    public Profile fetchProfile(UUID uuid) {
        Profile cached = getProfile(uuid);
        if (cached != null) return cached;

        RequestResponse response = RequestHandler.get("api/profile/%s", uuid.toString());
        return response.wasSuccessful() ? new Profile(response.asObject()) : null;
    }

    /**
     * Re-reads a cached profile from the API (e.g. after a grant or punishment changed it
     * elsewhere).  Does nothing if the profile isn't cached.  Blocking.
     */
    public Profile refreshProfile(UUID uuid) {
        Profile profile = getProfile(uuid);
        if (profile == null) return null;

        RequestResponse response = RequestHandler.get("api/profile/%s", uuid.toString());
        if (response.wasSuccessful()) {
            profile.update(response.asObject());
        }
        return profile;
    }

    public void getAlts(Profile profile, Consumer<List<Profile>> callback, boolean async) {
        if (async) {
            Tasks.runAsync(() -> getAlts(profile, callback, false));
            return;
        }

        RequestResponse response = RequestHandler.get("api/profile/" + profile.getUuid() + "/alts");
        if (response.wasSuccessful()) {
            List<Profile> alts = new ArrayList<>();
            // Not cached: alts are usually offline, and a cached copy would be reused
            // (stale) when they next log in.
            response.asArray().forEach(element -> alts.add(new Profile(element.getAsJsonObject())));
            if (callback != null) callback.accept(alts);
        } else {
            if (callback != null) callback.accept(new ArrayList<>());
        }
    }

    public void cacheProfile(Profile profile) {
        profiles.put(profile.getUuid(), profile);
    }
}