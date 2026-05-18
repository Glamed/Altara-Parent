package games.sparking.altara.world;

import games.sparking.altara.AltaraPaper;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

/**
 * Represents a loaded Altara arena world and its configuration data parsed from
 * a {@code WorldConfig.dat} file located in the world's root folder.
 *
 * <p>This class can be used anywhere on a Paper server – it is not tied to the
 * game framework.  Any plugin that loads a Bukkit {@link World} can wrap it in
 * an {@code AltaraWorld} to get typed access to map metadata and spawn points.
 *
 * <h2>WorldConfig.dat format</h2>
 * <pre>
 * MAP_NAME:Sky Bridge
 * MAP_AUTHOR:andredev
 * MIN_X:-256
 * MAX_X:256
 * MIN_Y:0
 * MAX_Y:256
 * MIN_Z:-256
 * MAX_Z:256
 *
 * # Team spawn points (gold block markers)
 * TEAM_NAME:Red
 * TEAM_SPAWNS:100,64,0:102,64,2:104,64,0
 *
 * TEAM_NAME:Blue
 * TEAM_SPAWNS:-100,64,0:-102,64,2
 *
 * # Game-specific data markers (iron block markers)
 * DATA_NAME:chest
 * DATA_LOCS:50,65,0:51,65,0
 *
 * # Free-form custom locations (sponge block markers)
 * CUSTOM_NAME:lobby
 * CUSTOM_LOCS:0,70,0
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // After loading a World with Bukkit's WorldCreator:
 * AltaraWorld arena = AltaraWorld.parse(world);
 *
 * String name       = arena.getMapName();       // "Sky Bridge"
 * List<Location> red  = arena.getSpawns("Red"); // team spawn points
 * Location lobby    = arena.getCustomPoint("lobby");
 * }</pre>
 */
public class AltaraWorld {

    // =========================================================================
    // Fields
    // =========================================================================

    @Getter private final World world;
    @Getter private final String mapName;
    @Getter private final String mapAuthor;

    /** The recorded bounding box of the playable area. */
    @Getter private final Location min;
    @Getter private final Location max;

    /**
     * Team spawn-point lists, keyed by team name as written in {@code TEAM_NAME}.
     * Corresponds to Mineplex's "gold block locations".
     */
    private final Map<String, List<Location>> spawnLocations;

    /**
     * Game-data marker lists, keyed by name as written in {@code DATA_NAME}.
     * Corresponds to Mineplex's "iron block locations".
     */
    private final Map<String, List<Location>> dataLocations;

    /**
     * Arbitrary custom marker lists, keyed by name as written in {@code CUSTOM_NAME}.
     * Corresponds to Mineplex's "sponge block locations".
     */
    private final Map<String, List<Location>> customLocations;

    // =========================================================================
    // Constructor (private – use parse())
    // =========================================================================

    private AltaraWorld(World world, String mapName, String mapAuthor,
                        Location min, Location max,
                        Map<String, List<Location>> spawnLocations,
                        Map<String, List<Location>> dataLocations,
                        Map<String, List<Location>> customLocations) {
        this.world           = world;
        this.mapName         = mapName;
        this.mapAuthor       = mapAuthor;
        this.min             = min;
        this.max             = max;
        this.spawnLocations  = spawnLocations;
        this.dataLocations   = dataLocations;
        this.customLocations = customLocations;
    }

    // =========================================================================
    // Static factory – parses WorldConfig.dat
    // =========================================================================

    /**
     * Parses the {@code WorldConfig.dat} from an explicit directory rather than
     * {@code world.getWorldFolder()}.  Use this overload from {@link MapLoader}
     * to avoid Paper's dimension path mismatch, where the server stores the
     * world at {@code world/dimensions/minecraft/<name>/} but the template was
     * copied to a different location on disk.
     *
     * @param world     the already-loaded Bukkit {@link World}
     * @param configDir the directory that <em>actually</em> contains {@code WorldConfig.dat}
     * @return the parsed arena world (never {@code null})
     */
    public static AltaraWorld parse(World world, java.nio.file.Path configDir) {
        return parseFromDir(world, configDir.toFile());
    }

    /**
     * Parses the {@code WorldConfig.dat} file from the given world's folder and
     * returns a fully-populated {@link AltaraWorld}.
     *
     * <p>Matches Mineplex's fault-tolerant behaviour: all exceptions are caught
     * internally and logged.  If the file is missing or unparseable the method
     * still returns a valid (but empty) {@link AltaraWorld} so the game can
     * continue without crashing the load pipeline.
     *
     * @param world the already-loaded Bukkit {@link World}
     * @return the parsed arena world (never {@code null})
     */
    public static AltaraWorld parse(World world) {
        return parseFromDir(world, world.getWorldFolder());
    }

    /** Returns the first {@code WorldConfig.dat} found by checking {@code primary} then {@code world.getWorldFolder()}. */
    private static File findWorldConfig(World world, File primary) {
        File f1 = new File(primary, "WorldConfig.dat");
        if (f1.exists()) return f1;
        File f2 = new File(world.getWorldFolder(), "WorldConfig.dat");
        if (f2.exists()) return f2;
        return null;
    }

    private static AltaraWorld parseFromDir(World world, File directory) {
        Map<String, List<Location>> spawnLocs  = new LinkedHashMap<>();
        Map<String, List<Location>> dataLocs   = new LinkedHashMap<>();
        Map<String, List<Location>> customLocs = new LinkedHashMap<>();

        String mapName = world.getName(), mapAuthor = "Unknown";
        int minX = -256, minY = 0, minZ = -256;
        int maxX =  256, maxY = 256, maxZ =  256;

        try {
            File configFile = findWorldConfig(world, directory);

            if (configFile == null) {
                AltaraPaper.getPaperInstance().getLogger().warning(
                        "[AltaraWorld] WorldConfig.dat not found for world '" + world.getName() + "'."
                        + " Searched:"
                        + "\n  1) " + new File(directory, "WorldConfig.dat").getAbsolutePath()
                        + "\n  2) " + new File(world.getWorldFolder(), "WorldConfig.dat").getAbsolutePath());
            } else {
                AltaraPaper.getPaperInstance().getLogger().info(
                        "[AltaraWorld] Reading WorldConfig.dat for '" + world.getName()
                        + "' from: " + configFile.getAbsolutePath());

                List<String> lines = Files.readAllLines(configFile.toPath());
                List<Location> current = null;

                for (String line : lines) {
                    String[] tokens = line.split(":");
                    if (tokens.length < 2 || tokens[0].isEmpty()) continue;

                    String key   = tokens[0];
                    String value = tokens[1];

                    if (key.equalsIgnoreCase("MAP_NAME")) {
                        mapName = value;
                    } else if (key.equalsIgnoreCase("MAP_AUTHOR")) {
                        mapAuthor = value;
                    } else if (key.equalsIgnoreCase("TEAM_NAME")) {
                        current = spawnLocs.computeIfAbsent(value, k -> new ArrayList<>());
                    } else if (key.equalsIgnoreCase("TEAM_SPAWNS")) {
                        for (int i = 1; i < tokens.length; i++) {
                            Location loc = parseLocation(world, tokens[i]);
                            if (loc != null && current != null) current.add(loc);
                        }
                    } else if (key.equalsIgnoreCase("DATA_NAME")) {
                        current = dataLocs.computeIfAbsent(value, k -> new ArrayList<>());
                    } else if (key.equalsIgnoreCase("DATA_LOCS")) {
                        for (int i = 1; i < tokens.length; i++) {
                            Location loc = parseLocation(world, tokens[i]);
                            if (loc != null && current != null) current.add(loc);
                        }
                    } else if (key.equalsIgnoreCase("CUSTOM_NAME")) {
                        current = customLocs.computeIfAbsent(value, k -> new ArrayList<>());
                    } else if (key.equalsIgnoreCase("CUSTOM_LOCS")) {
                        for (int i = 1; i < tokens.length; i++) {
                            Location loc = parseLocation(world, tokens[i]);
                            if (loc != null && current != null) current.add(loc);
                        }
                    } else if (key.equalsIgnoreCase("MIN_X")) { minX = Integer.parseInt(value);
                    } else if (key.equalsIgnoreCase("MAX_X")) { maxX = Integer.parseInt(value);
                    } else if (key.equalsIgnoreCase("MIN_Y")) { minY = Integer.parseInt(value);
                    } else if (key.equalsIgnoreCase("MAX_Y")) { maxY = Integer.parseInt(value);
                    } else if (key.equalsIgnoreCase("MIN_Z")) { minZ = Integer.parseInt(value);
                    } else if (key.equalsIgnoreCase("MAX_Z")) { maxZ = Integer.parseInt(value);
                    }
                }

                AltaraPaper.getPaperInstance().getLogger().info(
                        "[AltaraWorld] Parsed '" + world.getName() + "': map=" + mapName
                        + "  redSpawns=" + spawnLocs.getOrDefault("Red", List.of()).size()
                        + "  blueSpawns=" + spawnLocs.getOrDefault("Blue", List.of()).size()
                        + "  allTeams=" + spawnLocs.keySet());
            }
        } catch (Exception e) {
            AltaraPaper.getPaperInstance().getLogger().log(Level.SEVERE,
                    "[AltaraWorld] Exception parsing WorldConfig.dat for world '" + world.getName() + "'", e);
        }

        Location min = new Location(world, minX, minY, minZ);
        Location max = new Location(world, maxX, maxY, maxZ);

        return new AltaraWorld(world, mapName, mapAuthor, min, max,
                spawnLocs, dataLocs, customLocs);
    }

    // =========================================================================
    // Public accessors – spawn locations
    // =========================================================================

    /**
     * Returns all spawn points for the given team name.
     *
     * @param team the team name as written in {@code TEAM_NAME} (case-sensitive)
     * @return an unmodifiable list of spawn {@link Location}s; empty if not found
     */
    public List<Location> getSpawns(String team) {
        return Collections.unmodifiableList(spawnLocations.getOrDefault(team, List.of()));
    }

    /**
     * Returns the first spawn point for the given team, or {@code null} if none exist.
     */
    public Location getSpawn(String team) {
        List<Location> locs = spawnLocations.get(team);
        return (locs == null || locs.isEmpty()) ? null : locs.get(0);
    }

    /** Returns all registered team spawn maps. */
    public Map<String, List<Location>> getAllSpawns() {
        return Collections.unmodifiableMap(spawnLocations);
    }

    // =========================================================================
    // Public accessors – data locations
    // =========================================================================

    public List<Location> getData(String key) {
        return Collections.unmodifiableList(dataLocations.getOrDefault(key, List.of()));
    }

    public Location getDataPoint(String key) {
        List<Location> locs = dataLocations.get(key);
        return (locs == null || locs.isEmpty()) ? null : locs.get(0);
    }

    public Map<String, List<Location>> getAllData() {
        return Collections.unmodifiableMap(dataLocations);
    }

    // =========================================================================
    // Public accessors – custom locations
    // =========================================================================

    public List<Location> getCustom(String key) {
        return Collections.unmodifiableList(customLocations.getOrDefault(key, List.of()));
    }

    public Location getCustomPoint(String key) {
        List<Location> locs = customLocations.get(key);
        return (locs == null || locs.isEmpty()) ? null : locs.get(0);
    }

    public Map<String, List<Location>> getAllCustom() {
        return Collections.unmodifiableMap(customLocations);
    }

    // =========================================================================
    // Formatting
    // =========================================================================

    /**
     * Returns a player-facing description line, e.g.:
     * {@code "§aMap: §fSky Bridge §7by §fandredev"}
     */
    public String getFormattedName() {
        return "§aMap: §f" + mapName + " §7by §f" + mapAuthor;
    }

    @Override
    public String toString() {
        return "AltaraWorld{world='" + world.getName() + "', map='" + mapName
                + "', author='" + mapAuthor + "', spawns=" + spawnLocations.keySet() + "}";
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Parses a {@code "x,y,z"} coordinate string into a {@link Location}.
     * The resulting X and Z are offset by +0.5 so players stand in the block
     * centre (consistent with Mineplex behaviour).
     */
    private static Location parseLocation(World world, String coord) {
        String[] parts = coord.split(",");
        if (parts.length < 3) return null;
        try {
            double x = Integer.parseInt(parts[0].trim()) + 0.5;
            double y = Integer.parseInt(parts[1].trim());
            double z = Integer.parseInt(parts[2].trim()) + 0.5;
            // Optional yaw/pitch: "x,y,z,yaw,pitch"
            float yaw = parts.length >= 4 ? Float.parseFloat(parts[3].trim()) : 0f;
            float pitch = parts.length >= 5 ? Float.parseFloat(parts[4].trim()) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            AltaraPaper.getPaperInstance().getLogger().warning(
                    "[AltaraWorld] Invalid location string: '" + coord + "'");
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { return fallback; }
    }
}
