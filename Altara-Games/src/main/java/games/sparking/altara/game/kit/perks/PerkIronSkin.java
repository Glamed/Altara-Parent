package games.sparking.altara.game.kit.perks;

import games.sparking.altara.game.kit.Perk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * <b>Iron Skin</b>
 *
 * <p>Reduces all incoming damage by a flat amount.
 */
public class PerkIronSkin extends Perk implements Listener {

    private final double reduction;

    public PerkIronSkin(double reduction) {
        super("Iron Skin", new String[]{
                "§7Incoming damage is reduced by §a" + reduction + " hearts§7."
        });
        this.reduction = reduction;
    }

    public PerkIronSkin() {
        this(0.5);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!hasPerk(player)) return;
        double newDamage = Math.max(0, event.getDamage() - reduction);
        event.setDamage(newDamage);
    }
}

