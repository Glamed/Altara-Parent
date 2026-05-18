package games.sparking.altara.game.kit;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.game.impl.Game;
import games.sparking.altara.game.kit.event.KitApplyEvent;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages kit registration, player selection, application, and the
 * kit-selector GUI for a single {@link Game} instance.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Register kits during the game constructor with {@link #registerKit(Kit)}</li>
 *   <li>{@link Game#setState} calls {@link #initialize()} on {@code Loading}, which
 *       registers all kit/perk listeners with Bukkit and activates the selection UI.</li>
 *   <li>During {@code Recruit}, players right-click the §6Kit Selector§r compass to
 *       open a chest menu.  Clicking an icon records their selection.</li>
 *   <li>Call {@link #applyKit(Player)} for each player once the game goes {@code Live}.
 *       Players without an explicit selection receive the first registered kit.</li>
 *   <li>{@link Game#setState} calls {@link #cleanup()} on {@code Dead}, which
 *       unregisters every listener and clears all data.</li>
 * </ol>
 *
 * <h2>Usage from inside a Game</h2>
 * <pre>{@code
 * // In the game constructor:
 * getKitManager().registerKit(new KitJumper(this));
 * getKitManager().registerKit(new KitArmorer(this));
 *
 * // In onStart():
 * for (GamePlayer gp : getPlayers().values()) {
 *     Player p = gp.getPlayer();
 *     if (p != null) getKitManager().applyKit(p);
 * }
 * }</pre>
 */
public class KitManager implements Listener {

    private static final String SELECTOR_TITLE   = "§6Choose your Kit";
    private static final int    SELECTOR_SLOT    = 8;

    private final Game game;

    /** Kits available in this game, in registration order. */
    @Getter private final List<Kit> kits = new ArrayList<>();

    /** Player selection: UUID → chosen Kit. */
    private final Map<UUID, Kit> playerKits = new ConcurrentHashMap<>();

    public KitManager(Game game) {
        this.game = game;
    }

    // =========================================================================
    // Kit registration
    // =========================================================================

    /**
     * Registers a kit for this game.  Must be called before {@link #initialize()}
     * (i.e. during the game's constructor or {@code onLoad()}).
     *
     * @param kit the kit to register
     * @return the same kit (for chaining)
     */
    public Kit registerKit(Kit kit) {
        kits.add(kit);
        return kit;
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Registers all kit/perk listeners and the kit-selector UI listener.
     * Called by {@link Game#setState} when transitioning to {@code Loading}.
     */
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, AltaraPaper.getPaperInstance());

        for (Kit kit : kits) {
            for (Listener listener : kit.getListeners()) {
                Bukkit.getPluginManager().registerEvents(listener, AltaraPaper.getPaperInstance());
            }
            for (Perk perk : kit.getPerks()) {
                if (perk != null) perk.onRegister();
            }
        }
    }

    /**
     * Unregisters all listeners and clears player selections.
     * Called by {@link Game#setState} when transitioning to {@code Dead}.
     */
    public void cleanup() {
        HandlerList.unregisterAll(this);

        for (Kit kit : kits) {
            for (Listener listener : kit.getListeners()) {
                HandlerList.unregisterAll(listener);
            }
            for (Perk perk : kit.getPerks()) {
                if (perk != null) perk.onUnregister();
            }
        }
        playerKits.clear();
    }

    // =========================================================================
    // Kit selection
    // =========================================================================

    /**
     * Records the kit chosen by a player.
     *
     * @param player the selecting player
     * @param kit    the kit they chose (must be registered with this manager)
     */
    public void selectKit(Player player, Kit kit) {
        playerKits.put(player.getUniqueId(), kit);
    }

    /**
     * Returns the kit the player has selected, or the first registered kit if
     * they have not made a selection yet.
     *
     * @param player the player to query
     * @return their (possibly defaulted) kit, or {@code null} if no kits are registered
     */
    public Kit getKit(Player player) {
        Kit selection = playerKits.get(player.getUniqueId());
        if (selection != null) return selection;
        return kits.isEmpty() ? null : kits.get(0);
    }

    /**
     * @return {@code true} if the player has explicitly chosen the given kit
     */
    public boolean hasKit(Player player, Kit kit) {
        return getKit(player) == kit;
    }

    // =========================================================================
    // Kit application
    // =========================================================================

    /**
     * Applies the player's chosen kit (or the default) to them.
     * Fires a {@link KitApplyEvent} which can be cancelled.
     *
     * @param player the player to equip
     */
    public void applyKit(Player player) {
        Kit kit = getKit(player);
        if (kit == null) return;

        KitApplyEvent event = new KitApplyEvent(game, kit, player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        playerKits.put(player.getUniqueId(), kit); // Ensure default is stored.
        kit.apply(player);
    }

    /**
     * Removes kit effects from a player (e.g. when they leave mid-game).
     *
     * @param player the player to clean up
     */
    public void removeKit(Player player) {
        Kit kit = playerKits.get(player.getUniqueId());
        if (kit != null) kit.remove(player);
    }

    // =========================================================================
    // Kit selector UI
    // =========================================================================

    /**
     * Gives the player the Kit Selector compass item.
     * Call this during {@code onRecruit()} / {@code onPlayerJoin()}.
     *
     * @param player the player to receive the compass
     */
    public void giveSelectorItem(Player player) {
        if (kits.isEmpty()) return;
        ItemStack selector = new ItemStack(Material.COMPASS);
        ItemMeta meta = selector.getItemMeta();
        meta.displayName(Component.text("Kit Selector", NamedTextColor.GOLD));
        meta.lore(List.of(Component.text("Right-click to choose your kit", NamedTextColor.GRAY)));
        selector.setItemMeta(meta);
        player.getInventory().setItem(SELECTOR_SLOT, selector);
        player.updateInventory();
    }

    // -------------------------------------------------------------------------
    // Open-selector interact
    // -------------------------------------------------------------------------

    @EventHandler
    public void onSelectorInteract(PlayerInteractEvent event) {
        if (game.isLive()) return;
        Player player = event.getPlayer();
        if (!game.hasPlayer(player)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isSelectorItem(item)) return;

        event.setCancelled(true);
        openKitMenu(player);
    }

    // -------------------------------------------------------------------------
    // Kit menu click
    // -------------------------------------------------------------------------

    @EventHandler
    public void onKitMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(SELECTOR_TITLE)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= kits.size()) return;

        Kit chosen = kits.get(slot);
        selectKit(player, chosen);
        player.closeInventory();

        player.sendMessage("§6Kit selected§7: §e" + chosen.getName());
        for (String line : chosen.getDescription()) {
            player.sendMessage(line);
        }
    }

    // -------------------------------------------------------------------------
    // Build the menu
    // -------------------------------------------------------------------------

    private void openKitMenu(Player player) {
        int rows = Math.max(1, (int) Math.ceil(kits.size() / 9.0));
        Inventory menu = Bukkit.createInventory(null, rows * 9, SELECTOR_TITLE);

        Kit current = playerKits.get(player.getUniqueId());

        for (int i = 0; i < kits.size(); i++) {
            Kit kit = kits.get(i);
            ItemStack icon = new ItemStack(kit.getIcon());
            ItemMeta meta = icon.getItemMeta();
            meta.displayName(Component.text(kit.getName(), NamedTextColor.YELLOW));

            List<Component> lore = new ArrayList<>();
            for (String line : kit.getDescription()) lore.add(Component.text(line));

            // Visible perks
            for (Perk perk : kit.getPerks()) {
                if (perk == null || !perk.isVisible() || perk.getDescription().length == 0) continue;
                lore.add(Component.text("§8── " + perk.getName() + ":", NamedTextColor.DARK_GRAY));
                for (String d : perk.getDescription())
                    lore.add(Component.text("  " + d));
            }

            if (kit == current) {
                lore.add(Component.text("§a§l✔ SELECTED", NamedTextColor.GREEN));
            }

            meta.lore(lore);
            icon.setItemMeta(meta);
            menu.setItem(i, icon);
        }

        player.openInventory(menu);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private boolean isSelectorItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        Component name = item.getItemMeta().displayName();
        return name != null && name.equals(Component.text("Kit Selector", NamedTextColor.GOLD));
    }
}

