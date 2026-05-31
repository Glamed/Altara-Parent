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
    private Interaction interactionEntity;

    public HologramLine(String text) {
        this.text = text;
    }

    /** Updates stored text and refreshes the live entity's name tag if present. */
    public void setText(String text) {
        this.text = text;
        if (entity != null && !entity.isDead()) {
            entity.customName(toComponent(text));
            entity.setCustomNameVisible(!text.isEmpty());
        }
    }

    /** Removes both the armor stand and the interaction entity from the world. */
    public void remove() {
        if (entity != null && !entity.isDead()) entity.remove();
        if (interactionEntity != null && !interactionEntity.isDead()) interactionEntity.remove();
    }

    public static Component toComponent(String legacyText) {
        return LegacyComponentSerializer.legacySection().deserialize(legacyText);
    }
}
