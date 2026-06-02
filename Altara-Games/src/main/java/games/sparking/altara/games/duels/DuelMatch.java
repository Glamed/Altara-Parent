package games.sparking.altara.games.duels;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.UUID;

/**
 * Tracks a single active duel between two players.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>{@link #initialize(Player, Player)} — save inventories, teleport, apply kit, announce start</li>
 *   <li>Modules handle events and call {@link DuelGame#endMatch} when a player's health hits zero or they quit</li>
 *   <li>{@link #restore(Player)} — called by {@link DuelGame#endMatch} to give back the saved inventory</li>
 * </ol>
 */
@Getter
public class DuelMatch {

    private final UUID player1;
    private final UUID player2;
    private final DuelArena arena;
    private final DuelKit kit;
    private final long startTime;

    // Saved pre-duel inventories (restored on match end)
    private ItemStack[] player1Contents;
    private ItemStack[] player1Armor;
    private ItemStack[] player2Contents;
    private ItemStack[] player2Armor;

    // Combo tracking (hits without being hit back)
    private int player1Combo;
    private int player2Combo;

    public DuelMatch(UUID player1, UUID player2, DuelArena arena, DuelKit kit) {
        this.player1 = player1;
        this.player2 = player2;
        this.arena = arena;
        this.kit = kit;
        this.startTime = System.currentTimeMillis();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Saves both players' current inventories, teleports them to arena spawns,
     * and applies the kit.  Must be called from the main thread.
     */
    public void initialize(Player p1, Player p2) {
        // Save inventories
        player1Contents = p1.getInventory().getContents().clone();
        player1Armor = p1.getInventory().getArmorContents().clone();
        player2Contents = p2.getInventory().getContents().clone();
        player2Armor = p2.getInventory().getArmorContents().clone();

        // Clear potion effects
        for (PotionEffect effect : p1.getActivePotionEffects())
            p1.removePotionEffect(effect.getType());
        for (PotionEffect effect : p2.getActivePotionEffects())
            p2.removePotionEffect(effect.getType());

        // Teleport to arena spawns
        p1.teleport(arena.getSpawn1());
        p2.teleport(arena.getSpawn2());

        // Apply kit
        kit.apply(p1);
        kit.apply(p2);

        // Announce
        p1.sendMessage("");
        p1.sendMessage("§6§lDUEL §7» §eYou are now fighting §c" + p2.getName()
                + " §7in §a" + kit.getDisplayName() + "§7!");
        p1.sendMessage("§7Arena: §a" + arena.getName());
        p1.sendMessage("");

        p2.sendMessage("");
        p2.sendMessage("§6§lDUEL §7» §eYou are now fighting §c" + p1.getName()
                + " §7in §a" + kit.getDisplayName() + "§7!");
        p2.sendMessage("§7Arena: §a" + arena.getName());
        p2.sendMessage("");
    }

    /**
     * Restores a player's pre-duel inventory.
     * Heals them and clears any potion effects applied by the kit.
     */
    public void restore(Player player) {
        // Clear kit effects
        for (PotionEffect effect : player.getActivePotionEffects())
            player.removePotionEffect(effect.getType());

        if (player.getUniqueId().equals(player1)) {
            player.getInventory().setContents(player1Contents);
            player.getInventory().setArmorContents(player1Armor);
        } else {
            player.getInventory().setContents(player2Contents);
            player.getInventory().setArmorContents(player2Armor);
        }

        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.updateInventory();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns the UUID of the opponent of the given player. */
    public UUID getOpponent(UUID playerUuid) {
        return playerUuid.equals(player1) ? player2 : player1;
    }

    /** Returns the live {@link Player} of the opponent, or null if offline. */
    public Player getOpponentOnline(UUID playerUuid) {
        return Bukkit.getPlayer(getOpponent(playerUuid));
    }

    // -------------------------------------------------------------------------
    // Combo tracking
    // -------------------------------------------------------------------------

    public int incrementCombo(UUID playerUuid) {
        if (playerUuid.equals(player1)) return ++player1Combo;
        return ++player2Combo;
    }

    public void resetCombo(UUID playerUuid) {
        if (playerUuid.equals(player1)) player1Combo = 0;
        else player2Combo = 0;
    }

    public int getCombo(UUID playerUuid) {
        return playerUuid.equals(player1) ? player1Combo : player2Combo;
    }

    /** Duration of this match in seconds. */
    public long getDurationSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000L;
    }
}

