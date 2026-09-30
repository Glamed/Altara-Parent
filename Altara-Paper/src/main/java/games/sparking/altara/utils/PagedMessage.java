package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Paginated chat list in the standard section format:
 * <pre>
 * -----[Title > Page 1/3]-----
 *  - entry
 *  ...
 * Use /command [page] to view more.
 * ----------------------------
 * </pre>
 */
public abstract class PagedMessage<T> {

    private final int perPage;

    protected PagedMessage() {
        this(9);
    }

    protected PagedMessage(int perPage) {
        this.perPage = perPage;
    }

    /** Heading shown in the section header. */
    protected abstract String title();

    /** Command players run to view another page, e.g. {@code "/ignore list"}. */
    protected abstract String pageCommand();

    /** Line for a single entry. */
    protected abstract Component format(T entry);

    /** Colour tier of the panel. */
    protected Panel panel() {
        return Panel.PLAYER;
    }

    /** Shown instead of the list when there are no entries. */
    protected String emptyMessage() {
        return "There's nothing here yet.";
    }

    public void display(CommandSender sender, List<T> entries, int page) {
        if (entries.isEmpty()) {
            sender.sendMessage(CC.header(panel(), title()));
            sender.sendMessage(CC.empty(emptyMessage()));
            sender.sendMessage(CC.footer(panel()));
            return;
        }

        int pages = (entries.size() + perPage - 1) / perPage;
        if (page < 1 || page > pages) {
            sender.sendMessage(CC.error("Invalid page.", "Choose a page between *1* and *" + pages + "*."));
            return;
        }

        sender.sendMessage(CC.header(panel(), title(), pages > 1 ? "Page " + page + "/" + pages : null));
        int end = Math.min(entries.size(), page * perPage);
        for (int i = (page - 1) * perPage; i < end; i++) {
            sender.sendMessage(format(entries.get(i)));
        }
        if (page < pages) {
            sender.sendMessage(CC.line(Component.text()
                    .append(Component.text("Use ", Theme.TEXT))
                    .append(Component.text(pageCommand() + " " + (page + 1), Theme.TEXT_STRONG))
                    .append(Component.text(" to view more.", Theme.TEXT))));
        }
        sender.sendMessage(CC.footer(panel()));
    }
}
