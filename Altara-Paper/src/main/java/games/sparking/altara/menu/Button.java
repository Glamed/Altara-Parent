package games.sparking.altara.menu;

import lombok.Data;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public abstract class Button {

    /** A decorative, non-interactive button showing the given item. */
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
        private float volume;
        private float pitch;

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
