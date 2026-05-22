package games.sparking.altara.menu;

import games.sparking.altara.utils.ItemBuilder;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class Button {

    public static Button createPlaceholder() {
        return createPlaceholder(" ", Material.BLACK_STAINED_GLASS_PANE);
    }

    public static Button createPlaceholder(Material material) {
        return createPlaceholder(" ", material);
    }

    public static Button createPlaceholder(String displayName, Material material) {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return new ItemBuilder(material)
                        .setDisplayName(displayName)
                        .build();
            }
        };
    }

    public static Button createPlaceholder(ItemStack item) {
        return new Button() {
            @Override
            public ItemStack getItem(Player player) {
                return item;
            }
        };
    }

    public abstract ItemStack getItem(Player player);

    public void click(Player player, int slot, ClickType clickType, int hotbarButton) {
    }

    public ButtonClickSound getClickSound(Player player) {
        return null;
    }

    public boolean isCancelClick() {
        return true;
    }

    @Data
    public static class ButtonClickSound {

        private Sound sound;
        private float volume = 1F;
        private float pitch = 1F;
        public ButtonClickSound(Sound sound) {
            this(sound, 1.0F, 1.0F);
        }
        public ButtonClickSound(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

    }

}
