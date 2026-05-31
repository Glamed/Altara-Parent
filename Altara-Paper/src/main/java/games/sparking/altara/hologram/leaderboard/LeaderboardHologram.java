package games.sparking.altara.hologram.leaderboard;

import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.hologram.updating.UpdatingHologram;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A player-specific, paginated leaderboard hologram that can cycle through
 * multiple categories in place.
 *
 * <p>Controls:
 * <ul>
 *   <li><b>Left-click</b>       → previous page</li>
 *   <li><b>Right-click</b>      → next page</li>
 *   <li><b>Shift + Left-click</b>  → previous category</li>
 *   <li><b>Shift + Right-click</b> → next category</li>
 * </ul>
 */
public class LeaderboardHologram {

    // -----------------------------------------------------------------------
    // Rank decorations
    // -----------------------------------------------------------------------
    private static final String[] RANK_COLORS   = { "§6§l", "§7§l", "§c§l" };
    private static final String[] RANK_PREFIXES = { "✦ ", "✦ ", "✦ " };
    private static final String   DEFAULT_COLOR  = "§f";

    private static final String SEPARATOR = "§8§m──────────────";
    private static final String NAV_PAGES = "§c◀ Left  §8|  §aRight ▶";
    private static final String NAV_TYPES = "§5↩ Shift-Left  §8|  §dShift-Right ↪";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final UpdatingHologram hologram;
    private final List<LeaderboardCategory> categories;
    private final int pageSize;
    private int categoryIndex = 0;
    private int page          = 0;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Single-category convenience constructor — category cycling is hidden.
     */
    public LeaderboardHologram(Player viewer,
                               Location location,
                               String title,
                               List<LeaderboardEntry> entries,
                               int pageSize) {
        this(viewer, location, Collections.singletonList(
                new LeaderboardCategory(title, entries)), pageSize);
    }

    /**
     * Multi-category constructor.
     *
     * @param viewer     the only player who will see this hologram
     * @param location   where to spawn it
     * @param categories ordered list of categories to cycle through
     * @param pageSize   entries shown per page (recommended 5–10)
     */
    public LeaderboardHologram(Player viewer,
                               Location location,
                               List<LeaderboardCategory> categories,
                               int pageSize) {
        if (categories == null || categories.isEmpty())
            throw new IllegalArgumentException("At least one category is required.");

        this.categories = Collections.unmodifiableList(categories);
        this.pageSize   = Math.max(1, pageSize);

        this.hologram = new HologramBuilder()
                .at(location)
                .visibleTo(viewer)
                .updating()
                .intervalTicks(40L)
                .lines(this::buildLines)
                .clickHandler((player, holo, lineIndex, clickType) -> {
                    boolean shift = player.isSneaking();
                    if (shift) {
                        // Shift-click → cycle category
                        if (clickType == HologramClickHandler.ClickType.LEFT_CLICK) prevCategory();
                        else nextCategory();
                    } else {
                        // Normal click → cycle page
                        if (clickType == HologramClickHandler.ClickType.LEFT_CLICK) prevPage();
                        else nextPage();
                    }
                    holo.update();
                })
                .build();
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void spawn() { hologram.spawn(); }
    public void start() { hologram.start(); }
    public void stop()  { hologram.cancel(); hologram.unregister(); }

    // -----------------------------------------------------------------------
    // Navigation
    // -----------------------------------------------------------------------

    public void nextPage() {
        int total = totalPages();
        page = (total == 0) ? 0 : (page + 1) % total;
    }

    public void prevPage() {
        int total = totalPages();
        page = (total == 0) ? 0 : (page - 1 + total) % total;
    }

    public void nextCategory() {
        categoryIndex = (categoryIndex + 1) % categories.size();
        page = 0; // reset to first page when switching
    }

    public void prevCategory() {
        categoryIndex = (categoryIndex - 1 + categories.size()) % categories.size();
        page = 0;
    }

    public int getPage()          { return page; }
    public int getTotalPages()    { return totalPages(); }
    public int getCategoryIndex() { return categoryIndex; }

    // -----------------------------------------------------------------------
    // Line builder
    // -----------------------------------------------------------------------

    private List<String> buildLines() {
        LeaderboardCategory cat     = categories.get(categoryIndex);
        List<LeaderboardEntry> entries = cat.getEntries();
        int totalPages = totalPages();
        boolean multiCategory = categories.size() > 1;

        List<String> lines = new ArrayList<>();

        // --- Header ---
        String categoryNav = multiCategory
                ? " §8[§5" + (categoryIndex + 1) + "§8/§5" + categories.size() + "§8]"
                : "";
        lines.add("§6§l" + cat.getTitle() + " §e§lLeaderboard" + categoryNav);
        lines.add("§7Page §e" + (page + 1) + " §8/ §e" + Math.max(1, totalPages));
        lines.add(SEPARATOR);

        // --- Entries ---
        if (entries.isEmpty()) {
            lines.add("§7No entries yet.");
        } else {
            int start = page * pageSize;
            int end   = Math.min(start + pageSize, entries.size());
            for (int i = start; i < end; i++) {
                lines.add(formatEntry(entries.get(i)));
            }
        }

        // --- Footer ---
        lines.add(SEPARATOR);
        lines.add(NAV_PAGES);
        if (multiCategory) lines.add(NAV_TYPES);

        return lines;
    }

    private String formatEntry(LeaderboardEntry entry) {
        int rank = entry.getRank();
        String rankStr = (rank <= RANK_COLORS.length)
                ? RANK_COLORS[rank - 1] + RANK_PREFIXES[rank - 1] + "#" + rank
                : DEFAULT_COLOR + "#" + rank;
        String unitSuffix = (entry.getUnit() == null || entry.getUnit().isEmpty())
                ? ""
                : " §7" + entry.getUnit();
        return rankStr + " §f" + entry.getPlayerName() + " §8- §e" + String.format("%,d", entry.getScore()) + unitSuffix;
    }

    private int totalPages() {
        int size = categories.get(categoryIndex).getEntries().size();
        return Math.max(1, (int) Math.ceil((double) size / pageSize));
    }
}
