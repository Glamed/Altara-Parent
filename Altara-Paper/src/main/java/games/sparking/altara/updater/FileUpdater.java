package games.sparking.altara.updater;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import games.sparking.altara.updater.events.RestartServerEvent;
import games.sparking.altara.utils.CC;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileUpdater implements Listener {

    private static final FilenameFilter JAR_FILTER = (file, name) -> name.endsWith(".jar");

    @Getter
    private static Properties buildProperties;

    private final Map<String, String> _jarMd5Map = new HashMap<>();
    private final AtomicBoolean _restartTriggered = new AtomicBoolean(false);
    private boolean _enabled;

    // -------------------------------------------------------------------------
    // Build properties
    // -------------------------------------------------------------------------

    private void loadBuildProperties() {
        buildProperties = new Properties();
        try {
            buildProperties.load(this.getClass().getResourceAsStream("/version.properties"));
        } catch (Exception e) {
            System.out.println(Arrays.toString(e.getStackTrace()));
        }
    }

    // -------------------------------------------------------------------------
    // MD5 hashing (no external dependency)
    // -------------------------------------------------------------------------

    private static String md5Hex(InputStream input) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = input.read(buf)) != -1) md.update(buf, 0, n);
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 not available", e);
        }
    }

    // -------------------------------------------------------------------------
    // Hash collection
    // -------------------------------------------------------------------------

    private void collectHashes(File[] files) {
        if (files == null) return;
        String serverName = Altara.getSharedInstance().getLocalServerName();
        for (File file : files) {
            try (FileInputStream stream = new FileInputStream(file)) {
                String fileName = (serverName != null)
                        ? file.getName().replace(serverName, "server")
                        : file.getName();
                _jarMd5Map.put(fileName, md5Hex(stream));
            } catch (IOException ex) {
                System.err.println("Failed to parse hash for file: " + file.getName() + ":");
                ex.printStackTrace();
            }
        }
    }

    private void getJarHashes() {
        collectHashes(new File(".").listFiles(JAR_FILTER));
        collectHashes(new File("plugins").listFiles(JAR_FILTER));
    }

    // -------------------------------------------------------------------------
    // Update check (runs async every minute)
    // -------------------------------------------------------------------------

    private void checkForUpdates() {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        File updateDir = new File(windows
                ? "C:\\Users\\andre\\Desktop\\Sparking\\Update"
                : "/home/mineplex/update"
        );
        File[] files = updateDir.listFiles(JAR_FILTER);
        if (files == null) return;

        String serverName = Altara.getSharedInstance().getLocalServerName();
        for (File file : files) {
            String key = (serverName != null)
                    ? file.getName().replace(serverName, "server")
                    : file.getName();
            String hash = _jarMd5Map.get(key);
            if (hash == null) continue;
            try (FileInputStream stream = new FileInputStream(file)) {
                String newHash = md5Hex(stream);
                if (!hash.equals(newHash)) {
                    System.out.println(file.getName() + " old hash: " + hash);
                    System.out.println(file.getName() + " new hash: " + newHash);
                    attemptToRestart(RestartReason.JAR_UPDATE, 5 * 60); // 5 minute countdown
                }
            } catch (IOException ex) {
                System.err.println("Failed to parse hash for file: " + file.getName() + ":");
                ex.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.SLOWER || !_enabled || _restartTriggered.get()) return;
        Tasks.runAsync(this::checkForUpdates);
    }

    // -------------------------------------------------------------------------
    // Restart with countdown broadcasts + titles
    // -------------------------------------------------------------------------

    private void attemptToRestart(RestartReason reason, int delayInSeconds) {
        if (!_restartTriggered.compareAndSet(false, true)) return; // only trigger once

        int totalMinutes = Math.max(1, delayInSeconds / 60);

        // Schedule per-minute countdown broadcasts + titles
        for (int i = 1; i <= totalMinutes; i++) {
            final int minutesRemaining = totalMinutes - i + 1;
            long tickOffset = (long) (totalMinutes - minutesRemaining) * 60 * 20;

            Tasks.runLater(() -> {
                String minuteWord = minutesRemaining == 1 ? "minute" : "minutes";

                // Broadcast every pre-formatted message from the reason's description list.
                for (String line : reason.getDescription()) {
                    Bukkit.broadcastMessage(CC.translate(line));
                }

                Component titleText    = Component.text("⚠ REBOOTING ⚠", NamedTextColor.RED);
                Component subtitleText = Component.text(
                        "Rebooting in " + minutesRemaining + " " + minuteWord + ".",
                        NamedTextColor.WHITE
                );
                Title title = Title.title(
                        titleText,
                        subtitleText,
                        Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofSeconds(4),
                                Duration.ofMillis(500)
                        )
                );
                Bukkit.getOnlinePlayers().forEach(p -> p.showTitle(title));
            }, tickOffset);
        }

        // Fire the actual restart event after the full delay
        Tasks.runLater(() -> {
            RestartServerEvent restartEvent = new RestartServerEvent(reason);
            AltaraPaper.getPlugin().getServer().getPluginManager().callEvent(restartEvent);
            if (restartEvent.isCancelled()) {
                _restartTriggered.set(false); // allow re-trigger if cancelled
                return;
            }

            AltaraPaper.getPlugin().getServer().restart();
        }, (long) totalMinutes * 60 * 20);
    }

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public FileUpdater() {
        _enabled = !new File("IgnoreUpdates.dat").exists();
        if (_enabled) {
            loadBuildProperties();
            getJarHashes();
        }
        Bukkit.getPluginManager().registerEvents(this, AltaraPaper.getPlugin());
    }

}