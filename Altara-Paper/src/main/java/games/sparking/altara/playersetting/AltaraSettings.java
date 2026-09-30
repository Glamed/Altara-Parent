package games.sparking.altara.playersetting;

import games.sparking.altara.playersetting.impl.BooleanSetting;
import games.sparking.altara.utils.Statics;
import games.sparking.altara.visibility.VisibilityService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.TimeZone;

public class AltaraSettings implements PlayerSettingProvider {

    public static final BooleanSetting PRIVATE_MESSAGES = new BooleanSetting("altara", "private_messages") {
        @Override public String getDisplayName() { return "Private Messages"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("Other players can currently", "send you private messages.")
                    : List.of("Other players can't send", "you private messages.");
        }
    };

    public static final BooleanSetting MESSAGING_SOUNDS = new BooleanSetting("altara", "messaging_sounds") {
        @Override public String getDisplayName() { return "Message Sounds"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("A sound plays when you", "receive a private message.")
                    : List.of("Private messages arrive", "without a sound.");
        }
    };

    public static final BooleanSetting STAFF_MESSAGES = new BooleanSetting("altara", "staff_messages") {
        @Override public String getDisplayName() { return "Staff Messages"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("You see staff chat and", "staff notifications.")
                    : List.of("Staff chat and staff", "notifications are hidden.");
        }

        @Override
        public boolean canUpdate(Player player) {
            return player.hasPermission("altara.command.togglestaffmessages");
        }
    };

    public static final BooleanSetting STAFF_SHOWN = new BooleanSetting("altara", "staff_shown") {
        @Override public String getDisplayName() { return "Vanished Staff"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("You can see vanished", "staff members.")
                    : List.of("Vanished staff members", "are hidden from you.");
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

    public static final PlayerSetting<TimeZone> TIME_ZONE = new PlayerSetting<TimeZone>("altara", "time_zone") {
        @Override public TimeZone getDefaultValue() { return Statics.TIME_ZONE; }
        @Override public TimeZone parse(String input) { return TimeZone.getTimeZone(input); }
        @Override public ItemStack getIcon(Player player) { return new ItemStack(Material.AIR); }
        @Override public void click(Player player, ClickType clickType) {}
        @Override public String toString(TimeZone value) { return value.toZoneId().getId(); }

        /** Not shown in the settings menu. */
        @Override public boolean canUpdate(Player player) { return false; }
    };

    /** Stored under the legacy "ilib" key so existing preferences are kept. */
    public static final BooleanSetting GLOBAL_CHAT = new BooleanSetting("ilib", "global_chat") {
        @Override public String getDisplayName() { return "Cross-Realm Chat"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("You see chat from", "players on other realms.")
                    : List.of("You only see chat from", "players on your realm.");
        }
    };

    public static final BooleanSetting ALL_CHAT = new BooleanSetting("altara", "all_chat") {
        @Override public String getDisplayName() { return "Chat"; }
        @Override public Boolean getDefaultValue() { return true; }

        @Override
        public List<String> getDescription(boolean enabled) {
            return enabled
                    ? List.of("You see public chat.")
                    : List.of("Public chat is hidden.", "Your messages still send.");
        }
    };

    /** The player's active chat channel; managed by {@code /channel}, not the menu. */
    public static final PlayerSetting<String> ACTIVE_CHANNEL = new PlayerSetting<String>("altara", "active_channel") {
        { storedInProfile = true; }

        @Override public String getDefaultValue() { return "Global"; }
        @Override public String parse(String input) { return input; }
        @Override public ItemStack getIcon(Player player) { return new ItemStack(Material.AIR); }
        @Override public void click(Player player, ClickType clickType) {}
        @Override public boolean canUpdate(Player player) { return false; }
    };

    @Override
    public List<PlayerSetting> getProvidedSettings() {
        return List.of(
                PRIVATE_MESSAGES,
                MESSAGING_SOUNDS,
                ALL_CHAT,
                GLOBAL_CHAT,
                STAFF_MESSAGES,
                STAFF_SHOWN,
                TIME_ZONE,       // hidden, but must be provided to be loaded
                ACTIVE_CHANNEL   // hidden, managed by /channel
        );
    }

    @Override
    public int getPriority() {
        return 1;
    }
}
