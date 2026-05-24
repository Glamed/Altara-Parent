package games.sparking.altara.npc.config;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import games.sparking.altara.configuration.StaticConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
public class SerializableEquipment implements StaticConfiguration {
    private String material;
    private int amount = 1;
    private Map<String, Object> nbt = new HashMap<>();
    private String displayName;
    private java.util.List<String> lore;

    public SerializableEquipment(ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() != null) {
            this.material = itemStack.getType().getName().getKey();
            this.amount = itemStack.getAmount();
            // Add NBT and other data extraction if needed
        }
    }

    public SerializableEquipment(String material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public ItemStack toItemStack() {
        if (material == null || material.isEmpty()) {
            return ItemStack.EMPTY;
        }

        try {
            ItemType itemType = ItemTypes.getByName(material);
            if (itemType == null) {
                // Fallback to finding by key
                for (ItemType type : ItemTypes.values()) {
                    if (type.getName().getKey().equalsIgnoreCase(material)) {
                        itemType = type;
                        break;
                    }
                }
            }

            if (itemType != null) {
                ItemStack.Builder builder = ItemStack.builder()
                        .type(itemType)
                        .amount(amount);

                // Add NBT data if present
                if (!nbt.isEmpty()) {
                    // Handle NBT data conversion here if needed
                }

                return builder.build();
            }
        } catch (Exception e) {
            System.err.println("Failed to create ItemStack for material: " + material);
            e.printStackTrace();
        }

        return ItemStack.EMPTY;
    }
}