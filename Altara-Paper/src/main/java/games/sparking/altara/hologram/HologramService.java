package games.sparking.altara.hologram;

import games.sparking.altara.configuration.ConfigurationService;
import games.sparking.altara.hologram.config.HologramConfig;
import games.sparking.altara.hologram.config.HologramConfigEntry;
import games.sparking.altara.hologram.statics.StaticHologram;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class HologramService {

    // ------------------------------------------------------------------
    // Static registry – all active holograms regardless of persistence
    // ------------------------------------------------------------------

    private static final Map<Integer, Hologram> CURRENT_HOLOGRAMS = new ConcurrentHashMap<>();
    private static int hologramId = 1;

    protected static int registerHologram(Hologram hologram) {
        int id = hologramId++;
        CURRENT_HOLOGRAMS.put(id, hologram);
        return id;
    }

    public static void unregisterHologram(int id) {
        CURRENT_HOLOGRAMS.remove(id);
    }

    public static Collection<Hologram> getHolograms() {
        return CURRENT_HOLOGRAMS.values();
    }

    // ------------------------------------------------------------------
    // Instance – manages persisted (config-backed) static holograms
    // ------------------------------------------------------------------

    private final Plugin plugin;
    private final ConfigurationService configurationService;

    private HologramConfig config;
    private final Map<Integer, StaticHologram> serializedHolograms = new HashMap<>();
    private final Map<String, Integer> nameMapping = new HashMap<>();
    private final Set<String> loadedWorlds = new HashSet<>();

    public void load() {
        config = configurationService.loadConfiguration(HologramConfig.class,
                new File(plugin.getDataFolder(), "holograms.json"));

        // WorldLoadEvent fires before the plugin enables, so any worlds that are
        // already loaded by the time load() is called must be handled explicitly here.
        for (World world : Bukkit.getWorlds()) {
            loadWorld(world);
        }
    }

    public void loadWorld(World world) {
        if (config == null) return;
        if (!loadedWorlds.add(world.getName())) return; // already loaded, prevent duplicates
        for (HologramConfigEntry entry : config.getHolograms()) {
            if (entry.getLocation().getWorld().equals(world.getName())) {
                StaticHologram hologram = new HologramBuilder()
                        .at(entry.getLocation().getLocation())
                        .withSpacing(entry.getSpacing())
                        .staticHologram()
                        .addLines(entry.getLines())
                        .build();
                hologram.setName(entry.getName());
                hologram.spawn();
                serializedHolograms.put(hologram.getId(), hologram);
                if (entry.getName() != null)
                    nameMapping.put(entry.getName().toLowerCase(), hologram.getId());
            }
        }
    }

    public void save() {
        if (config == null) return;
        config.getHolograms().clear();
        for (StaticHologram hologram : serializedHolograms.values())
            config.getHolograms().add(hologram.toConfig());

        try {
            configurationService.saveConfiguration(config,
                    new File(plugin.getDataFolder(), "holograms.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StaticHologram getHologram(Integer id) {
        return serializedHolograms.get(id);
    }

    public StaticHologram getHologram(String name) {
        Integer id = nameMapping.get(name.toLowerCase());
        return id == null ? null : getHologram(id);
    }

    public void remove(StaticHologram hologram) {
        hologram.unregister(); // destroys entities + removes from CURRENT_HOLOGRAMS
        serializedHolograms.remove(hologram.getId());
        if (hologram.getName() != null)
            nameMapping.remove(hologram.getName().toLowerCase());
    }

    public void register(StaticHologram hologram) {
        serializedHolograms.put(hologram.getId(), hologram);
        if (hologram.getName() != null)
            nameMapping.put(hologram.getName().toLowerCase(), hologram.getId());
    }

    public List<StaticHologram> getSerializedHolograms() {
        return Collections.unmodifiableList(new ArrayList<>(serializedHolograms.values()));
    }

}
