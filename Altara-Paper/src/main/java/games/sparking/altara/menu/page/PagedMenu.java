package games.sparking.altara.menu.page;

import games.sparking.altara.menu.Button;
import games.sparking.altara.menu.Gui;
import games.sparking.altara.menu.Menu;
import games.sparking.altara.menu.fill.FillTemplate;
import games.sparking.altara.utils.Theme;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Six-row paginated menu in the standard Altara frame.
 *
 * <pre>
 * row 0     frame · slot 3 previous · slot 5 next · slot 8 close
 * rows 1-4  content (7 columns inside the frame, 28 per page)
 * row 5     frame · slot 45 back (via {@link #getGlobalButtons})
 * </pre>
 *
 * Implementors return content from {@link #getAllPagesButtons} keyed 0..n-1.
 */
public abstract class PagedMenu extends Menu {

    public static final int PREVIOUS_SLOT = 3;
    public static final int NEXT_SLOT     = 5;

    private static final int CONTENT_ROWS    = 4;
    private static final int CONTENT_COLUMNS = 7;

    @Getter
    private int page = 1;

    public abstract Map<Integer, Button> getAllPagesButtons(Player player);

    /** Breadcrumb pages after "Altara", e.g. {@code {"Grants", "PlayerName"}}. */
    public abstract String[] getBreadcrumb(Player player);

    public final int getPages(Player player) {
        int buttonAmount = getAllPagesButtons(player).size();
        return Math.max(1, (int) Math.ceil(buttonAmount / (double) getMaxItemsPerPage()));
    }

    public final void modPage(Player player, int mod) {
        page = Math.max(1, Math.min(getPages(player), page + mod));
        openMenu(player);
    }

    @Override
    public final Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> all = getAllPagesButtons(player);
        int pages = Math.max(1, (int) Math.ceil(all.size() / (double) getMaxItemsPerPage()));
        page = Math.min(page, pages);

        Map<Integer, Button> buttons = new HashMap<>();
        int minIndex = (page - 1) * getMaxItemsPerPage();
        int maxIndex = page * getMaxItemsPerPage();

        for (Map.Entry<Integer, Button> entry : all.entrySet()) {
            int index = entry.getKey();
            if (index >= minIndex && index < maxIndex) {
                buttons.put(contentSlot(index - minIndex), entry.getValue());
            }
        }

        if (page > 1) buttons.put(PREVIOUS_SLOT, new PageButton(-1, this));
        if (page < pages) buttons.put(NEXT_SLOT, new PageButton(1, this));
        buttons.put(Gui.CLOSE_SLOT, Gui.closeButton());

        Map<Integer, Button> global = getGlobalButtons(player);
        if (global != null) buttons.putAll(global);

        return buttons;
    }

    /** Maps a 0-based position on the page to a slot inside the frame. */
    private static int contentSlot(int position) {
        int row = position / CONTENT_COLUMNS;
        int column = position % CONTENT_COLUMNS;
        return (row + 1) * 9 + column + 1;
    }

    public int getMaxItemsPerPage() {
        return CONTENT_ROWS * CONTENT_COLUMNS;
    }

    /** Extra fixed buttons (e.g. back at slot 45); these override content and frame. */
    public Map<Integer, Button> getGlobalButtons(Player player) {
        return null;
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public FillTemplate getFillTemplate() {
        return FillTemplate.ALTARA;
    }

    @Override
    public Component getTitle(Player player) {
        Component title = Gui.title(getBreadcrumb(player));
        int pages = getPages(player);
        return pages <= 1 ? title
                : title.append(Component.text(" (" + page + "/" + pages + ")", Theme.STRUCTURE));
    }
}
