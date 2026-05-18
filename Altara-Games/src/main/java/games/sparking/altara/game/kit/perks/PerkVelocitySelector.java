package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.games.bomblobbers.BombLobbers;
import games.sparking.altara.game.games.bomblobbers.event.BombThrowEvent;
import games.sparking.altara.game.games.bomblobbers.kit.KitPitcher;
import games.sparking.altara.game.kit.Perk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * <b>Velocity Selector</b> – a {@link BombLobbers}-specific perk for {@link KitPitcher}.
 *
 * <ul>
 *   <li>Gives the player a lever item in slot 1 at game start.</li>
 *   <li>Right-click lever → increase throw power (max 3 stacks).</li>
 *   <li>Left-click lever  → decrease throw power (min 1 stack).</li>
 *   <li>Listens to {@link BombThrowEvent} and overrides the TNT velocity to match
 *       the selected power level.</li>
 * </ul>
 */
public class PerkVelocitySelector extends Perk implements Listener {

    private static final int SLOT_LEVER = 1;

    public PerkVelocitySelector() {
        super("Velocity Selector", new String[]{
                "§7Right-click lever §8→ §aincrease throw power",
                "§7Left-click lever  §8→ §cdecrease throw power"
        });
    }

    @Override
    public void apply(Player player) {
        player.getInventory().setItem(SLOT_LEVER, makeLever(2));
    }

    @Override
    public void remove(Player player) {
        ItemStack slot = player.getInventory().getItem(SLOT_LEVER);
        if (slot != null && slot.getType() == Material.LEVER)
            player.getInventory().setItem(SLOT_LEVER, null);
    }

    // =========================================================================
    // Velocity override on throw
    // =========================================================================

    @EventHandler
    public void onBombThrow(BombThrowEvent event) {
        if (!hasPerk(event.getThrower())) return;

        Player thrower = event.getThrower();
        ItemStack lever = thrower.getInventory().getItem(SLOT_LEVER);
        int amount = (lever == null || lever.getType() != Material.LEVER) ? 2 : lever.getAmount();
        amount = Math.max(1, Math.min(3, amount));

        double speed = switch (amount) {
            case 1 -> 1.5;
            case 3 -> 2.5;
            default -> 2.0;
        };

        event.getTnt().setVelocity(
                thrower.getLocation().getDirection()
                        .multiply(speed)
                        .add(new org.bukkit.util.Vector(0, 0.1, 0))
        );
    }

    // =========================================================================
    // Lever interaction – change power level
    // =========================================================================

    @EventHandler
    public void onChangePower(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!hasPerk(player)) return;

        ItemStack lever = player.getInventory().getItem(SLOT_LEVER);
        if (lever == null || lever.getType() != Material.LEVER) return;

        Action action = event.getAction();
        if (action == Action.PHYSICAL) return;
        // Prevent interacting with the main throwable at the same time.
        event.setCancelled(true);

        int amount = lever.getAmount();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            if (amount >= 3) return;
            amount++;
        } else {
            if (amount <= 1) return;
            amount--;
        }

        player.getInventory().setItem(SLOT_LEVER, makeLever(amount));
        player.updateInventory();

        String label = switch (amount) {
            case 1 -> "§cSlow";
            case 3 -> "§aFast";
            default -> "§eNormal";
        };
        player.sendActionBar(Component.text("Throw power: " + label, NamedTextColor.GOLD));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    static ItemStack makeLever(int amount) {
        ItemStack item = new ItemStack(Material.LEVER, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Velocity Selector", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Amount = throw power (1–3)", NamedTextColor.GRAY)
        ));
        item.setItemMeta(meta);
        return item;
    }
}

