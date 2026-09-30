package games.sparking.altara.rank;

import com.google.gson.JsonElement;
import games.sparking.altara.Altara;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.Timings;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RankService {

    /** Replaced wholesale on reload so readers never see a half-loaded set. */
    private volatile Map<UUID, Rank> ranks = new ConcurrentHashMap<>();

    @Getter
    private volatile boolean loaded = false;

    /** Placeholder default used until ranks have loaded — never persisted. */
    private final Rank fallbackDefault = createFallbackDefault();

    public void loadRanks(Runnable callback) {
        Tasks.runAsync(() -> {
            Altara.getSharedInstance().getLogger().info("Loading ranks...");
            Timings timings = new Timings("rank-loading").startTimings();

            RequestResponse response = RequestHandler.get("api/rank");
            if (!response.wasSuccessful()) {
                Altara.getSharedInstance().getLogger().warn(String.format("Could not load ranks: %s (%d)",
                        response.getErrorMessage(), response.getCode()));
                return;
            }

            Map<UUID, Rank> loadedRanks = new ConcurrentHashMap<>();
            for (JsonElement element : response.asArray()) {
                try {
                    Rank rank = new Rank(element.getAsJsonObject());
                    loadedRanks.put(rank.getUuid(), rank);
                } catch (RuntimeException e) {
                    Altara.getSharedInstance().getLogger().warn("Skipping malformed rank: " + e.getMessage());
                }
            }

            this.ranks = loadedRanks;
            this.loaded = true;
            Altara.getSharedInstance().getLogger().info(String.format("Loaded %d ranks in %dms",
                    loadedRanks.size(), timings.stopTimings().calculateDifference()));
            callback.run();
        });
    }

    public void loadRank(UUID uuid, Consumer<Rank> callback) {
        Tasks.runAsync(() -> {
            RequestResponse response = RequestHandler.get("api/rank/%s", uuid.toString());
            if (!response.wasSuccessful()) {
                Altara.getSharedInstance().getLogger().warn(String.format("Could not load rank %s: %s (%d)",
                        uuid, response.getErrorMessage(), response.getCode()));
                return;
            }

            Rank rank = new Rank(response.asObject());
            ranks.put(rank.getUuid(), rank);
            callback.accept(rank);
        });
    }

    /** Deletes the rank through the API; the API's delete packet updates every server. */
    public void deleteRank(UUID uuid, Consumer<String> feedback, Runnable callback) {
        Tasks.runAsync(() -> {
            RequestResponse response = RequestHandler.delete("api/rank/%s", uuid.toString());
            if (response.getCode() != 404 && !response.wasSuccessful()) {
                feedback.accept("Could not delete rank: " + response.getErrorMessage() + " (" + response.getCode() + ")");
                return;
            }

            removeLocally(uuid);
            callback.run();
        });
    }

    /** Drops a deleted rank from this server's cache and refreshes affected players. */
    public void removeLocally(UUID uuid) {
        Rank rank = ranks.remove(uuid);
        if (rank != null) {
            Altara.getSharedInstance().handleRankDeletion(rank);
            Altara.getSharedInstance().updatePermissionsWithRank(rank);
        }
    }

    public void updateRank(UUID uuid, Runnable callback) {
        loadRank(uuid, newRank -> {
            Altara.getSharedInstance().updatePermissionsWithRank(newRank);
            callback.run();
        });
    }

    public Rank getRank(UUID uuid) {
        return uuid == null ? null : ranks.get(uuid);
    }

    public Rank getRank(String name) {
        for (Rank rank : ranks.values()) {
            if (rank.getName().equalsIgnoreCase(name))
                return rank;
        }
        return null;
    }

    /**
     * The rank flagged as default.  Before ranks load this returns an in-memory placeholder;
     * once loaded, a missing default is created exactly once.
     */
    public Rank getDefaultRank() {
        for (Rank rank : ranks.values()) {
            if (rank.isDefaultRank()) return rank;
        }

        if (!loaded) return fallbackDefault;

        synchronized (this) {
            for (Rank rank : ranks.values()) {
                if (rank.isDefaultRank()) return rank;
            }

            Altara.getSharedInstance().getLogger().info("Default rank missing, creating one");
            Rank created = createFallbackDefault();
            RequestResponse response = RequestHandler.post("api/rank", created.toJson());
            if (!response.wasSuccessful()) {
                Altara.getSharedInstance().getLogger().warn(String.format("Could not create default rank: %s (%d)",
                        response.getErrorMessage(), response.getCode()));
            }
            ranks.put(created.getUuid(), created);
            return created;
        }
    }

    private static Rank createFallbackDefault() {
        Rank rank = new Rank("Member");
        rank.setDefaultRank(true);
        rank.setPrefix("<gray>");
        rank.setColor("<gray>");
        rank.setChatColor("<white>");
        return rank;
    }

    public List<Rank> getRanks() {
        return new ArrayList<>(ranks.values());
    }

    /** Highest weight first. */
    public List<Rank> getRanksSorted() {
        List<Rank> sorted = new ArrayList<>(ranks.values());
        sorted.sort(Rank.COMPARATOR);
        return sorted;
    }

    /** Highest queue priority first. */
    public List<Rank> getRanksSortedPriority() {
        List<Rank> sorted = new ArrayList<>(ranks.values());
        sorted.sort(Comparator.comparingInt(Rank::getQueuePriority).reversed());
        return sorted;
    }

    public void cacheRank(Rank rank) {
        ranks.put(rank.getUuid(), rank);
    }
}
