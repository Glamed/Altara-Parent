package games.sparking.altara.hologram.leaderboard;

import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.hologram.updating.UpdatingHologram;
import games.sparking.altara.task.Tasks;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A player-specific, paginated leaderboard hologram that can cycle through
 * multiple categories in place.
 *
 * <p>Controls:
 * <ul>
 *   <li><b>Left-click</b>            → previous page</li>
 *   <li><b>Right-click</b>           → next page</li>
 *   <li><b>Shift + Left-click</b>    → previous category</li>
 *   <li><b>Shift + Right-click</b>   → next category</li>
 * </ul>
 *
 * <p>Use {@link Builder} for full control over optional features like click sounds
 * and time-based auto-rotation.
 */
public class LeaderboardHologram {

    // -----------------------------------------------------------------------
    // Rank decorations
    // -----------------------------------------------------------------------
    private static final String[] RANK_COLORS  = { "<gold><bold>", "<gray><bold>", "<red><bold>" };
    private static final String   RANK_PREFIX  = "✦ ";
    private static final String   DEFAULT_COLOR = "<white>";

    private static final String SEPARATOR = "<dark_gray><st>──────────────</st>";
    private static final String NAV_PAGES = "<red>◀ Prev Page  <dark_gray>|  <red>Next Page ▶";
    private static final String NAV_TYPES = "<green>◀ Shift Prev Category  <dark_gray>|  <green>Shift Next Category ▶";

    // -----------------------------------------------------------------------
    // State
    // -----------------------------------------------------------------------

    private final Player viewer;
    private final Location hologramLocation;
    private final UpdatingHologram hologram;
    private final List<LeaderboardCategory> categories;
    private final int pageSize;

    @Getter private int categoryIndex = 0;
    @Getter private int page          = 0;

    // Sounds
    private final Sound  clickSound;
    private final float  clickSoundPitch;

    // Auto-rotate
    private final long  autoRotatePageTicks;
    private final long  autoRotateCategoryTicks;
    private final long  autoRotateBothTicks;
    private final Sound autoRotateSound;
    private final float autoRotateSoundDistance; // in blocks
    private final float autoRotateSoundPitch;

    /** Set to {@code true} by {@link #stop()} to cancel all running auto-rotate tasks. */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /** Millis of hold after a manual click before auto-rotate resumes. 0 = disabled. */
    private final long manualInteractionHoldMs;
    /** Timestamp of the last manual navigation click, in epoch millis. */
    private volatile long lastManualInteractionMs = 0;

    // -----------------------------------------------------------------------
    // Private constructor (use Builder or convenience constructors)
    // -----------------------------------------------------------------------

    private LeaderboardHologram(Builder b) {
        if (b.categories == null || b.categories.isEmpty())
            throw new IllegalArgumentException("At least one category is required.");

        this.viewer                  = b.viewer;
        this.hologramLocation        = b.location.clone();
        this.categories              = List.copyOf(b.categories);
        this.pageSize                = Math.max(1, b.pageSize);
        this.clickSound              = b.clickSound;
        this.clickSoundPitch         = b.clickSoundPitch;
        this.autoRotatePageTicks     = b.autoRotatePageTicks;
        this.autoRotateCategoryTicks = b.autoRotateCategoryTicks;
        this.autoRotateBothTicks     = b.autoRotateBothTicks;
        this.autoRotateSound         = b.autoRotateSound;
        this.autoRotateSoundDistance = b.autoRotateSoundDistance;
        this.autoRotateSoundPitch    = b.autoRotateSoundPitch;
        this.manualInteractionHoldMs = b.manualInteractionHoldMs;

        this.hologram = new HologramBuilder()
                .at(b.location)
                .visibleTo(b.viewer)
                .updating()
                .intervalTicks(40L)
                .lines(this::buildLines)
                .clickHandler((player, holo, lineIndex, clickType) -> {
                    boolean shift = player.isSneaking();
                    if (shift) {
                        if (clickType == HologramClickHandler.ClickType.LEFT_CLICK) prevCategory();
                        else nextCategory();
                    } else {
                        if (clickType == HologramClickHandler.ClickType.LEFT_CLICK) prevPage();
                        else nextPage();
                    }
                    if (clickSound != null)
                        player.playSound(player.getLocation(), clickSound, 1.0f, clickSoundPitch);
                    lastManualInteractionMs = System.currentTimeMillis();
                    holo.update();
                })
                .build();
    }

    // -----------------------------------------------------------------------
    // Convenience constructors (backward-compatible)
    // -----------------------------------------------------------------------

    /** Single-category, no optional features. */
    public LeaderboardHologram(Player viewer, Location location,
                               String title, List<LeaderboardEntry> entries, int pageSize) {
        this(new Builder(viewer, location, title, entries, pageSize));
    }

    /** Multi-category, no optional features. */
    public LeaderboardHologram(Player viewer, Location location,
                               List<LeaderboardCategory> categories, int pageSize) {
        this(new Builder(viewer, location, categories, pageSize));
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    public void spawn() { hologram.spawn(); }

    public void start() {
        hologram.start();

        if (autoRotateBothTicks > 0) {
            // Combined mode: step through every page of a category, then advance to the next.
            // autoRotatePage / autoRotateCategory are intentionally ignored here to avoid conflicts.
            Tasks.runTimer(() -> {
                if (cancelled.get() || isInManualHold()) return;
                int before = page;
                nextPage(); // safe — always clamps via modulo
                // If page wrapped back to 0, we've exhausted all pages → advance category.
                // Also handles single-page categories (before == 0, page still 0 → wrap).
                if (page == 0 && (before != 0 || totalPages() == 1)) {
                    nextCategory(); // resets page to 0 as well
                }
                playAutoRotateSound();
                hologram.update();
            }, autoRotateBothTicks, autoRotateBothTicks);
            return; // don't start individual tasks when using combined mode
        }

        if (autoRotatePageTicks > 0) {
            Tasks.runTimer(() -> {
                if (cancelled.get() || isInManualHold()) return;
                // Clamp page in case the category changed externally and has fewer pages now
                int total = totalPages();
                if (page >= total) page = 0;
                nextPage();
                playAutoRotateSound();
                hologram.update();
            }, autoRotatePageTicks, autoRotatePageTicks);
        }

        if (autoRotateCategoryTicks > 0) {
            Tasks.runTimer(() -> {
                if (cancelled.get() || isInManualHold()) return;
                nextCategory(); // always resets page to 0 — no stale page issues
                playAutoRotateSound();
                hologram.update();
            }, autoRotateCategoryTicks, autoRotateCategoryTicks);
        }
    }

    public void stop() {
        cancelled.set(true);
        hologram.cancel();
        hologram.unregister();
    }

    private void playAutoRotateSound() {
        if (autoRotateSound == null || !viewer.isOnline()) return;
        // Only play if the viewer is within range of the hologram.
        if (!isWithinRange(viewer.getLocation(), autoRotateSoundDistance)) return;
        viewer.playSound(hologramLocation, autoRotateSound, autoRotateSoundDistance / 16.0f, autoRotateSoundPitch);
    }

    /**
     * Returns {@code true} when the viewer is within {@code rangeBlocks} of the hologram.
     * Worlds are compared first to avoid a cross-world {@link Location#distance} call.
     */
    private boolean isWithinRange(Location viewerLoc, float rangeBlocks) {
        if (viewerLoc.getWorld() == null || !viewerLoc.getWorld().equals(hologramLocation.getWorld())) return false;
        return viewerLoc.distanceSquared(hologramLocation) <= (double) rangeBlocks * rangeBlocks;
    }

    /** Returns {@code true} while the player is still within the manual-interaction hold window. */
    private boolean isInManualHold() {
        return manualInteractionHoldMs > 0
                && System.currentTimeMillis() - lastManualInteractionMs < manualInteractionHoldMs;
    }

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
        page = 0;
    }

    public void prevCategory() {
        categoryIndex = (categoryIndex - 1 + categories.size()) % categories.size();
        page = 0;
    }

    public int getTotalPages() { return totalPages(); }

    // -----------------------------------------------------------------------
    // Line builder
    // -----------------------------------------------------------------------

    private List<String> buildLines() {
        LeaderboardCategory    cat           = categories.get(categoryIndex);
        List<LeaderboardEntry> entries       = cat.getEntries();
        int                    totalPages    = totalPages();
        boolean                multiCategory = categories.size() > 1;

        List<String> lines = new ArrayList<>();

        // --- Header ---
        String categoryNav = multiCategory
                ? " <dark_gray>[<dark_purple>" + (categoryIndex + 1) + "<dark_gray>/<dark_purple>" + categories.size() + "<dark_gray>]"
                : "";
        lines.add("<gold><bold>" + cat.getTitle() + " <yellow><bold>Leaderboard" + categoryNav);
        lines.add("<gray>Page <yellow>" + (page + 1) + " <dark_gray>/ <yellow>" + Math.max(1, totalPages));
        lines.add(SEPARATOR);

        // --- Entries ---
        if (entries.isEmpty()) {
            lines.add("<gray>No entries yet.");
        } else {
            int start = page * pageSize;
            int end   = Math.min(start + pageSize, entries.size());
            for (int i = start; i < end; i++) lines.add(formatEntry(entries.get(i)));
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
                ? RANK_COLORS[rank - 1] + RANK_PREFIX + "#" + rank
                : DEFAULT_COLOR + "#" + rank;
        String unitSuffix = entry.getUnit().isEmpty() ? "" : " <gray>" + entry.getUnit();
        return rankStr + " <white>" + entry.getPlayerName() + " <dark_gray>- <yellow>"
                + String.format("%,d", entry.getScore()) + unitSuffix;
    }

    private int totalPages() {
        int size = categories.get(categoryIndex).getEntries().size();
        return Math.max(1, (int) Math.ceil((double) size / pageSize));
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    public static class Builder {

        // Required
        private final Player                    viewer;
        private final Location                  location;
        private final List<LeaderboardCategory> categories;
        private final int                       pageSize;

        // Click sound (optional)
        private Sound clickSound      = null;
        private float clickSoundPitch = 1.0f;

        // Auto-rotate (optional, -1 = disabled)
        private long autoRotatePageTicks     = -1;
        private long autoRotateCategoryTicks = -1;
        private long autoRotateBothTicks     = -1;

        // Auto-rotate sound
        private Sound autoRotateSound         = null;
        private float autoRotateSoundDistance = 3.0f;
        private float autoRotateSoundPitch    = 1.0f;

        // Manual interaction hold (optional)
        private long manualInteractionHoldMs = 0;

        /** Multi-category builder entry-point. */
        public Builder(Player viewer, Location location,
                       List<LeaderboardCategory> categories, int pageSize) {
            this.viewer     = viewer;
            this.location   = location;
            this.categories = new ArrayList<>(categories);
            this.pageSize   = pageSize;
        }

        /** Single-category shortcut. */
        public Builder(Player viewer, Location location,
                       String title, List<LeaderboardEntry> entries, int pageSize) {
            this(viewer, location,
                    List.of(new LeaderboardCategory(title, entries)), pageSize);
        }

        // --- Click sound ---

        /** Play a sound to the viewer every time they click the hologram. */
        public Builder clickSound(Sound sound) {
            this.clickSound = sound;
            return this;
        }

        /** Pitch of the click sound. Default is {@code 1.0}. */
        public Builder clickSoundPitch(float pitch) {
            this.clickSoundPitch = pitch;
            return this;
        }

        // --- Auto-rotate ---

        /** Automatically advance to the next page every {@code ticks} ticks. */
        public Builder autoRotatePage(long ticks) {
            this.autoRotatePageTicks = ticks;
            return this;
        }

        /** Automatically advance to the next category every {@code ticks} ticks. */
        public Builder autoRotateCategory(long ticks) {
            this.autoRotateCategoryTicks = ticks;
            return this;
        }

        /**
         * Cycles through every page of the current category on each tick interval,
         * then automatically advances to the next category once all pages are exhausted.
         * <p>
         * This is the recommended alternative to using {@link #autoRotatePage} and
         * {@link #autoRotateCategory} together — it uses a single timer so page counts
         * changing between categories never cause stale indices or double-fires.
         */
        public Builder autoRotateBoth(long ticks) {
            this.autoRotateBothTicks = ticks;
            return this;
        }

        // --- Auto-rotate sound ---

        /**
         * Sound to play each time the hologram auto-rotates, audible only within
         * {@link #autoRotateSoundDistance} blocks of the hologram.
         */
        public Builder autoRotateSound(Sound sound) {
            this.autoRotateSound = sound;
            return this;
        }

        /**
         * How far (in blocks) the auto-rotate sound can be heard.
         */
        public Builder autoRotateSoundDistance(float blocks) {
            this.autoRotateSoundDistance = blocks;
            return this;
        }

        /** Pitch of the auto-rotate sound. Default is {@code 1.0}. */
        public Builder autoRotateSoundPitch(float pitch) {
            this.autoRotateSoundPitch = pitch;
            return this;
        }

        // --- Manual interaction hold ---

        /**
         * How long (in ticks) to pause auto-rotation after the player manually
         * navigates the hologram. Prevents auto-rotate from immediately overriding
         * a manual page/category change. Default is {@code 0} (no hold).
         */
        public Builder manualInteractionHold(long ticks) {
            this.manualInteractionHoldMs = ticks * 50L;
            return this;
        }

        public LeaderboardHologram build() {
            return new LeaderboardHologram(this);
        }
    }
}
