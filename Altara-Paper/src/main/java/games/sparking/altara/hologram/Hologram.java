package games.sparking.altara.hologram;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base hologram.  Every line is spawned <b>per-player</b> — no shared entities,
 * no visibility packet tricks.  When a player joins, call {@link #spawnFor(Player)};
 * when they leave, call {@link #despawnFor(Player)}.
 */
@Getter
public abstract class Hologram {

    private final int id;
    private Location location;
    private final double lineSpacing;
    private final HologramClickHandler clickHandler;

    /**
     * Empty = visible to all players.
     * Non-empty = only these UUIDs receive spawned entities.
     */
    private final Set<UUID> viewers = new LinkedHashSet<>();

    /**
     * Per-player spawned lines.  Each entry is the live HologramLines for one player,
     * complete with entity references.
     */
    private final Map<UUID, List<HologramLine>> playerLines = new ConcurrentHashMap<>();

    protected Hologram(HologramBuilder builder) {
        if (builder.getLocation() == null)
            throw new IllegalArgumentException("Please provide a location using HologramBuilder#at");
        this.location    = builder.getLocation();
        this.lineSpacing = builder.getLineSpacing();
        this.id          = HologramService.registerHologram(this);
        this.clickHandler = builder.getClickHandler();
        this.viewers.addAll(builder.getViewers());
    }

    /** Returns the current set of template lines (text only — no entities attached). */
    public abstract List<HologramLine> getLines();

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Spawns entities for all currently-applicable online players
     * (all online players if public, or only viewers if restricted).
     */
    public void spawn() {
        if (viewers.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(this::spawnFor);
        } else {
            viewers.stream()
                   .map(Bukkit::getPlayer)
                   .filter(Objects::nonNull)
                   .forEach(this::spawnFor);
        }
    }

    /**
     * Spawns (or re-spawns) this hologram's entities specifically for {@code player}.
     * Any previously spawned entities for that player are removed first.
     */
    public void spawnFor(Player player) {
        despawnFor(player); // remove stale entities

        Plugin plugin    = AltaraPaper.getPlugin();
        List<HologramLine> templates = getLines();
        Location cursor  = location.clone().add(0, templates.size() * lineSpacing, 0);
        List<HologramLine> spawned = new ArrayList<>(templates.size());

        for (HologramLine template : templates) {
            String resolved = PlaceholderResolver.resolve(template.getText(), player);
            HologramLine line = new HologramLine(resolved);

            if (resolved != null && !resolved.isEmpty()) {
                ArmorStand as = spawnArmorStand(cursor, resolved, player, plugin);
                Interaction interaction = spawnInteraction(cursor, player, plugin);
                line.setEntity(as);
                line.setInteractionEntity(interaction);
            }

            spawned.add(line);
            cursor.subtract(0, lineSpacing, 0);
        }

        playerLines.put(player.getUniqueId(), spawned);
    }

    /** Removes all entities belonging to {@code player} and clears their line list. */
    public void despawnFor(Player player) {
        despawnFor(player.getUniqueId());
    }

    private void despawnFor(UUID uuid) {
        List<HologramLine> lines = playerLines.remove(uuid);
        if (lines != null) lines.forEach(HologramLine::remove);
    }

    /** Removes all per-player entities (all players). */
    public void destroy() {
        new ArrayList<>(playerLines.keySet()).forEach(this::despawnFor);
    }

    /**
     * Destroys all entities <em>and</em> removes this hologram from the global registry.
     * Call this when the hologram is permanently gone (e.g. a leaderboard being stopped).
     */
    public void unregister() {
        destroy();
        HologramService.unregisterHologram(id);
    }

    /**
     * Refreshes text for every active player.  If the line count changed the hologram
     * is fully re-spawned for that player; otherwise only the name-tag text is updated.
     */
    public void update() {
        List<HologramLine> templates = getLines();

        for (UUID uuid : new ArrayList<>(playerLines.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) { despawnFor(uuid); continue; }

            List<HologramLine> current = playerLines.get(uuid);
            if (current == null) continue;

            if (current.size() != templates.size()) {
                spawnFor(player);   // line count changed — full re-spawn
            } else {
                for (int i = 0; i < current.size(); i++) {
                    String resolved = PlaceholderResolver.resolve(templates.get(i).getText(), player);
                    current.get(i).setText(resolved);
                }
            }
        }
    }

    /** Moves the hologram and re-spawns it at the new location for every current viewer. */
    public void setLocation(Location location) {
        this.location = location;
        destroy();
        spawn();
    }

    // -----------------------------------------------------------------------
    // Viewer management
    // -----------------------------------------------------------------------

    /**
     * Adds players as explicit viewers and spawns their entities immediately
     * if the hologram is restricted by viewer list.
     */
    public void addViewer(Player... players) {
        for (Player player : players) {
            viewers.add(player.getUniqueId());
            spawnFor(player);
        }
    }

    /** Removes a viewer and despawns their entities. */
    public void removeViewer(Player player) {
        viewers.remove(player.getUniqueId());
        despawnFor(player);
    }

    // -----------------------------------------------------------------------
    // Click / entity identity  (now player-scoped — no global entity scan needed)
    // -----------------------------------------------------------------------

    /** Returns {@code true} if {@code entity} belongs to this hologram's lines for {@code player}. */
    public boolean isHologramEntity(Player player, Entity entity) {
        List<HologramLine> lines = playerLines.get(player.getUniqueId());
        if (lines == null) return false;
        for (HologramLine line : lines) {
            if (entity.equals(line.getEntity()) || entity.equals(line.getInteractionEntity()))
                return true;
        }
        return false;
    }

    /** Returns the 0-based line index whose entity/interaction matches, or -1. */
    public int getClickedLineIndex(Player player, Entity entity) {
        List<HologramLine> lines = playerLines.get(player.getUniqueId());
        if (lines == null) return -1;
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            if (entity.equals(line.getEntity()) || entity.equals(line.getInteractionEntity()))
                return i;
        }
        return -1;
    }

    // -----------------------------------------------------------------------
    // Internal entity spawning helpers
    // -----------------------------------------------------------------------

    private ArmorStand spawnArmorStand(Location loc, String text, Player viewer, Plugin plugin) {
        Location spawnAt = loc.clone().subtract(0, 0.9875, 0);
        return spawnAt.getWorld().spawn(spawnAt, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setBasePlate(false);
            as.setSmall(true);
            as.setPersistent(false);
            as.customName(HologramLine.toComponent(text));
            as.setCustomNameVisible(!text.isEmpty());
            as.setVisibleByDefault(false);
            viewer.showEntity(plugin, as);
        });
    }

    private Interaction spawnInteraction(Location loc, Player viewer, Plugin plugin) {
        Location spawnAt = loc.clone().add(0, 0.10, 0);
        return spawnAt.getWorld().spawn(spawnAt, Interaction.class, i -> {
            i.setInteractionWidth(0.8f);
            i.setInteractionHeight(0.30f);
            i.setResponsive(true);
            i.setPersistent(false);
            i.setVisibleByDefault(false);
            viewer.showEntity(plugin, i);
        });
    }
}
