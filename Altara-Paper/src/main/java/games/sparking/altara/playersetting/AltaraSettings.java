package games.sparking.altara.playersetting;

import games.sparking.altara.playersetting.impl.BooleanSetting;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Statics;
import games.sparking.altara.visibility.VisibilityService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

public class AltaraSettings implements PlayerSettingProvider {

    
    public static final BooleanSetting PRIVATE_MESSAGES
            = new BooleanSetting("altara", "private_messages") {
        @Override
        public String getDisplayName() {
            return "Private Messages";
        }

        @Override
        public String getEnabledText() {
            return "Receive private messages";
        }

        @Override
        public String getDisabledText() {
            return "Block private messages";
        }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                    CC.YELLOW + "If enabled, you will be able to",
                    CC.YELLOW + "receive private messages."
            );
        }

        @Override
        public Material getMaterial() {
            return Material.OAK_SIGN;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }
    };

    public static final BooleanSetting MESSAGING_SOUNDS
            = new BooleanSetting("altara", "messaging_sounds") {
        @Override
        public String getDisplayName() {
            return "Messaging Sounds";
        }

        @Override
        public String getEnabledText() {
            return "Play a sound on private messages";
        }

        @Override
        public String getDisabledText() {
            return "Disable sound on private messages";
        }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                    CC.YELLOW + "If enabled, you will hear a sound",
                    CC.YELLOW + "when you receive a private message."
            );
        }

        @Override
        public Material getMaterial() {
            return Material.NOTE_BLOCK;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }
    };

    public static final BooleanSetting STAFF_MESSAGES
            = new BooleanSetting("altara", "staff_messages") {
        @Override
        public String getDisplayName() {
            return "Staff Messages";
        }

        @Override
        public String getEnabledText() {
            return "Staff messages are shown";
        }

        @Override
        public String getDisabledText() {
            return "Staff messages are hidden";
        }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                    CC.YELLOW + "If enabled, you will be able to",
                    CC.YELLOW + "see staff only messages."
            );
        }

        @Override
        public Material getMaterial() {
            return Material.ENDER_EYE;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }

        @Override
        public boolean canUpdate(Player player) {
            return player.hasPermission("altara.command.togglestaffmessages");
        }
    };

    public static final BooleanSetting STAFF_SHOWN
            = new BooleanSetting("altara", "staff_shown") {
        @Override
        public String getDisplayName() {
            return "Show Vanished Staff";
        }

        @Override
        public String getEnabledText() {
            return "Vanished staff is visible";
        }

        @Override
        public String getDisabledText() {
            return "Vanished staff is hidden";
        }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                    CC.YELLOW + "If enabled, you will be able to",
                    CC.YELLOW + "see vanished staff members."
            );
        }

        @Override
        public Material getMaterial() {
            return Material.INK_SAC;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }

        @Override
        public boolean canUpdate(Player player) {
            return player.hasPermission("altara.command.hidestaff");
        }

        @Override
        public void click(Player player, ClickType clickType) {
            super.click(player, clickType);
            VisibilityService.update(player);
        }
    };

    public static final PlayerSetting<TimeZone> TIME_ZONE
            = new PlayerSetting<TimeZone>("altara", "time_zone") {
        @Override
        public TimeZone getDefaultValue() {
            return Statics.TIME_ZONE;
        }

        @Override
        public TimeZone parse(String input) {
            return TimeZone.getTimeZone(input);
        }

        @Override
        public ItemStack getIcon(Player player) {
            return new ItemBuilder(Material.AIR).build();
        }

        @Override
        public void click(Player player, ClickType clickType) {
        }

        @Override
        public String toString(TimeZone value) {
            return value.toZoneId().getId();
        }

        @Override
        public boolean canUpdate(Player player) {
            // returning false here will not show this in the settings menu for anyone
            return false;
        }
    };

    public static final BooleanSetting GLOBAL_CHAT = new BooleanSetting("ilib", "global_chat") {
        @Override
        public String getDisplayName() {
            return "Global Chat";
        }

        @Override
        public String getEnabledText() {
            return "Global chat is shown";
        }

        @Override
        public String getDisabledText() {
            return "Global chat is hidden";
        }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                    CC.YELLOW + "If enabled, you will see messages",
                    CC.YELLOW + "sent in the global chat channel."
            );
        }

        @Override
        public Material getMaterial() {
            return Material.WRITABLE_BOOK;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }
    };

    public static final BooleanSetting ALL_CHAT = new BooleanSetting("altara", "all_chat") {
        @Override
        public String getDisplayName() {
            return "All Chat";
        }

        @Override
        public String getEnabledText() {
            return "All chat messages are shown";
        }

        @Override
        public String getDisabledText() {
            return "All chat messages are hidden";
        }

        @Override
        public List<String> getDescription() {
            return Arrays.asList(
                    CC.YELLOW + "If disabled, you will not see",
                    CC.YELLOW + "any chat messages. Your own",
                    CC.YELLOW + "messages still send globally."
            );
        }

        @Override
        public Material getMaterial() {
            return Material.BARRIER;
        }

        @Override
        public Boolean getDefaultValue() {
            return true;
        }
    };

    @Override
    public List<PlayerSetting> getProvidedSettings() {
        return Arrays.asList(
                PRIVATE_MESSAGES,
                MESSAGING_SOUNDS,
                STAFF_MESSAGES,
                STAFF_SHOWN,
                GLOBAL_CHAT,
                ALL_CHAT,
                TIME_ZONE // even tho we don't show this in the menu, it still has to be provided to be loaded
        );
    }

    @Override
    public int getPriority() {
        return 1;
    }
}