package games.sparking.altara.world;

import games.sparking.altara.AltaraPaper;
import org.bukkit.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for loading, copying, and unloading Altara arena worlds.
 *
 * <p>This class is intentionally framework-agnostic: it only depends on Paper
 * and {@link AltaraPaper}.  It can be used from any plugin code, lobby systems,
 * map editors, or game instances.
 *
 * <h2>Map storage layout</h2>
 * <pre>
 * plugins/Altara/maps/
 *   bomblobbers/
 *     skybridge/          ← template world folder
 *       WorldConfig.dat
 *       region/
 *       level.dat
 *   skywars/
 *     ...
 * </pre>
 *
 * <h2>Loading a map</h2>
 * <pre>{@code
 * MapLoader.load("bomblobbers", "skybridge", instanceShortId)
 *     .thenAccept(arena -> {
 *         // `arena` is an AltaraWorld ready to use
 *         List<Location> redSpawns = arena.getSpawns("Red");
 *     })
 *     .exceptionally(err -> { err.printStackTrace(); return null; });
 * }</pre>
 *
 * <h2>Unloading a map</h2>
 * <pre>{@code
 * MapLoader.unload(arena.getWorld()).thenRun(() -> System.out.println("World deleted."));
 * }</pre>
 *
 * <h2>Listing available maps</h2>
 * <pre>{@code
 * List<String> maps = MapLoader.getAvailableMaps("bomblobbers");
 * Optional<String> random = MapLoader.getRandomMap("bomblobbers");
 * }</pre>
 */
public final class MapLoader {

    /**
     * Absolute path to the shared read-only map templates directory.
     * Because this path is outside any individual server's folder, all server
     * instances on this machine share the same map files without duplication.
     * Each game instance still copies the template to its own server root
     * (under a unique world folder) so instances remain fully isolated.
     *
     * <p>Change this at runtime with {@code MapLoader.MAPS_DIRECTORY = "…"} if
     * you need a different path per environment.
     */
    public static String MAPS_DIRECTORY = "C:/Users/andre/Desktop/Sparking/Maps";

    /**
     * Prefix applied to every live-copy world folder name.
     * e.g. {@code altara-A1B2C3D4}
     */
    public static String WORLD_PREFIX = "altara-";

    private MapLoader() {}

    // =========================================================================
    // Primary API
    // =========================================================================

    /**
     * Asynchronously loads a map for the given game type and instance.
     *
     * <ol>
     *   <li><b>Async</b> – copies the template folder to a unique live folder.</li>
     *   <li><b>Main thread</b> – loads the world with Bukkit's {@link WorldCreator}.</li>
     *   <li><b>Async</b> – parses {@code WorldConfig.dat} into an {@link AltaraWorld}.</li>
     * </ol>
     *
     * <p>The returned future completes on an arbitrary thread.  Use
     * {@code .thenAcceptAsync(..., runnable -> Bukkit.getScheduler().runTask(plugin, runnable))}
     * if you need to interact with the game state on the main thread.
     *
     * @param gameType   subfolder inside {@link #MAPS_DIRECTORY} (e.g. {@code "bomblobbers"})
     * @param mapName    the map folder name (e.g. {@code "skybridge"})
     * @param instanceId unique suffix for the live world folder (usually a game's short ID)
     * @return a future resolving to the fully-loaded {@link AltaraWorld}
     */
    public static CompletableFuture<AltaraWorld> load(String gameType, String mapName, String instanceId) {
        Path templateDir = Path.of(MAPS_DIRECTORY, gameType, mapName);
        Path templateZip = Path.of(MAPS_DIRECTORY, gameType, mapName + ".zip");
        String worldName = WORLD_PREFIX + instanceId;
        Path destination = Path.of(worldName).toAbsolutePath();

        return CompletableFuture
                // Step 1 – copy/extract template (async, IO-bound)
                .runAsync(() -> {
                    try {
                        if (Files.isDirectory(templateDir)) {
                            copyDirectory(templateDir, destination);
                        } else if (Files.exists(templateZip)) {
                            extractZip(templateZip, destination);
                        } else {
                            throw new RuntimeException(
                                    "Map template not found (tried directory and .zip): "
                                    + templateDir.toAbsolutePath());
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load map template '" + mapName + "'", e);
                    }
                })
                // Step 2 – load world (must run on main thread)
                .thenCompose(v -> loadWorldOnMainThread(worldName))
                // Step 3 – inject WorldConfig.dat from the original template into the world
                // folder Paper chose (Paper's LegacyCraftBukkitWorldMigration deletes our
                // original copy during world load, so we re-source it from the template).
                .thenApplyAsync(world -> {
                    injectWorldConfig(templateDir, templateZip, world);
                    return AltaraWorld.parse(world);
                });
    }

    /**
     * Convenience overload that picks a <em>random</em> available map for the
     * given game type.  Fails with an exception future if no maps exist.
     *
     * @param gameType   game type sub-folder
     * @param instanceId unique suffix for the live world folder
     * @return a future resolving to the loaded {@link AltaraWorld}
     */
    public static CompletableFuture<AltaraWorld> loadRandom(String gameType, String instanceId) {
        return getRandomMap(gameType)
                .map(mapName -> load(gameType, mapName, instanceId))
                .orElseGet(() -> CompletableFuture.failedFuture(
                        new RuntimeException("No maps available for game type: " + gameType)));
    }

    /**
     * Unloads the given world from the server (without saving) and deletes its
     * folder from disk.  The unload runs on the main thread; the folder deletion
     * runs asynchronously afterwards.
     *
     * @param world the Bukkit world to remove
     * @return a future that completes when the folder has been deleted
     */
    public static CompletableFuture<Void> unload(World world) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Capture both paths before unloading (world object becomes invalid after unload).
        // Paper moves standard Minecraft files to world/dimensions/minecraft/<name>/
        // but leaves non-standard files (WorldConfig.dat) at the original copy path.
        Path paperFolder   = world.getWorldFolder().toPath().toAbsolutePath();  // where Paper put world data
        Path originalCopy  = Path.of(world.getWorldFolder().getName()).toAbsolutePath(); // our copy at server root

        runOnMainThread(() -> {
//            world.getPlayers().forEach(p -> p.kickPlayer("§cArena closed."));
            Bukkit.unloadWorld(world, /* save= */ false);

            CompletableFuture.runAsync(() -> {
                try {
                    // Delete Paper's dimension folder (region files etc.)
                    deleteDirectory(paperFolder);
                    // Delete our original copy folder (WorldConfig.dat and any leftovers)
                    if (!originalCopy.equals(paperFolder)) {
                        deleteDirectory(originalCopy);
                    }
                    future.complete(null);
                } catch (IOException e) {
                    log(Level.WARNING, "Failed to delete world folders for '" + world.getWorldFolder().getName() + "'", e);
                    future.completeExceptionally(e);
                }
            });
        });

        return future;
    }


    // =========================================================================
    // Map discovery
    // =========================================================================

    /**
     * Returns a list of all map names available for the given game type.
     * Reads the {@link #MAPS_DIRECTORY} directory; returns an empty list on error.
     *
     * @param gameType the game type sub-folder
     * @return sorted list of map names (directory names)
     */
    public static List<String> getAvailableMaps(String gameType) {
        Path dir = Path.of(MAPS_DIRECTORY, gameType);

        log(Level.INFO, "Scanning for maps — resolved path: " + dir.toAbsolutePath(), null);

        if (!Files.isDirectory(dir)) {
            log(Level.WARNING, "Map directory does not exist or is not a folder: " + dir.toAbsolutePath(), null);
            return List.of();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            List<String> maps = stream
                    .filter(p -> Files.isDirectory(p) || p.getFileName().toString().endsWith(".zip"))
                    .map(p -> {
                        String name = p.getFileName().toString();
                        return name.endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
                    })
                    .sorted()
                    .collect(Collectors.toList());

            log(Level.INFO, "Found " + maps.size() + " map(s) for '" + gameType + "': " + maps, null);
            return maps;
        } catch (IOException e) {
            log(Level.WARNING, "Could not list maps for '" + gameType + "'", e);
            return List.of();
        }
    }

    /**
     * Returns a randomly chosen available map for the given game type, or
     * {@link Optional#empty()} if no maps exist.
     *
     * @param gameType the game type sub-folder
     * @return a random map name, or empty
     */
    public static Optional<String> getRandomMap(String gameType) {
        List<String> maps = getAvailableMaps(gameType);
        if (maps.isEmpty()) return Optional.empty();
        return Optional.of(maps.get(new Random().nextInt(maps.size())));
    }

    /**
     * Returns {@code true} if at least one map template exists for the given
     * game type.
     */
    public static boolean hasAnyMap(String gameType) {
        return !getAvailableMaps(gameType).isEmpty();
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Schedules world creation on the main thread (required by Bukkit) and wraps
     * the result in a {@link CompletableFuture}.
     */
    private static CompletableFuture<World> loadWorldOnMainThread(String worldName) {
        CompletableFuture<World> future = new CompletableFuture<>();

        runOnMainThread(() -> {
            try {
                World world = new WorldCreator(worldName)
                        .environment(World.Environment.NORMAL)
                        .createWorld();

                if (world == null) {
                    future.completeExceptionally(
                            new RuntimeException("WorldCreator returned null for '" + worldName + "'"));
                    return;
                }

                // Sensible arena defaults.
                world.setDifficulty(Difficulty.HARD);
                world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);

                future.complete(world);
            } catch (Exception e) {
                future.completeExceptionally(
                        new RuntimeException("Exception while loading world '" + worldName + "'", e));
            }
        });

        return future;
    }

    /**
     * Copies {@code WorldConfig.dat} from the template (directory or zip) into
     * {@code world.getWorldFolder()}.  Called after the world has been loaded on
     * the main thread, because Paper's LegacyCraftBukkitWorldMigration deletes the
     * original copy folder during import.
     */
    private static void injectWorldConfig(Path templateDir, Path templateZip, World world) {
        Path target = world.getWorldFolder().toPath().resolve("WorldConfig.dat");
        if (Files.exists(target)) return; // already there – nothing to do

        try {
            Files.createDirectories(target.getParent());

            if (Files.isDirectory(templateDir)) {
                Path src = templateDir.resolve("WorldConfig.dat");
                if (Files.exists(src)) {
                    Files.copy(src, target, StandardCopyOption.REPLACE_EXISTING);
                    log(Level.INFO, "Injected WorldConfig.dat from template directory into " + target, null);
                } else {
                    log(Level.WARNING, "WorldConfig.dat missing from template: " + src.toAbsolutePath(), null);
                }
            } else if (Files.exists(templateZip)) {
                extractFileFromZip(templateZip, "WorldConfig.dat", target);
                log(Level.INFO, "Injected WorldConfig.dat from zip into " + target, null);
            }
        } catch (IOException e) {
            log(Level.WARNING, "Failed to inject WorldConfig.dat for world '" + world.getName() + "'", e);
        }
    }

    /**
     * Extracts a single named file from a zip archive into {@code target}.
     * Handles zips that wrap everything under a single top-level folder.
     */
    private static void extractFileFromZip(Path zipFile, String fileName, Path target) throws IOException {
        String stripPrefix = detectZipPrefix(zipFile);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (!stripPrefix.isEmpty() && name.startsWith(stripPrefix)) {
                    name = name.substring(stripPrefix.length());
                }
                if (name.equals(fileName)) {
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    zis.closeEntry();
                    return;
                }
                zis.closeEntry();
            }
        }
        log(Level.WARNING, fileName + " not found inside zip: " + zipFile.toAbsolutePath(), null);
    }

    /** Returns the common top-level folder prefix for a zip, or "" if none. */
    private static String detectZipPrefix(Path zipFile) throws IOException {
        String stripPrefix = null;
        try (ZipInputStream probe = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = probe.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                int slash = name.indexOf('/');
                String top = (slash >= 0) ? name.substring(0, slash + 1) : name;
                if (stripPrefix == null) {
                    stripPrefix = top;
                } else if (!name.startsWith(stripPrefix)) {
                    stripPrefix = "";
                    break;
                }
                probe.closeEntry();
            }
        }
        return (stripPrefix != null && stripPrefix.endsWith("/")) ? stripPrefix : "";
    }

    /**
     * Recursively copies {@code source} into {@code target}.
     * Skips the {@code uid.dat} and {@code session.lock} files so the copy is
     * treated as a fresh world by the Bukkit server.
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path dest = target.resolve(source.relativize(dir));
                Files.createDirectories(dest);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                // Skip world identity / lock files so the copy is clean.
                if (name.equals("uid.dat") || name.equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Extracts a zip archive into {@code destination}, mirroring the zip's
     * internal directory structure.  Files named {@code uid.dat} or
     * {@code session.lock} are skipped so the extracted world is treated as
     * fresh by the Bukkit server.
     *
     * <p>If the zip contains a single top-level folder (the common "wrapped"
     * layout), that folder is transparently unwrapped so the destination is
     * the world root, not a parent directory.
     */
    private static void extractZip(Path zipFile, Path destination) throws IOException {
        // Detect whether all entries share a single top-level prefix.
        final String prefix = detectZipPrefix(zipFile);

        // Second pass: actual extraction.
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (!prefix.isEmpty() && name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                }
                if (name.isEmpty()) { zis.closeEntry(); continue; }

                // Skip world identity / lock files so the copy is clean.
                String fileName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
                if (fileName.equals("uid.dat") || fileName.equals("session.lock")) {
                    zis.closeEntry();
                    continue;
                }

                Path target = destination.resolve(name);
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Recursively deletes a directory tree.  Silently returns if the path does
     * not exist.
     */
    private static void deleteDirectory(Path root) throws IOException {
        if (!Files.exists(root)) return;

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void runOnMainThread(Runnable action) {
        Bukkit.getScheduler().runTask(AltaraPaper.getPlugin(), action);
    }

    private static void log(Level level, String msg, Throwable t) {
        if (t != null) {
            AltaraPaper.getPlugin().getLogger().log(level, msg, t);
        } else {
            AltaraPaper.getPlugin().getLogger().log(level, msg);
        }
    }
}

