package games.sparking.altara.spawn;

import games.sparking.altara.AltaraLobby;
import games.sparking.altara.playersetting.LobbySettings;
import games.sparking.altara.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Egg;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SpawnListener implements Listener {

    public boolean shouldCancel(Player player) {
        if (player.getWorld().equals(AltaraLobby.getLobbyInstance().getLobbyConfig().getSpawnLocation().getLocation().getWorld())) {
                return false;
        }
        return false;
    }

    /**
     * Spawn Teleport
     */
    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Location spawnLocation = AltaraLobby.getLobbyInstance()
                .getLobbyConfig()
                .getSpawnLocation()
                .getLocation()
                .clone();

        boolean shouldFly = LobbySettings.FLY_MODE.canUpdate(player) && LobbySettings.FLY_MODE.get(player);

        if (shouldFly) {
            spawnLocation.add(0, 1, 0);
        }

        player.teleport(spawnLocation);

        if (shouldFly) {
            Tasks.runLater(() -> {
                player.setAllowFlight(true);
                player.setFlying(true);
            }, 20L);
        }
    }

    /**
     * Void world vanish
     */
    private final Map<UUID, Boolean> vanishState = new HashMap<>();

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        String toWorld = player.getWorld().getName();

        if (toWorld.equalsIgnoreCase("VOID")) {
            vanishState.put(player.getUniqueId(), player.hasMetadata("vanished"));

            // Make the player invisible to others
            for (Player other : Bukkit.getOnlinePlayers()) {
                other.hidePlayer(player);
            }
        } else if (fromWorld.equalsIgnoreCase("VOID")) {
            boolean wasVanished = vanishState.getOrDefault(player.getUniqueId(), false);
            vanishState.remove(player.getUniqueId());

            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!wasVanished) {
                    other.showPlayer(player);
                }
            }
        }
    }


    /**
     * Prevent block breaking
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBreak(BlockBreakEvent event)
    {
        event.setCancelled(shouldCancel(event.getPlayer()));
    }

    /**
     * Prevent block placing
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPlace(BlockPlaceEvent event)
    {
        event.setCancelled(shouldCancel(event.getPlayer()));
    }

    /**
     * Prevent item drop
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void itemDrop(PlayerDropItemEvent event)
    {
        event.setCancelled(shouldCancel(event.getPlayer()));
    }

    /**
     * Prevent item pickup
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void itemPickup(PlayerPickupItemEvent event) {
        event.setCancelled(shouldCancel(event.getPlayer()));
    }

    /**
     * Prevent block burning
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockBurn(BlockBurnEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevent blocks catching fire
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockIgnite(BlockIgniteEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevent falling blocks becoming solid
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockEntityChange(EntityChangeBlockEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevents liquid flow
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockPhysics(BlockPhysicsEvent event)
    {
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
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                entity.eject();
                entity.leaveVehicle();
                entity.teleport(AltaraLobby.getLobbyInstance().getLobbyConfig().getSpawnLocation().getLocation());
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
    public void blockSpread(BlockSpreadEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevent leaves decaying
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void leavesDecay(LeavesDecayEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevent block fading
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockFade(BlockFadeEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevent block forming, e.g ice
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void blockForm(BlockFormEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevent inventory interation
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void inventoryClick(InventoryClickEvent event)
    {
        event.setCancelled(shouldCancel((Player) event.getWhoClicked()));
    }

    /**
     * Prevent hunger loss
     */
    @EventHandler
    public void playerFood(FoodLevelChangeEvent event)
    {
        event.setFoodLevel(20);
    }

    /**
     * Prevents emptying buckets
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void playerBucketEmpty(PlayerBucketEmptyEvent event)
    {
        event.setCancelled(true);
    }

    /**
     * Prevents filling buckets
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void playerBucketFill(PlayerBucketFillEvent event)
    {
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
        if (event.getEntity() instanceof Egg) {
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