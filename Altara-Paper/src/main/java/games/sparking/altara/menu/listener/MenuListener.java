package games.sparking.altara.menu.listener;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.hotbaritem.HotbarItem;
import games.sparking.altara.utils.timebased.TimeBasedContainer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MenuListener implements Listener {

    private final TimeBasedContainer<UUID> clickCooldown = new TimeBasedContainer<>(500, TimeUnit.MILLISECONDS);
    private final TimeBasedContainer<UUID> entityCooldown = new TimeBasedContainer<>(500, TimeUnit.MILLISECONDS);

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = Menu.getOpenMenu(player);
        if (menu == null || event.getView().getTopInventory() != menu.getInventory()) return;
        if (event.getClickedInventory() == null) return;

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            if (menu.cancelLowerClicks()) event.setCancelled(true);
            return;
        }

        if (menu.cancelClicks()) event.setCancelled(true);

        Button button = menu.getDisplayedButtons().get(event.getSlot());
        if (button == null) return;

        event.setCancelled(button.isCancelClick());
        button.click(player, event.getSlot(), event.getClick(), event.getHotbarButton());

        Button.ButtonClickSound sound = button.getClickSound(player);
        if (sound != null) {
            player.playSound(player.getLocation(), sound.getSound(), sound.getVolume(), sound.getPitch());
        }

        // The click may have opened a different menu or closed this one.
        if (menu.isClickUpdate() && Menu.getOpenMenu(player) == menu) {
            menu.updateInventory(player);
        }
    }

    /** Dragging items across menu slots would otherwise place them into the menu. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Menu menu = Menu.getOpenMenu(player);
        if (menu == null || event.getView().getTopInventory() != menu.getInventory()) return;

        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        Menu menu = Menu.getOpenMenu(player);
        if (menu == null || event.getInventory() != menu.getInventory()) return;

        closeMenu(player, menu);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Menu menu = Menu.getOpenMenu(player);
        if (menu != null) {
            closeMenu(player, menu);
        }
        HotbarItem.HOTBAR_ITEMS.remove(player.getUniqueId());
    }

    private void closeMenu(Player player, Menu menu) {
        menu.stopUpdates();
        Menu.getOpenedMenus().remove(player.getUniqueId(), menu);
        menu.onClose(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;

        ItemStack held = event.getItem();
        if (held == null || held.getType() == Material.AIR) return;

        Player player = event.getPlayer();
        Map<String, HotbarItem> items = HotbarItem.HOTBAR_ITEMS.get(player.getUniqueId());
        if (items == null) return;

        for (HotbarItem item : items.values()) {
            if (!item.getItem().isSimilar(held)) continue;

            event.setCancelled(true);
            if (clickCooldown.contains(player.getUniqueId())) return;

            item.click(event.getAction(), event.getClickedBlock());
            if (item.hasCoolDown()) clickCooldown.add(player.getUniqueId());
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItem(event.getHand());
        if (held.getType() == Material.AIR) return;

        Map<String, HotbarItem> items = HotbarItem.HOTBAR_ITEMS.get(player.getUniqueId());
        if (items == null) return;

        for (HotbarItem item : items.values()) {
            if (!item.getItem().isSimilar(held)) continue;

            event.setCancelled(true);
            if (entityCooldown.contains(player.getUniqueId())) return;

            item.clickEntity(event.getRightClicked());
            if (item.hasCoolDown()) entityCooldown.add(player.getUniqueId());
            return;
        }
    }
}
