package games.sparking.altara.hologram;

import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class Hologram {

    private int id;
    private Location location;
    private double lineSpacing;
    private HologramClickHandler clickHandler = null;

    private final List<HologramLine> spawnedLines = new ArrayList<>();

    protected Hologram(HologramBuilder builder) {
        if (builder.getLocation() == null)
            throw new IllegalArgumentException("Please provide a location using HologramBuilder#at");

        this.location = builder.getLocation();
        this.lineSpacing = builder.getLineSpacing();
        this.id = HologramService.registerHologram(this);
        this.clickHandler = builder.getClickHandler();
    }

    /** Return the lines that should currently be displayed. */
    public abstract List<HologramLine> getLines();

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void spawn() {
        if (!spawnedLines.isEmpty()) destroy();

        List<HologramLine> lines = getLines();
        Location cursor = location.clone().add(0, lines.size() * lineSpacing, 0);

        for (HologramLine line : lines) {
            if (!line.getText().isEmpty()) {
                ArmorStand as = spawnArmorStand(cursor, line.getText());
                line.setEntity(as);
                Interaction interaction = spawnInteraction(cursor);
                line.setInteractionEntity(interaction);
            }
            spawnedLines.add(line);
            cursor.subtract(0, lineSpacing, 0);
        }
    }

    public void destroy() {
        for (HologramLine line : spawnedLines) {
            if (line.getEntity() != null && !line.getEntity().isDead())
                line.getEntity().remove();
            line.setEntity(null);
            if (line.getInteractionEntity() != null && !line.getInteractionEntity().isDead())
                line.getInteractionEntity().remove();
            line.setInteractionEntity(null);
        }
        spawnedLines.clear();
    }

    public void update() {
        List<HologramLine> newLines = getLines();

        // If line count changed, do a full respawn
        if (newLines.size() != spawnedLines.size()) {
            destroy();
            spawn();
            return;
        }

        // Otherwise just update text in-place via HologramLine#setText
        for (int i = 0; i < spawnedLines.size(); i++)
            spawnedLines.get(i).setText(newLines.get(i).getText());
    }

    public void setLocation(Location location) {
        this.location = location;
        destroy();
        spawn();
    }

    // -----------------------------------------------------------------------
    // Click / entity identity
    // -----------------------------------------------------------------------

    public boolean isHologramEntity(Entity entity) {
        for (HologramLine line : spawnedLines) {
            if (line.getEntity() != null && line.getEntity().equals(entity))
                return true;
            if (line.getInteractionEntity() != null && line.getInteractionEntity().equals(entity))
                return true;
        }
        return false;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private ArmorStand spawnArmorStand(Location loc, String text) {
        Location spawnAt = loc.clone().subtract(0, 0.9875, 0);
        return spawnAt.getWorld().spawn(spawnAt, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setInvulnerable(true);
            as.setBasePlate(false);
            as.setSmall(true);
            as.setPersistent(false);
            as.customName(HologramLine.toComponent(text));
            as.setCustomNameVisible(true);
        });
    }

    /**
     * Spawns an Interaction entity at the hologram line's position.
     * This entity has a reliable, configurable hitbox and is the modern
     * equivalent of the 1.7 wither-skull trick for click detection.
     */
    private Interaction spawnInteraction(Location loc) {
        // Centre the hitbox on the nametag: the nametag renders roughly
        // 0.3 blocks above the armor stand body, so we nudge the interaction
        // entity up slightly so clicking on the text fires the event.
        Location spawnAt = loc.clone().subtract(0, 0.25, 0);
        return spawnAt.getWorld().spawn(spawnAt, Interaction.class, i -> {
            i.setInteractionWidth(0.8f);   // wide enough to catch clicks easily
            i.setInteractionHeight(0.55f); // covers the nametag area
            i.setResponsive(true);         // fire PlayerInteractAtEntityEvent
            i.setPersistent(false);
        });
    }
}
