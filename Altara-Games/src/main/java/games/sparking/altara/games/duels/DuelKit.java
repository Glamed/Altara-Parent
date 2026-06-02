package games.sparking.altara.games.duels;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Defines what gear a player receives when a duel begins.
 *
 * <p>Games can register custom kits by passing any implementation to
 * {@link DuelGame#registerKit(DuelKit)}.
 */
public interface DuelKit {

    /** Unique identifier used in commands. */
    String getId();

    /** Display name shown to players. */
    String getDisplayName();

    /** Apply this kit to the given player (clear inventory first, then fill). */
    void apply(Player player);

    // -------------------------------------------------------------------------
    // Built-in kits
    // -------------------------------------------------------------------------

    DuelKit CLASSIC = new DuelKit() {

        @Override public String getId() { return "classic"; }
        @Override public String getDisplayName() { return "Classic"; }

        @Override
        public void apply(Player player) {
            player.getInventory().clear();
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);

            // Armor
            player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));

            // Weapons & food
            player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
            player.getInventory().setItem(1, new ItemStack(Material.BOW));
            for (int i = 2; i <= 8; i++) {
                player.getInventory().setItem(i, new ItemStack(Material.COOKED_BEEF, 8));
            }
            player.getInventory().setItem(9, new ItemStack(Material.ARROW, 32));

            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    };

    DuelKit BOXING = new DuelKit() {

        @Override public String getId() { return "boxing"; }
        @Override public String getDisplayName() { return "Boxing"; }

        @Override
        public void apply(Player player) {
            player.getInventory().clear();
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);

            // No armor, no weapons — raw fists
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 3, false, false));

            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    };

    DuelKit SUMO = new DuelKit() {

        @Override public String getId() { return "sumo"; }
        @Override public String getDisplayName() { return "Sumo"; }

        @Override
        public void apply(Player player) {
            player.getInventory().clear();
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);

            // Knockback stick — void-fall counts as a kill
            ItemStack stick = new ItemStack(Material.STICK);
            stick.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
            player.getInventory().setItem(0, stick);

            // Resistance so hits don't deal damage, only knockback
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 4, false, false));

            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    };

    DuelKit ARCHER = new DuelKit() {

        @Override public String getId() { return "archer"; }
        @Override public String getDisplayName() { return "Archer"; }

        @Override
        public void apply(Player player) {
            player.getInventory().clear();
            player.getActivePotionEffects().stream()
                    .map(PotionEffect::getType)
                    .forEach(player::removePotionEffect);

            // Leather armor + bow
            player.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
            player.getInventory().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
            player.getInventory().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
            player.getInventory().setBoots(new ItemStack(Material.LEATHER_BOOTS));

            ItemStack bow = new ItemStack(Material.BOW);
            bow.addUnsafeEnchantment(Enchantment.POWER, 3);
            bow.addUnsafeEnchantment(Enchantment.INFINITY, 1);
            player.getInventory().setItem(0, bow);
            player.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));

            player.getInventory().setItem(1, new ItemStack(Material.IRON_SWORD));
            for (int i = 2; i <= 5; i++) {
                player.getInventory().setItem(i, new ItemStack(Material.COOKED_BEEF, 6));
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, false, false));

            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    };
}

