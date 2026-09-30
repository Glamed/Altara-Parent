package games.sparking.altara.hologram.command;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.HologramLine;
import games.sparking.altara.hologram.HologramService;
import games.sparking.altara.hologram.leaderboard.LeaderboardCategory;
import games.sparking.altara.hologram.leaderboard.LeaderboardEntry;
import games.sparking.altara.hologram.leaderboard.LeaderboardHologram;
import games.sparking.altara.hologram.statics.StaticHologram;
import games.sparking.altara.hologram.updating.UpdatingHologram;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Hologram management.  Line text is a staff-authored MiniMessage template; type
 * {@code {empty}} for a blank line.
 */
@Header(value = "Hologram", panel = Panel.DEV)
public class HologramCommands {

    private static final String PERMISSION = "altara.holograms";
    private static final String EMPTY_LINE = "{empty}";

    private final HologramService hologramService = AltaraPaper.getPaperInstance().getHologramService();

    private static String lineText(String input) {
        return input.equalsIgnoreCase(EMPTY_LINE) ? "" : input;
    }

    private static String label(StaticHologram hologram) {
        return hologram.getName() != null ? hologram.getName() : "#" + hologram.getId();
    }

    /** Validates a 1-based line index; returns it 0-based, or -1 after telling the sender. */
    private static int lineIndex(CommandSender sender, StaticHologram hologram, int index, boolean allowAppend) {
        int size = hologram.getCurrentLines().size();
        int max = allowAppend ? size + 1 : size;
        if (index < 1 || index > max) {
            sender.sendMessage(CC.error("Invalid line.", "Choose a line between *1* and *" + max + "*."));
            return -1;
        }
        return index - 1;
    }

    private void save(StaticHologram hologram, List<String> lines) {
        hologram.setLines(lines);
        hologramService.save();
    }

    private static List<String> texts(StaticHologram hologram) {
        List<String> lines = new ArrayList<>();
        for (HologramLine line : hologram.getCurrentLines()) lines.add(line.getText());
        return lines;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Command(names = {"hologram create", "holo create"}, permission = PERMISSION,
            description = "Create a hologram where you stand")
    public boolean create(Player sender, @Param(name = "name") String name, @Param(name = "text", wildcard = true) String text) {
        if (name.matches("\\d+")) {
            sender.sendMessage(CC.error("Invalid name.", "Names can't be numbers — those are used for IDs."));
            return false;
        }
        if (hologramService.getHologram(name) != null) {
            sender.sendMessage(CC.error("Name taken.", "A hologram called *" + name + "* already exists."));
            return false;
        }

        StaticHologram hologram = new HologramBuilder()
                .at(sender.getLocation())
                .staticHologram()
                .addLines(lineText(text))
                .build();
        hologram.setName(name);
        hologram.spawn();
        hologramService.register(hologram);
        hologramService.save();
        sender.sendMessage(CC.success("Hologram created.", "*" + name + "* (#" + hologram.getId() + ") is ready."));
        return true;
    }

    @Command(names = {"hologram delete", "hologram remove", "holo delete"}, permission = PERMISSION,
            description = "Delete a hologram")
    public boolean delete(CommandSender sender, @Param(name = "hologram") StaticHologram hologram) {
        hologramService.remove(hologram);
        hologramService.save();
        sender.sendMessage(CC.success("Hologram deleted.", "*" + label(hologram) + "* has been removed."));
        return true;
    }

    @Command(names = {"hologram list", "holo list"}, permission = PERMISSION, description = "List saved holograms")
    public boolean list(CommandSender sender) {
        List<StaticHologram> holograms = hologramService.getSerializedHolograms();

        sender.sendMessage(CC.header(Panel.DEV, "Holograms"));
        if (holograms.isEmpty()) sender.sendMessage(CC.empty("No holograms have been created yet."));

        for (StaticHologram hologram : holograms) {
            Location loc = hologram.getLocation();
            TextComponent.Builder hover = Component.text()
                    .append(Component.text(String.format("%s %.1f, %.1f, %.1f", loc.getWorld().getName(),
                            loc.getX(), loc.getY(), loc.getZ()), Theme.TEXT));
            int i = 0;
            for (HologramLine line : hologram.getCurrentLines()) {
                hover.append(Component.newline())
                        .append(Component.text(++i + ". ", Theme.STRUCTURE))
                        .append(HologramLine.toComponent(line.getText()));
            }
            hover.append(Component.newline()).append(Component.newline())
                    .append(Component.text("Click to teleport to this hologram.", Theme.TEXT));

            sender.sendMessage(CC.item(Component.text()
                    .append(Component.text(label(hologram), Theme.TEXT_STRONG))
                    .append(Component.text(" #" + hologram.getId(), Theme.TEXT))
                    .hoverEvent(HoverEvent.showText(hover.build()))
                    .clickEvent(ClickEvent.runCommand("/hologram tpto " + hologram.getId()))
                    .build()));
        }
        sender.sendMessage(CC.footer(Panel.DEV));
        return true;
    }

    // ── Lines ────────────────────────────────────────────────────────────────

    @Command(names = {"hologram addline", "holo addline"}, permission = PERMISSION, description = "Add a line to the bottom")
    public boolean addLine(CommandSender sender, @Param(name = "hologram") StaticHologram hologram,
                           @Param(name = "text", wildcard = true) String text) {
        List<String> lines = texts(hologram);
        lines.add(lineText(text));
        save(hologram, lines);
        sender.sendMessage(CC.success("Line added.", "*" + label(hologram) + "* now has *" + lines.size() + "* lines."));
        return true;
    }

    @Command(names = {"hologram removeline", "holo removeline"}, permission = PERMISSION, description = "Remove a line")
    public boolean removeLine(CommandSender sender, @Param(name = "hologram") StaticHologram hologram,
                              @Param(name = "line") int index) {
        int position = lineIndex(sender, hologram, index, false);
        if (position < 0) return false;

        List<String> lines = texts(hologram);
        lines.remove(position);
        save(hologram, lines);
        sender.sendMessage(CC.success("Line removed.", "Removed line *" + index + "* from *" + label(hologram) + "*."));
        return true;
    }

    @Command(names = {"hologram setline", "holo setline"}, permission = PERMISSION, description = "Replace a line")
    public boolean setLine(CommandSender sender, @Param(name = "hologram") StaticHologram hologram,
                           @Param(name = "line") int index, @Param(name = "text", wildcard = true) String text) {
        int position = lineIndex(sender, hologram, index, false);
        if (position < 0) return false;

        List<String> lines = texts(hologram);
        lines.set(position, lineText(text));
        save(hologram, lines);
        sender.sendMessage(CC.success("Line updated.", "Line *" + index + "* of *" + label(hologram) + "* was changed."));
        return true;
    }

    @Command(names = {"hologram insertbefore", "holo insertbefore"}, permission = PERMISSION,
            description = "Insert a line above another")
    public boolean insertBefore(CommandSender sender, @Param(name = "hologram") StaticHologram hologram,
                                @Param(name = "line") int index, @Param(name = "text", wildcard = true) String text) {
        return insert(sender, hologram, index, text, 0);
    }

    @Command(names = {"hologram insertafter", "holo insertafter"}, permission = PERMISSION,
            description = "Insert a line below another")
    public boolean insertAfter(CommandSender sender, @Param(name = "hologram") StaticHologram hologram,
                               @Param(name = "line") int index, @Param(name = "text", wildcard = true) String text) {
        return insert(sender, hologram, index, text, 1);
    }

    private boolean insert(CommandSender sender, StaticHologram hologram, int index, String text, int offset) {
        int position = lineIndex(sender, hologram, index, false);
        if (position < 0) return false;

        List<String> lines = texts(hologram);
        lines.add(position + offset, lineText(text));
        save(hologram, lines);
        sender.sendMessage(CC.success("Line inserted.", "Added a line at position *" + (position + offset + 1)
                + "* on *" + label(hologram) + "*."));
        return true;
    }

    // ── Placement ────────────────────────────────────────────────────────────

    @Command(names = {"hologram tphere", "hologram movehere", "holo tphere"}, permission = PERMISSION,
            description = "Move a hologram to you")
    public boolean tphere(Player sender, @Param(name = "hologram") StaticHologram hologram) {
        hologram.setLocation(sender.getLocation());
        hologramService.save();
        sender.sendMessage(CC.success("Hologram moved.", "*" + label(hologram) + "* is now at your location."));
        return true;
    }

    @Command(names = {"hologram tpto", "holo tpto"}, permission = PERMISSION, description = "Teleport to a hologram")
    public boolean tpto(Player sender, @Param(name = "hologram") StaticHologram hologram) {
        sender.teleport(hologram.getLocation());
        sender.sendMessage(CC.info("Teleported to *" + label(hologram) + "*."));
        return true;
    }

    @Command(names = {"hologram setspacing", "holo setspacing"}, permission = PERMISSION,
            description = "Set the gap between lines")
    public boolean setSpacing(CommandSender sender, @Param(name = "hologram") StaticHologram hologram,
                              @Param(name = "spacing") double spacing) {
        if (spacing <= 0 || spacing > 5) {
            sender.sendMessage(CC.error("Invalid spacing.", "Choose a value above *0* and up to *5*."));
            return false;
        }
        hologram.setLineSpacing(spacing);
        hologramService.save();
        sender.sendMessage(CC.success("Spacing updated.", "*" + label(hologram) + "* now uses *" + spacing + "* spacing."));
        return true;
    }

    // ── Previews (only visible to you, not saved) ────────────────────────────

    @Command(names = {"hologram test", "holo test"}, permission = PERMISSION, hidden = true,
            description = "Preview an updating hologram")
    public boolean test(Player sender) {
        UpdatingHologram hologram = new HologramBuilder()
                .at(sender.getLocation())
                .visibleTo(sender)
                .updating()
                .intervalTicks(20L)
                .lines(() -> List.of("<aqua>Players Online", "<white>" + Bukkit.getOnlinePlayers().size()))
                .clickHandler((player, holo, line, clickType) -> player.sendMessage(
                        CC.info("There are *" + Bukkit.getOnlinePlayers().size() + "* players online.")))
                .build();

        hologram.spawn();
        hologram.start();
        sender.sendMessage(CC.info("Preview hologram spawned. Only you can see it, and it isn't saved."));
        return true;
    }

    @Command(names = {"hologram leaderboard", "holo lb"}, permission = PERMISSION, hidden = true,
            description = "Preview a leaderboard hologram")
    public boolean leaderboard(Player sender) {
        List<LeaderboardCategory> categories = List.of(
                new LeaderboardCategory("Kills", List.of(
                        new LeaderboardEntry(1, "Notch", 42_000, "kills"),
                        new LeaderboardEntry(2, "Jeb_", 38_500, "kills"),
                        new LeaderboardEntry(3, "Dinnerbone", 31_200, "kills"),
                        new LeaderboardEntry(4, "Grumm", 28_000, "kills"))),
                new LeaderboardCategory("Wins", List.of(
                        new LeaderboardEntry(1, "Jeb_", 980, "wins"),
                        new LeaderboardEntry(2, "Notch", 870, "wins")))
        );

        LeaderboardHologram lb = new LeaderboardHologram.Builder(sender, sender.getLocation(), categories, 3)
                .clickSound(Sound.UI_BUTTON_CLICK)
                .clickSoundPitch(1.2f)
                .autoRotateBoth(100L)
                .build();

        lb.spawn();
        lb.start();
        sender.sendMessage(CC.info("Preview leaderboard spawned. Only you can see it, and it isn't saved."));
        return true;
    }
}
