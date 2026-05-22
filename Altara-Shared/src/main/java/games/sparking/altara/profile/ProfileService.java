package games.sparking.altara.profile;

import com.google.gson.JsonObject;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.task.Tasks;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ProfileService {

    @Getter
    private static final Map<UUID, Profile> profiles = new HashMap<>();

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
        return profiles.getOrDefault(uuid, null);
    }

    public void removeProfile(UUID uuid) {
        profiles.remove(uuid);
    }

    public void updateProfile(UUID uuid, Consumer<Profile> callback, boolean async) {
        Profile profile = getProfile(uuid);
        if (profile == null) {
            if (callback != null) callback.accept(null);
            return;
        }

        JsonObject object = profile.toJson();
        if (async) {
            Tasks.runAsync(() -> {
                RequestResponse response = RequestHandler.put("api/profile/" + uuid, object);
                if (response.wasSuccessful()) {
                    profile.update(response.asObject());
                    if (callback != null) callback.accept(profile);
                } else {
                    if (callback != null) callback.accept(null);
                }
            });
        } else {
            RequestResponse response = RequestHandler.put("api/profile/" + uuid, object);
            if (response.wasSuccessful()) {
                profile.update(response.asObject());
                if (callback != null) callback.accept(profile);
            } else {
                if (callback != null) callback.accept(null);
            }
        }
    }

    public void getAlts(Profile profile, Consumer<List<Profile>> callback, boolean async) {
        if (async) {
            Tasks.runAsync(() -> getAlts(profile, callback, false));
            return;
        }

        RequestResponse response = RequestHandler.get("api/profile/" + profile.getUuid() + "/alts");
        if (response.wasSuccessful()) {
            List<Profile> alts = new ArrayList<>();
            response.asArray().forEach(element -> {
                Profile alt = new Profile();
                alt.update(element.getAsJsonObject());
                alts.add(alt);
                cacheProfile(alt);
            });
            if (callback != null) callback.accept(alts);
        } else {
            if (callback != null) callback.accept(new ArrayList<>());
        }
    }

    public void cacheProfile(Profile profile) {
        profiles.put(profile.getUuid(), profile);
    }
}