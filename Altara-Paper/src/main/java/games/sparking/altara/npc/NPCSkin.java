package games.sparking.altara.npc;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.util.MojangAPIUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Holds the Base64-encoded texture value and signature that make up a Minecraft player skin.
 *
 * <p>Obtain a skin via:
 * <ul>
 *   <li>{@link #of(String, String)} – supply pre-fetched texture and signature directly.</li>
 *   <li>{@link #fetchAsync(String, JavaPlugin, Consumer)} – look up a player's skin from
 *       Mojang asynchronously by their username.</li>
 *   <li>{@link #fetchBlocking(String)} – synchronous variant (call only off the main thread).</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * NPCSkin.fetchAsync("Notch", plugin, skin -> {
 *     if (skin != null) {
 *         new NPCBuilder()
 *             .at(location)
 *             .skin(skin)
 *             .build()
 *             .spawn();
 *     }
 * });
 * }</pre>
 */
@Getter
public final class NPCSkin {

    private final String texture;
    private final String signature;

    private NPCSkin(String texture, String signature) {
        this.texture   = texture;
        this.signature = signature;
    }

    /** Creates a skin directly from pre-fetched texture and signature strings. */
    public static NPCSkin of(String texture, String signature) {
        return new NPCSkin(texture, signature);
    }

    /**
     * Creates an {@link NPCSkin} from a PacketEvents {@link TextureProperty}.
     * Useful when you already have the property from the Mojang API.
     */
    public static NPCSkin of(TextureProperty prop) {
        return new NPCSkin(prop.getValue(), prop.getSignature());
    }

    /**
     * Asynchronously fetches the skin for {@code username} from the Mojang API via
     * PacketEvents' built-in {@link MojangAPIUtil}, then delivers the result to
     * {@code callback} on the main thread.
     *
     * <p>If the lookup fails (invalid name, rate-limit, no internet), the callback
     * receives {@code null}.
     *
     * @param username the Minecraft player name whose skin to fetch
     * @param plugin   used to schedule the callback back onto the main thread
     * @param callback receives the {@link NPCSkin} (or {@code null} on failure)
     */
    public static void fetchAsync(String username, JavaPlugin plugin, Consumer<NPCSkin> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            NPCSkin skin = fetchBlocking(username);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(skin));
        });
    }

    /**
     * Blocking skin fetch — must <b>not</b> be called on the main thread.
     *
     * @return the skin, or {@code null} if the lookup failed
     */
    public static NPCSkin fetchBlocking(String username) {
        try {
            UUID uuid = MojangAPIUtil.requestPlayerUUID(username);
            if (uuid == null) return null;

            List<TextureProperty> props = MojangAPIUtil.requestPlayerTextureProperties(uuid);
            if (props.isEmpty()) return null;

            TextureProperty prop = props.get(0);
            return new NPCSkin(prop.getValue(), prop.getSignature());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts this skin to a single-element {@link TextureProperty} list suitable for
     * passing to a PacketEvents {@link com.github.retrooper.packetevents.protocol.player.UserProfile}.
     */
    public List<TextureProperty> toTextureProperties() {
        if (texture == null || texture.isBlank()) return List.of();
        return List.of(new TextureProperty("textures", texture, signature));
    }
}



