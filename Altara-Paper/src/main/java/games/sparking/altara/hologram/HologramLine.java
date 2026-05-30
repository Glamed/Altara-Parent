package games.sparking.altara.hologram;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;

@Getter
@Setter
public class HologramLine {

    private String text;
    private ArmorStand entity;
    /** Invisible click-hitbox entity paired with the armor stand. */
    private Interaction interactionEntity;

    public HologramLine(String text) {
        this.text = text;
    }

    /** Sets text and updates the live entity's name tag if it already exists. */
    public void setText(String text) {
        this.text = text;
        if (entity != null && !entity.isDead()) {
            entity.customName(toComponent(text));
            entity.setCustomNameVisible(!text.isEmpty());
        }
    }

    public static Component toComponent(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
}
