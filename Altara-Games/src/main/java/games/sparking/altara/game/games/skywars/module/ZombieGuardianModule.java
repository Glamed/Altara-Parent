package games.sparking.altara.game.games.skywars.module;

import games.sparking.altara.game.module.GameModule;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * <h1>ZombieGuardianModule</h1>
 *
 * <p>Spawns {@link Zombie} guardians at configured island locations when the game
 * goes {@link games.sparking.altara.game.GameState#Live}.
 *
 * <ul>
 *   <li>Zombies are fully armoured in gold and have permanent fire resistance.</li>
 *   <li>They patrol their home island (leash range: 8 blocks) and return to spawn
 *       if pulled away.</li>
 *   <li>On death they drop a random piece of iron armour.</li>
 * </ul>
 *
 * <p>Session isolation is guaranteed because this module only tracks the zombies
 * it spawned (stored in an instance-local map) and all event handlers filter by
 * that set.
 */
public class ZombieGuardianModule extends GameModule {

    // =========================================================================
    // Constants
    // =========================================================================

    private static final double MAX_OFFSET_SQUARED = 64.0; // 8-block leash radius
    private static final double MAX_HEALTH = 15.0;

    private static final PotionEffect FIRE_RESISTANCE =
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false);

    private static final ItemStack[] ARMOR_SET = {
            makeItem(Material.GOLDEN_BOOTS),
            makeItem(Material.GOLDEN_LEGGINGS),
            makeItem(Material.GOLDEN_CHESTPLATE),
            makeItem(Material.GOLDEN_HELMET)
    };

    // =========================================================================
    // State
    // =========================================================================

    /** Maps each zombie to its home spawn location. */
    private final Map<UUID, Location> guardians = new LinkedHashMap<>();

    private final List<Location> spawnLocations;

    // =========================================================================
    // Constructor
    // =========================================================================

    public ZombieGuardianModule(List<Location> spawnLocations) {
        this.spawnLocations = List.copyOf(spawnLocations);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Override
    protected void onEnable() {
        for (Location loc : spawnLocations) {
            spawnGuardian(loc);
        }
    }

    @Override
    protected void onDisable() {
        // Remove all zombies still alive
        for (UUID id : new ArrayList<>(guardians.keySet())) {
            Entity entity = spawnLocations.isEmpty() ? null
                    : spawnLocations.get(0).getWorld().getEntity(id);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        guardians.clear();
    }

    // =========================================================================
    // Spawning
    // =========================================================================

    private void spawnGuardian(Location location) {
        if (location.getWorld() == null) return;

        @SuppressWarnings("deprecation")
        Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        zombie.setRemoveWhenFarAway(false);
        zombie.setCustomName(ChatColor.DARK_RED + "Zombie Guardian");
        zombie.setCustomNameVisible(true);
        zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(MAX_HEALTH);
        zombie.setHealth(MAX_HEALTH);
        zombie.addPotionEffect(FIRE_RESISTANCE);

        EntityEquipment eq = zombie.getEquipment();
        if (eq != null) {
            eq.setArmorContents(ARMOR_SET);
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
        }

        guardians.put(zombie.getUniqueId(), location.clone());
    }

    // =========================================================================
    // Event handlers — scoped to this module's zombies only
    // =========================================================================

    /** Patrol: return to home if the zombie wanders too far. */
    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.FASTER) return;
        if (!getGame().isLive()) return;

        // Remove entries for dead/removed zombies
        guardians.entrySet().removeIf(e -> {
            if (spawnLocations.isEmpty()) return true;
            Entity entity = spawnLocations.get(0).getWorld().getEntity(e.getKey());
            return entity == null || !entity.isValid();
        });

        for (Map.Entry<UUID, Location> entry : guardians.entrySet()) {
            Entity entity = entry.getValue().getWorld().getEntity(entry.getKey());
            if (!(entity instanceof Zombie zombie)) continue;

            double distSq = zombie.getLocation().distanceSquared(entry.getValue());
            if (distSq > MAX_OFFSET_SQUARED) {
                // Pull back to home
                zombie.setTarget(null);
                zombie.teleport(entry.getValue());
            }
        }
    }

    /** Prevent zombies from targeting players outside the leash zone. */
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!guardians.containsKey(zombie.getUniqueId())) return;

        Location home = guardians.get(zombie.getUniqueId());
        if (event.getTarget() == null) return;

        double distSq = event.getTarget().getLocation().distanceSquared(home);
        if (distSq > MAX_OFFSET_SQUARED) {
            event.setCancelled(true);
            zombie.setTarget(null);
        }
    }

    /** Prevent guardians from burning in sunlight. */
    @EventHandler
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Zombie z)) return;
        if (guardians.containsKey(z.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /** On death: drop a random loot item instead of normal drops. */
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie z)) return;
        if (!guardians.containsKey(z.getUniqueId())) return;

        guardians.remove(z.getUniqueId());
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Drop a random iron armour piece
        Material[] drops = {
                Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS, Material.IRON_BOOTS
        };
        event.getDrops().add(new ItemStack(drops[new Random().nextInt(drops.length)]));

        // Announce to game players
        Player killer = z.getKiller();
        if (killer != null && getGame().hasPlayer(killer)) {
            getGame().broadcast(ChatColor.GRAY + killer.getName()
                    + " slew a " + ChatColor.DARK_RED + "Zombie Guardian" + ChatColor.GRAY + "!");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static ItemStack makeItem(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }
}

