package games.sparking.altara.rank;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.Timings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class RankService {

    private final Map<UUID, Rank> ranks = new ConcurrentHashMap<>();
    @Getter
    private boolean loaded = false;

    public void loadRanks(Runnable callback) {
        Tasks.runAsync(() -> {
            System.out.println("[CONFIG] Loading ranks...");
            Timings timings = new Timings("rank-loading").startTimings();
            ranks.clear();

            RequestResponse response = RequestHandler.get("api/rank");
            if (!response.wasSuccessful()) {
                System.out.printf("[WARN] Could not load ranks: %s (%d)%n",
                        response.getErrorMessage(), response.getCode());
                return;
            }

            JsonArray rankArray = response.asArray();
            rankArray.forEach(object -> {
                Rank rank = new Rank(object.getAsJsonObject());
                ranks.put(rank.getUuid(), rank);
            });

            for (Rank rank : ranks.values()) {
                response = RequestHandler.get("api/rank/%s", rank.getUuid().toString());
                if (!response.wasSuccessful()) {
                    System.out.printf("[WARN] Could not load inherits for %s: %s (%d)%n",
                            rank.getName(), response.getErrorMessage(), response.getCode());
                    continue;
                }

                JsonObject object = response.asObject();
                if (!object.has("inherits"))
                    continue;

                object.get("inherits").getAsJsonArray().forEach(element -> {
                    Rank inherit = getRank(UUID.fromString(element.getAsString()));
                    if (inherit != null)
                        rank.getInherits().add(inherit);
                });
            }

            System.out.println(String.format("[INFO] Loaded %d ranks in %dms",
                    ranks.size(), timings.stopTimings().calculateDifference()));
            loaded = true;
            callback.run();
        });
    }

    public void loadRank(UUID uuid, Consumer<Rank> callback) {
        Tasks.runAsync(() -> {
            RequestResponse response = RequestHandler.get("api/rank/%s", uuid.toString());
            if (!response.wasSuccessful()) {
                System.out.println(String.format("[WARN] Could not load rank %s: %s (%d)",
                        uuid, response.getErrorMessage(), response.getCode()));
                return;
            }

            JsonObject object = response.asObject();
            Rank rank = new Rank(object);
            object.get("inherits").getAsJsonArray().forEach(element -> {
                Rank inherit = getRank(UUID.fromString(element.getAsString()));
                if (inherit != null)
                    rank.getInherits().add(inherit);
            });
            ranks.put(rank.getUuid(), rank);
            callback.accept(rank);
        });
    }

    public void deleteRank(UUID uuid, Runnable callback) {
        Tasks.runAsync(() -> {
            RequestResponse response = RequestHandler.delete("api/rank/%s", uuid.toString());
            if (response.getCode() != 404 && !response.wasSuccessful()) {
                System.out.println(String.format("[WARN] Could not delete rank %s: %s (%d)",
                        uuid, response.getErrorMessage(), response.getCode()));
                return;
            }

            Rank rank = getRank(uuid);
            if (rank != null) {
                System.out.println(String.format("[CONFIG] Deleting rank %s...", rank.getName()));
                ranks.remove(rank.getUuid());
                Altara.getSharedInstance().updatePermissionsWithRank(rank);
            }

            callback.run();
        });
    }

    public void updateRank(UUID uuid, Runnable callback) {
        Rank rank = getRank(uuid);
        if (rank != null)
            System.out.println(String.format("[CONFIG] Updating rank %s...", rank.getName()));

        loadRank(uuid, (newRank) -> {
            Altara.getSharedInstance().updatePermissionsWithRank(newRank);
            callback.run();
            ranks.put(newRank.getUuid(), newRank);
        });
    }

    public Rank getRank(UUID uuid) {
        return ranks.get(uuid);
    }

    public Rank getRank(String name) {
        for (Rank rank : ranks.values()) {
            if (rank.getName().equalsIgnoreCase(name))
                return rank;
        }

        return null;
    }

    public Rank getDefaultRank() {
        for (Rank rank : ranks.values()) {
            if (rank.isDefaultRank()) {
                return rank;
            }
        }

        System.out.println("[INFO] Default rank missing, creating a new one");
        Rank found = new Rank("Member");
        found.setDefaultRank(true);

        RequestResponse response = RequestHandler.post("api/rank", found.toJson());
        if (!response.wasSuccessful())
            System.out.println(String.format("[WARN] Could not create default rank: %s (%d)",
                    response.getErrorMessage(), response.getCode()));

        ranks.put(found.getUuid(), found);
        return found;
    }

    public List<Rank> getRanks() {
        return new ArrayList<>(ranks.values());
    }

    public List<Rank> getRanksSorted() {
        List<Rank> sortedRanks = new ArrayList<>(this.ranks.values());
        sortedRanks.sort(Comparator.comparingInt(Rank::getWeight));
        Collections.reverse(sortedRanks);
        return sortedRanks;
    }

    public List<Rank> getRanksSortedPriority() {
        List<Rank> sortedRanks = new ArrayList<>(this.ranks.values());
        sortedRanks.sort(Comparator.comparingInt(Rank::getQueuePriority));
        Collections.reverse(sortedRanks);
        return sortedRanks;
    }

    public void cacheRank(Rank rank) {
        this.ranks.put(rank.getUuid(), rank);
    }
}