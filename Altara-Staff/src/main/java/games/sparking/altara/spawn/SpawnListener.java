package games.sparking.altara.spawn;

import games.sparking.altara.playersetting.LobbySettings;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.visibility.HubVisibilityAdapter;
import games.sparking.altara.visibility.VisibilityService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * Lobby protection.  Players can't build, drop, pick up or move items unless they're in
 * creative mode; the world itself is frozen (no physics, growth, fire, weather or decay).
 */
public class SpawnListener implements Listener {

    private static boolean shouldCancel(Player player) {
        return player.getGameMode() != GameMode.CREATIVE;
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean shouldFly = LobbySettings.FLY_MODE.canUpdate(player) && LobbySettings.FLY_MODE.get(player);

        Location spawn = SpawnCommands.spawnLocation();
        if (spawn != null) {
            player.teleport(shouldFly ? spawn.clone().add(0, 1, 0) : spawn);
        }

        if (shouldFly) {
            Tasks.runLater(() -> {
                if (!player.isOnline()) return;
                player.setAllowFlight(true);
                player.setFlying(true);
            }, 20L);
        }
    }

    /** {@link HubVisibilityAdapter} hides the void world; re-evaluate whenever someone crosses it. */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        VisibilityService.update(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBreak(BlockBreakEvent event) {
        if (shouldCancel(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPlace(BlockPlaceEvent event) {
        if (shouldCancel(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void itemDrop(PlayerDropItemEvent event) {
        if (shouldCancel(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void itemPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && shouldCancel(player)) event.setCancelled(true);
    }

    /**
     * Prevent block burning
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBurn(BlockBurnEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent blocks catching fire
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockIgnite(BlockIgniteEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent falling blocks becoming solid
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockEntityChange(EntityChangeBlockEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevents liquid flow
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPhysics(BlockPhysicsEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevents block growth
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockGrow(BlockGrowEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevents trees growing
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void structureGrow(StructureGrowEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent entities catching fire
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void entityCombust(EntityCombustEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent armor stand manipulation
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void armourStand(PlayerArmorStandManipulateEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent entities from taking damage
     * Void teleport
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void entityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Player) {
            Location spawn = SpawnCommands.spawnLocation();
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID && spawn != null) {
                entity.eject();
                entity.leaveVehicle();
                entity.teleport(spawn);
            }

            event.setCancelled(true);
        }
    }


    /**
     * Prevent creeper explosions
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void explosion(EntityExplodeEvent event) {
        event.blockList().clear();
    }

    /**
     * Prevent block spreading, e.g vines
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockSpread(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent leaves decaying
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void leavesDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent block fading
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockFade(BlockFadeEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent block forming, e.g ice
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockForm(BlockFormEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevent inventory interaction
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void inventoryClick(InventoryClickEvent event) {
        // Menus cancel their own clicks; this only guards the player's own inventory.
        if (event.getWhoClicked() instanceof Player player && event.getClickedInventory() == player.getInventory()
                && shouldCancel(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent hunger loss
     */
    @EventHandler
    public void playerFood(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
    }

    /**
     * Prevents emptying buckets
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void playerBucketEmpty(PlayerBucketEmptyEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevents filling buckets
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void playerBucketFill(PlayerBucketFillEvent event) {
        event.setCancelled(true);
    }

    /**
     * Prevents rain/storms in the hub.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void weatherChange(WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void eggSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.EGG) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent the crafting of items
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void itemCraft(CraftItemEvent event) {
        event.setCancelled(true);
    }

}