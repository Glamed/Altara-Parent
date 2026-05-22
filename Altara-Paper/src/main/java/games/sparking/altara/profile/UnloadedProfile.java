package games.sparking.altara.profile;

import games.sparking.altara.Altara;
import games.sparking.altara.task.Tasks;
import lombok.Getter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;


@Getter
public class UnloadedProfile {

    private final UUID uuid;
    private final String name;

    public UnloadedProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void load(Consumer<Profile> callable, boolean async) {
        Altara.getSharedInstance().getProfileService().loadProfile(uuid, callable, async);
    }

    public void loadPlayer(Consumer<Player> callable, boolean async) {
        if (async) {
            Tasks.runAsync(() -> loadPlayer(callable, false));
            return;
        }

        if (Bukkit.getPlayer(uuid) != null) {
            callable.accept(Bukkit.getPlayer(uuid));
            return;
        }

        if (!Bukkit.getOfflinePlayer(uuid).hasPlayedBefore()) {
            callable.accept(null);
            return;
        }

//        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
//        EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), new GameProfile(uuid, name),
//                new PlayerInteractManager(server.getWorld()));
//        Player player = entity.getBukkitEntity();
        Player player = Bukkit.getPlayer(uuid);

        if (player != null) {
            player.loadData();
        }
        callable.accept(player);
    }

    public void loadBoth(Consumer<Pair<Profile, Player>> callable, boolean async) {
        if (async) {
            Tasks.runAsync(() -> loadBoth(callable, false));
            return;
        }

        load(profile -> {
            loadPlayer(player -> {
                callable.accept(new ImmutablePair<>(profile, player));
            }, false);
        }, false);
    }
}