package games.sparking.altara.updater;

import games.sparking.altara.Altara;
import games.sparking.altara.AltaraPaper;
import games.sparking.altara.reboot.RebootService;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
                    RebootService.reboot(5 * 60 * 1000L); // 5 minute countdown
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