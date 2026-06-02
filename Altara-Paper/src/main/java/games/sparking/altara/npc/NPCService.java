package games.sparking.altara.npc;

import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.npc.config.NPCConfig;
import games.sparking.altara.npc.config.NPCConfigEntry;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class NPCService {

    // -----------------------------------------------------------------------
    // Static registry — every spawned NPC (including non-persisted ones)
    // -----------------------------------------------------------------------

    private static final Map<UUID, NPC> CURRENT_NPCS = new ConcurrentHashMap<>();
    private static int npcIdCounter = 1;

    protected static int registerNpc(NPC npc) {
        CURRENT_NPCS.put(npc.getUuid(), npc);
        return npcIdCounter++;
    }

    public static void unregisterNpc(UUID uuid) {
        CURRENT_NPCS.remove(uuid);
    }

    public static Collection<NPC> getNpcs() {
        return Collections.unmodifiableCollection(CURRENT_NPCS.values());
    }

    public static NPC getNpc(UUID uuid) {
        return CURRENT_NPCS.get(uuid);
    }

    // -----------------------------------------------------------------------
    // Instance — manages persisted (config-backed) NPCs
    // -----------------------------------------------------------------------

    private final Plugin plugin;
    private final ConfigurationService configurationService;

    private NPCConfig config;
    private final Map<Integer, NPC> serializedNpcs = new HashMap<>();
    private final Map<String, Integer> nameMapping   = new HashMap<>();
    private final Set<String> loadedWorlds           = new HashSet<>();

    public void load() {
        config = configurationService.loadConfiguration(NPCConfig.class,
                new File(plugin.getDataFolder(), "npcs.json"));

        // Worlds already loaded by the time the plugin enables must be handled here.
        for (World world : Bukkit.getWorlds()) {
            loadWorld(world);
        }

        startRotationTask();
    }

    public void loadWorld(World world) {
        if (config == null) return;
        if (!loadedWorlds.add(world.getName())) return; // prevent double-load

        for (NPCConfigEntry entry : config.getNpcs()) {
            if (!entry.getLocation().getWorld().equals(world.getName())) continue;

            NPCBuilder builder = new NPCBuilder()
                    .at(entry.getLocation().getLocation())
                    .displayName(entry.getDisplayName());

            if (entry.getTexture() != null)
                builder.skinTexture(entry.getTexture(), entry.getSignature());

            if (entry.getCommand() != null) {
                if (entry.isConsoleCommand())
                    builder.consoleCommand(entry.getCommand());
                else
                    builder.command(entry.getCommand());
            }

            if (entry.getEquipment() != null)
                entry.getEquipment().forEach(builder::withEquipment);

            NPC npc = builder.buildAndSpawn();
            npc.setName(entry.getName());
            npc.spawn();
            serializedNpcs.put(npc.getId(), npc);
            if (entry.getName() != null)
                nameMapping.put(entry.getName().toLowerCase(), npc.getId());
        }
    }

    public void save() {
        if (config == null) return;
        config.getNpcs().clear();
        for (NPC npc : serializedNpcs.values())
            config.getNpcs().add(npc.toConfig());

        try {
            configurationService.saveConfiguration(config,
                    new File(plugin.getDataFolder(), "npcs.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------------------------
    // Lookup
    // -----------------------------------------------------------------------

    public NPC getNpc(Integer id) {
        return serializedNpcs.get(id);
    }

    public NPC getNpc(String name) {
        Integer id = nameMapping.get(name.toLowerCase());
        return id == null ? null : getNpc(id);
    }

    public List<NPC> getSerializedNpcs() {
        return Collections.unmodifiableList(new ArrayList<>(serializedNpcs.values()));
    }

    // -----------------------------------------------------------------------
    // Mutation
    // -----------------------------------------------------------------------

    public void register(NPC npc) {
        serializedNpcs.put(npc.getId(), npc);
        if (npc.getName() != null)
            nameMapping.put(npc.getName().toLowerCase(), npc.getId());
    }

    public void remove(NPC npc) {
        npc.unregister(); // destroys entities + removes from static registry
        serializedNpcs.remove(npc.getId());
        if (npc.getName() != null)
            nameMapping.remove(npc.getName().toLowerCase());
    }

    // -----------------------------------------------------------------------
    // Rotation keep-alive
    // -----------------------------------------------------------------------

    private void startRotationTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (NPC npc : CURRENT_NPCS.values())
                npc.rotate(npc.getLocation().getYaw());
        }, 60L, 60L);
    }
}
