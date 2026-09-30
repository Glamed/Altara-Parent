package games.sparking.altara.menu;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.menu.fill.IMenuFiller;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Menu {

    @Getter
    private static final Map<UUID, Menu> openedMenus = new ConcurrentHashMap<>();

    /** Buttons currently displayed (including filler), used to resolve clicks. */
    private Map<Integer, Button> buttons = new HashMap<>();
    private Inventory inventory;

    @Setter
    private boolean cancelIncomingUpdates = false;

    @Getter
    @Setter
    private BukkitTask updateRunnable;

    public abstract Component getTitle(Player player);

    public abstract Map<Integer, Button> getButtons(Player player);

    public static Menu getOpenMenu(Player player) {
        return openedMenus.get(player.getUniqueId());
    }

    public int calculateSize(Map<Integer, Button> buttons) {
        int highest = buttons.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        return Math.min(54, (int) (Math.ceil((highest + 1) / 9D) * 9D));
    }

    public void openMenu(Player player) {
        Map<Integer, Button> buttons = new HashMap<>(this.getButtons(player));
        int size = this.getSize() == -1 ? this.calculateSize(buttons) : this.getSize();
        Component title = this.getTitle(player);

        Menu previousMenu = getOpenMenu(player);
        if (previousMenu != null) {
            previousMenu.stopUpdates();
        }

        // Re-use the open inventory when only the contents change (avoids cursor reset).
        Inventory open = player.getOpenInventory().getTopInventory();
        boolean update = previousMenu != null
                && previousMenu.inventory == open
                && open.getSize() == size
                && player.getOpenInventory().title().equals(title);

        Inventory inventory = update ? open : Bukkit.createInventory(null, size, title);
        render(player, inventory, buttons, size);
        this.inventory = inventory;

        if (!update) {
            player.openInventory(inventory);
        }

        openedMenus.put(player.getUniqueId(), this);
        cancelIncomingUpdates = false;
        this.startUpdateTask(player);
        this.onOpen(player);
    }

    private void render(Player player, Inventory inventory, Map<Integer, Button> buttons, int size) {
        IMenuFiller filler = getMenuFiller();
        if (filler != null) {
            filler.fill(this, player, buttons, size);
        }

        ItemStack[] contents = new ItemStack[inventory.getSize()];
        for (Map.Entry<Integer, Button> entry : buttons.entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < contents.length) {
                contents[slot] = entry.getValue().getItem(player);
            }
        }
        inventory.setContents(contents);
        this.buttons = buttons;
    }

    /** The inventory this menu last rendered into. */
    public Inventory getInventory() {
        return inventory;
    }

    /** Buttons currently displayed; used by the click listener. */
    public Map<Integer, Button> getDisplayedButtons() {
        return Collections.unmodifiableMap(buttons);
    }

    public void onOpen(Player player) {
    }

    public void onClose(Player player) {
    }

    public boolean isAutoUpdate() {
        return true;
    }

    public boolean isClickUpdate() {
        return false;
    }

    public int getSize() {
        return -1;
    }

    public FillTemplate getFillTemplate() {
        return null;
    }

    public IMenuFiller getMenuFiller() {
        return getFillTemplate() == null ? null : getFillTemplate().getMenuFiller();
    }

    /** Filler used for empty slots — Altara's light gray background by default. */
    public ItemStack getPlaceholderItem(Player player) {
        return Gui.backgroundPane();
    }

    public boolean cancelLowerClicks() {
        return true;
    }

    public boolean cancelClicks() {
        return true;
    }

    private void startUpdateTask(Player player) {
        if (!this.isAutoUpdate() || this.updateRunnable != null) return;

        // Inventories must only be touched on the main thread.
        this.updateRunnable = Bukkit.getScheduler().runTaskTimer(AltaraPaper.getPlugin(), () -> {
            if (!player.isOnline() || getOpenMenu(player) != this) {
                stopUpdates();
                return;
            }
            updateInventory(player);
        }, 20L, 20L);
    }

    /** Stops the auto-update task and ignores any pending refresh. */
    public void stopUpdates() {
        cancelIncomingUpdates = true;
        if (updateRunnable != null) {
            updateRunnable.cancel();
            updateRunnable = null;
        }
    }

    public void updateInventory(Player player) {
        if (cancelIncomingUpdates || inventory == null) return;

        Map<Integer, Button> buttons = new HashMap<>(getButtons(player));
        int size = getSize() == -1 ? calculateSize(buttons) : getSize();
        if (size != inventory.getSize()) {
            openMenu(player);
            return;
        }
        render(player, inventory, buttons, size);
    }

    public int getSlot(int row, int slot) {
        return 9 * row + slot;
    }
}
