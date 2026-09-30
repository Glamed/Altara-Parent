package games.sparking.altara.utils;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * Friendly names and short aliases for enchantments (e.g. {@code prot}, {@code sharp}).
 * Anything not listed here can still be referenced by its registry key.
 */
@Getter
public enum EnchantmentWrapper {

    PROTECTION("protection", "Protection", "p", "prot"),
    FIRE_PROTECTION("fire_protection", "Fire Protection", "fp", "fprot", "fireprot"),
    FEATHER_FALLING("feather_falling", "Feather Falling", "ff", "featherf", "ffalling"),
    BLAST_PROTECTION("blast_protection", "Blast Protection", "bp", "bprot", "blastprot"),
    PROJECTILE_PROTECTION("projectile_protection", "Projectile Protection", "pp", "projprot"),
    THORNS("thorns", "Thorns"),
    UNBREAKING("unbreaking", "Unbreaking", "unb", "dura"),
    SHARPNESS("sharpness", "Sharpness", "s", "sharp"),
    SMITE("smite", "Smite"),
    BANE_OF_ARTHROPODS("bane_of_arthropods", "Bane of Arthropods", "bane", "boa"),
    KNOCKBACK("knockback", "Knockback", "k", "kb"),
    FIRE_ASPECT("fire_aspect", "Fire Aspect", "fire", "fa"),
    RESPIRATION("respiration", "Respiration", "breathing", "resp"),
    AQUA_AFFINITY("aqua_affinity", "Aqua Affinity", "aa"),
    LOOTING("looting", "Looting", "loot"),
    FORTUNE("fortune", "Fortune", "fort"),
    EFFICIENCY("efficiency", "Efficiency", "e", "eff"),
    SILK_TOUCH("silk_touch", "Silk Touch", "silk"),
    POWER("power", "Power"),
    PUNCH("punch", "Punch"),
    FLAME("flame", "Flame"),
    INFINITY("infinity", "Infinity", "inf"),
    LUCK_OF_THE_SEA("luck_of_the_sea", "Luck of the Sea", "luck", "los"),
    LURE("lure", "Lure"),
    DEPTH_STRIDER("depth_strider", "Depth Strider", "depth"),
    MENDING("mending", "Mending");

    private final String key;
    private final String fancyName;
    private final String[] aliases;

    EnchantmentWrapper(String key, String fancyName, String... aliases) {
        this.key = key;
        this.fancyName = fancyName;
        this.aliases = aliases;
    }

    public Enchantment toBukkitEnchant() {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    /** Resolves an alias, friendly name or registry key to an enchantment, or {@code null}. */
    public static Enchantment resolve(String input) {
        String normalized = input.toLowerCase().replace(' ', '_');
        for (EnchantmentWrapper value : values()) {
            if (value.key.equals(normalized) || value.key.replace("_", "").equals(normalized)
                    || value.fancyName.equalsIgnoreCase(input)) {
                return value.toBukkitEnchant();
            }
            for (String alias : value.aliases) {
                if (alias.equalsIgnoreCase(input)) return value.toBukkitEnchant();
            }
        }

        NamespacedKey key = NamespacedKey.fromString(normalized);
        return key == null ? null : Registry.ENCHANTMENT.get(key);
    }

    /** Every enchantment key, for tab completion. */
    public static List<String> completions() {
        List<String> completions = new ArrayList<>();
        Registry.ENCHANTMENT.forEach(enchantment -> completions.add(enchantment.getKey().getKey()));
        return completions;
    }
}
