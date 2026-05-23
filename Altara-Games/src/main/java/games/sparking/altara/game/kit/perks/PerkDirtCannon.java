package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import games.sparking.altara.task.UpdateType;
import games.sparking.altara.task.events.UpdateEvent;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Dirt Cannon</b>
 *
 * <p>Right-clicking the enchanted Dirt item throws a falling-block projectile that
 * deals 0.5 damage and knockback to enemies it hits.
 * The kit restores dirt every {@value REFILL_INTERVAL_TICKS} ticks up to {@value MAX_DIRT}.
 */
public class PerkDirtCannon extends Perk implements Listener {

    private static final int MAX_DIRT = 4;
    private static final long REFILL_INTERVAL_TICKS = 400L; // 20 seconds
    private static final long THROW_COOLDOWN_MS = 500;
    private static final String ITEM_NAME = "§aThrowable Dirt";

    private final Map<UUID, Long> lastThrow = new ConcurrentHashMap<>();
    /** Tracks accumulated SEC events to implement the refill interval. */
    private int refillSecCounter = 0;

    public PerkDirtCannon() {
        super("Dirt Cannon", new String[]{
                "§7Right-click §aDirt §7to throw it at enemies.",
                "§7Dirt replenishes every §a20s§7 (max §a" + MAX_DIRT + "§7)."
        });
    }

    @Override
    public void apply(Player player) {
        giveDirt(player, MAX_DIRT);
    }

    @Override
    public void remove(Player player) {
        lastThrow.remove(player.getUniqueId());
    }

    @Override
    public void onUnregister() {
        lastThrow.clear();
        refillSecCounter = 0;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.DIRT) return;
        if (!ITEM_NAME.equals(getDisplayName(item))) return;

        long now = System.currentTimeMillis();
        Long last = lastThrow.get(player.getUniqueId());
        if (last != null && now - last < THROW_COOLDOWN_MS) return;
        lastThrow.put(player.getUniqueId(), now);

        event.setCancelled(true);

        // Decrement
        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        // Throw falling block
        FallingBlock fb = player.getWorld().spawnFallingBlock(
                player.getEyeLocation().add(player.getLocation().getDirection()),
                Material.DIRT.createBlockData()
        );
        fb.setDropItem(false);
        fb.setVelocity(player.getLocation().getDirection().multiply(2.0));
        fb.setGravity(true);

        // Track the falling block for damage
        trackDirtBlock(fb, player);
    }

    /** Prevent dirt from landing and creating blocks */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fb) {
            if (fb.getBlockData().getMaterial() == Material.DIRT) {
                event.setCancelled(true);
            }
        }
    }

    /** Refill dirt for alive Earth-kit players every REFILL_INTERVAL_TICKS ticks (~20 s). */
    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != UpdateType.TICK) return;
        if (!getKit().getGame().isLive()) return;
        if (++refillSecCounter < REFILL_INTERVAL_TICKS) return;
        refillSecCounter = 0;
        for (var gp : getKit().getGame().getPlayers().values()) {
            if (!gp.isAlive()) continue;
            Player p = gp.getPlayer();
            if (p == null || !hasPerk(p)) continue;
            int current = countDirt(p);
            if (current < MAX_DIRT) giveDirt(p, MAX_DIRT - current);
        }
    }

    private void trackDirtBlock(FallingBlock fb, Player thrower) {
        // Poll every tick for collision with players
        new org.bukkit.scheduler.BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (!fb.isValid() || fb.isOnGround() || ticks > 100) {
                    fb.remove();
                    cancel();
                    return;
                }
                for (org.bukkit.entity.Entity nearby : fb.getNearbyEntities(0.8, 0.8, 0.8)) {
                    if (!(nearby instanceof Player target)) continue;
                    if (target.equals(thrower)) continue;
                    if (!getKit().getGame().hasPlayer(target)) continue;
                    var tgp = getKit().getGame().getGamePlayer(target).orElse(null);
                    if (tgp == null || !tgp.isAlive()) continue;

                    target.damage(1.0, thrower);
                    Vector kb = fb.getVelocity().normalize().setY(0.5).multiply(1.0);
                    target.setVelocity(target.getVelocity().add(kb));
                    target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation(),
                            10, 0.3, 0.3, 0.3, 0.05, Material.DIRT.createBlockData());

                    fb.remove();
                    cancel();
                    return;
                }
            }
        }.runTaskTimer(games.sparking.altara.AltaraPaper.getPlugin(), 1L, 1L);
    }

    private void giveDirt(Player player, int amount) {
        ItemStack dirt = new ItemStack(Material.DIRT, amount);
        var meta = dirt.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ITEM_NAME);
            dirt.setItemMeta(meta);
        }
        player.getInventory().addItem(dirt);
    }

    private int countDirt(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIRT && ITEM_NAME.equals(getDisplayName(item))) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private static String getDisplayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        var meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : null;
    }
}

