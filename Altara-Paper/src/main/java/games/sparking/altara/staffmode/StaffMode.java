package games.sparking.altara.staffmode;

import games.sparking.altara.menu.hotbaritem.HotbarItem;
import games.sparking.altara.staffmode.menu.ExamineMenu;
import games.sparking.altara.staffmode.menu.OnlineStaffMenu;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.ItemBuilder;
import games.sparking.altara.utils.Theme;
import games.sparking.altara.visibility.VisibilityService;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player staff mode state: stashes the player's real inventory/gamemode, drops them
 * into a fixed creative toolkit, and always vanishes them while enabled.  Vanish can also
 * be toggled independently while <em>not</em> in staff mode, matching the old behaviour.
 */
public class StaffMode {

    public static final String PERMISSION = "altara.staff";

    private static final ItemStack INSPECT = new ItemBuilder(Material.BOOK)
            .setDisplayName(Component.text("Examine Inventory", Theme.ERROR)).build();
    private static final ItemStack HANDLE_REPORT = new ItemBuilder(Material.ANVIL)
            .setDisplayName(Component.text("Handle Report", Theme.ERROR)).build();
    private static final ItemStack ONLINE_STAFF_BASE = new ItemBuilder(Material.PLAYER_HEAD)
            .setDisplayName(Component.text("Online Staff", Theme.ERROR)).build();
    private static final ItemStack VANISH_ON = new ItemBuilder(Material.LIME_DYE)
            .setDisplayName(Component.text("Vanished ", Theme.SUCCESS).append(Component.text("(click to unvanish)", Theme.TEXT)))
            .build();
    private static final ItemStack VANISH_OFF = new ItemBuilder(Material.GRAY_DYE)
            .setDisplayName(Component.text("Not Vanished ", Theme.ERROR).append(Component.text("(click to vanish)", Theme.TEXT)))
            .build();

    private static final Map<UUID, StaffMode> INSTANCES = new HashMap<>();
    @Getter
    private static final Set<UUID> vanished = new HashSet<>();

    private final UUID uuid;
    private ItemStack[] inventory = new ItemStack[36];
    private ItemStack[] armor = new ItemStack[4];
    private GameMode previousGameMode = GameMode.SURVIVAL;
    private boolean enabled = false;

    private StaffMode(UUID uuid) {
        this.uuid = uuid;
    }

    public static StaffMode get(Player player) {
        return INSTANCES.computeIfAbsent(player.getUniqueId(), StaffMode::new);
    }

    public static boolean isEnabled(Player player) {
        return get(player).enabled;
    }

    public static boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public static boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    /** Used by the report-follow flow: puts the handler into staff mode + vanish if they aren't already. */
    public static void ensureSpectating(Player player) {
        StaffMode mode = get(player);
        if (!mode.enabled) mode.toggleEnabled(true);
    }

    public void toggleEnabled(boolean silent) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        enabled = !enabled;

        if (enabled) {
            inventory = player.getInventory().getContents();
            armor = player.getInventory().getArmorContents();
            previousGameMode = player.getGameMode();

            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.setGameMode(GameMode.CREATIVE);

            new InspectItem(player);
            new HandleReportItem(player);
            new OnlineStaffItem(player);
            new VanishItem(player);

            player.getInventory().setItem(0, INSPECT);
            player.getInventory().setItem(1, HANDLE_REPORT);
            player.getInventory().setItem(6, onlineStaffIcon(player));
            player.getInventory().setItem(7, vanished.contains(uuid) ? VANISH_ON : VANISH_OFF);

            if (!vanished.contains(uuid)) toggleVanish(true);
        } else {
            HotbarItem.unregisterItem(player, InspectItem.class);
            HotbarItem.unregisterItem(player, HandleReportItem.class);
            HotbarItem.unregisterItem(player, OnlineStaffItem.class);
            HotbarItem.unregisterItem(player, VanishItem.class);

            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.setGameMode(previousGameMode);

            if (vanished.contains(uuid)) toggleVanish(true);
        }

        if (!silent) {
            player.sendMessage(CC.notice("Staff mode " + CC.plain(CC.state(enabled)).toLowerCase() + "."));
        }
    }

    public void toggleVanish(boolean silent) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        boolean nowVanished = !vanished.contains(uuid);
        if (nowVanished) vanished.add(uuid); else vanished.remove(uuid);

        if (enabled) {
            player.getInventory().setItem(7, nowVanished ? VANISH_ON : VANISH_OFF);
        }

        VisibilityService.update(player);

        if (!silent) {
            player.sendMessage(CC.notice("Vanish " + CC.plain(CC.state(nowVanished)).toLowerCase() + "."));
        }
    }

    private static ItemStack onlineStaffIcon(Player player) {
        return new ItemBuilder(ONLINE_STAFF_BASE.clone())
                .setSkullOwner(player)
                .build();
    }

    private static class InspectItem extends HotbarItem {
        private final Player player;
        InspectItem(Player player) { super(player); this.player = player; }
        @Override public ItemStack getItem() { return INSPECT; }
        @Override public void click(Action action, Block block) { }
        @Override public void clickEntity(Entity entity) {
            if (entity instanceof Player target) {
                new ExamineMenu(target).openMenu(player);
            }
        }
    }

    private static class HandleReportItem extends HotbarItem {
        private final Player player;
        HandleReportItem(Player player) { super(player); this.player = player; }
        @Override public ItemStack getItem() { return HANDLE_REPORT; }
        @Override public void click(Action action, Block block) { Bukkit.dispatchCommand(player, "reporthandle"); }
        @Override public void clickEntity(Entity entity) { }
    }

    private static class OnlineStaffItem extends HotbarItem {
        private final Player player;
        OnlineStaffItem(Player player) { super(player); this.player = player; }
        @Override public ItemStack getItem() { return onlineStaffIcon(player); }
        @Override public void click(Action action, Block block) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                new OnlineStaffMenu().openMenu(player);
            }
        }
        @Override public void clickEntity(Entity entity) { }
    }

    private class VanishItem extends HotbarItem {
        VanishItem(Player player) { super(player); }
        @Override public ItemStack getItem() { return vanished.contains(uuid) ? VANISH_ON : VANISH_OFF; }
        @Override public void click(Action action, Block block) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) toggleVanish(false);
        }
        @Override public void clickEntity(Entity entity) { }
    }

}
