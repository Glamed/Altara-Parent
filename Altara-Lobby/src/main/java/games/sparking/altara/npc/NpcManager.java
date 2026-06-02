package games.sparking.altara.npc;


import games.sparking.altara.AltaraLobby;
import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.updating.UpdatingHologram;
import games.sparking.altara.selector.ServerSelectorEntry;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class NpcManager {

    private final AltaraLobby plugin;
    private final Map<String, NPC> npcMap = new HashMap<>();
    private final Map<String, UpdatingHologram> hologramMap = new HashMap<>();

    public void loadNpcs() {
        for (ServerSelectorEntry entry : plugin.getLobbyConfig().getServerSelector()) {
            if (entry.getNpcLocation() == null)
                continue;

            spawnForEntry(entry);
        }
    }

    public void spawnForEntry(ServerSelectorEntry entry) {
        NPC npc = new NPCBuilder()
                .at(entry.getNpcLocation().getLocation())
                .command("joinqueue " + entry.getServerName())
                .skinName(entry.getNpcSkin())
                .buildAndSpawn();

        npcMap.put(entry.getServerName().toLowerCase(), npc);

        UpdatingHologram hologram = new HologramBuilder()
                .at(entry.getNpcLocation().getLocation().clone().add(0, 1.7, 0))
                .updating()
                .interval(1, TimeUnit.SECONDS)
                .provider(new NpcHologramProvider(entry))
                .build();
        hologram.spawn(); // show immediately to any players already online
        hologram.start();

        hologramMap.put(entry.getServerName().toLowerCase(), hologram);
    }

    public void despawnEverythingOf(String serverName) {
        NPC npc = npcMap.remove(serverName.toLowerCase());
        if (npc != null) npc.unregister();

        UpdatingHologram hologram = hologramMap.remove(serverName.toLowerCase());
        if (hologram != null) {
            hologram.cancel();
            hologram.unregister();
        }
    }

    public NPC getNpc(String name) {
        return npcMap.get(name.toLowerCase());
    }

    public void despawnEverything() {
        npcMap.values().forEach(NPC::unregister);
        hologramMap.values().forEach(h -> { h.cancel(); h.unregister(); });
        npcMap.clear();
        hologramMap.clear();
    }

}