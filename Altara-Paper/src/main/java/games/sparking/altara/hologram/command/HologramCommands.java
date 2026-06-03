package games.sparking.altara.hologram.command;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.hologram.HologramBuilder;
import games.sparking.altara.hologram.HologramLine;
import games.sparking.altara.hologram.HologramService;
import games.sparking.altara.hologram.statics.StaticHologram;
import games.sparking.altara.hologram.updating.UpdatingHologram;
import games.sparking.altara.utils.CC;
import games.sparking.altara.hologram.leaderboard.LeaderboardCategory;
import games.sparking.altara.hologram.leaderboard.LeaderboardEntry;
import games.sparking.altara.hologram.leaderboard.LeaderboardHologram;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Header(
        primaryColor = "&5",
        secondaryColor = "&8",
        tertiaryColor = "&d",
        header = "Hologram"
)
public class HologramCommands {

    private final HologramService hologramService = AltaraPaper.getPaperInstance().getHologramService();

    @Command(names = {"hologram test", "holo test"},
             permission = "altara.holograms",
             description = "Spawn a temporary updating hologram (only visible to you)",
             playerOnly = true)
    public boolean test(Player sender) {
        UpdatingHologram hologram = new HologramBuilder()
                .at(sender.getLocation())
                .visibleTo(sender)
                .updating()
                .intervalTicks(20L)
                .lines(() -> Arrays.asList(
                        "<yellow><bold>Players Online",
                        "<white>" + Bukkit.getOnlinePlayers().size()
                ))
                .clickHandler((player, holo, line, clickType) -> {
                    if (line == 0) {
                        player.sendMessage(CC.GREEN + "There are currently " + Bukkit.getOnlinePlayers().size() + " players online.");
                    }
                })
                .build();

        hologram.spawn();
        hologram.start();
        sender.sendMessage(CC.GREEN + "Test hologram spawned — only you can see it.");
        return true;
    }

    @Command(names = {"hologram create", "holo create"},
             permission = "altara.holograms",
             description = "Create a new hologram at your location",
             playerOnly = true)
    public boolean create(Player sender,
                          @Param(name = "name") String name,
                          @Param(name = "text", wildcard = true) String text) {
        try {
            Integer.parseInt(name);
            sender.sendMessage(CC.RED + "Hologram names cannot be pure integers.");
            return false;
        } catch (NumberFormatException ignored) { }

        StaticHologram hologram = new HologramBuilder()
                .at(sender.getLocation())
                .staticHologram()
                .addLines(text)
                .build();

        hologram.setName(name);
        hologram.spawn();
        hologramService.register(hologram);
        hologramService.save();
        sender.sendMessage(CC.format("&9Hologram &e#%d &9created.", hologram.getId()));
        return true;
    }

    @Command(names = {"hologram delete", "hologram remove", "holo delete"},
             permission = "altara.holograms",
             description = "Delete a hologram")
    public boolean delete(CommandSender sender, @Param(name = "id") StaticHologram hologram) {
        hologramService.remove(hologram);
        hologramService.save();
        sender.sendMessage(CC.format("&9Deleted hologram &e#%d&9.", hologram.getId()));
        return true;
    }

    @Command(names = {"hologram list", "holo list"},
             permission = "altara.holograms",
             description = "List all saved holograms")
    public boolean list(CommandSender sender) {
        List<StaticHologram> holograms = hologramService.getSerializedHolograms();
        if (holograms.isEmpty()) {
            sender.sendMessage(CC.RED + "No holograms exist.");
            return true;
        }

        for (StaticHologram hologram : holograms) {
            String loc = String.format("[%.1f, %.1f, %.1f]",
                    hologram.getLocation().getX(),
                    hologram.getLocation().getY(),
                    hologram.getLocation().getZ());

            List<String> hover = new ArrayList<>();
            hover.add(CC.GREEN + "Location: " + loc);
            hover.add(CC.YELLOW + "Click to teleport");
            hover.add(" ");

            int i = 0;
            for (HologramLine line : hologram.getCurrentLines())
                hover.add(CC.GRAY + (++i) + ". " + CC.RESET + line.getText());

            new ChatMessage(hologram.getName() + " - #" + hologram.getId())
                    .color(ChatColor.RED.asBungee())
                    .hoverText(String.join("\n", hover))
                    .runCommand("/hologram tpto " + hologram.getId())
                    .send(sender);
        }
        return true;
    }

    @Command(names = {"hologram addline", "holo addline"},
             permission = "altara.holograms",
             description = "Append a line to a hologram")
    public boolean addLine(CommandSender sender,
                           @Param(name = "id") StaticHologram hologram,
                           @Param(name = "text", wildcard = true) String text) {
        text = text.equalsIgnoreCase("{empty}") ? "" : text;
        hologram.addLines(text);
        hologramService.save();
        sender.sendMessage(CC.format("&9Added line to hologram &e#%d&9.", hologram.getId()));
        return true;
    }

    @Command(names = {"hologram removeline", "holo removeline"},
             permission = "altara.holograms",
             description = "Remove a line from a hologram")
    public boolean removeLine(CommandSender sender,
                              @Param(name = "id") StaticHologram hologram,
                              @Param(name = "index") int index) {
        List<HologramLine> lines = new ArrayList<>(hologram.getCurrentLines());
        if (--index < 0 || index >= lines.size()) {
            sender.sendMessage(CC.format("&cInvalid index. (&e1&c-&e%d&c)", lines.size()));
            return false;
        }

        HologramLine removed = lines.remove(index);
        List<String> strings = new ArrayList<>();
        for (HologramLine l : lines) strings.add(l.getText());
        hologram.setLines(strings);
        hologramService.save();
        sender.sendMessage(CC.format("&9Removed '&r%s&9' from hologram &e#%d&9.",
                removed.getText(), hologram.getId()));
        return true;
    }

    @Command(names = {"hologram setline", "holo setline"},
             permission = "altara.holograms",
             description = "Set a specific line on a hologram")
    public boolean setLine(CommandSender sender,
                           @Param(name = "id") StaticHologram hologram,
                           @Param(name = "index") int index,
                           @Param(name = "text", wildcard = true) String text) {
        if (--index < 0 || index >= hologram.getCurrentLines().size()) {
            sender.sendMessage(CC.format("&cInvalid index. (&e1&c-&e%d&c)",
                    hologram.getCurrentLines().size()));
            return false;
        }

        text = text.equalsIgnoreCase("{empty}") ? "" : text;
        hologram.setLine(index, text);
        hologramService.save();
        sender.sendMessage(CC.format("&9Set line &e%d &9on hologram &e#%d&9.", index + 1, hologram.getId()));
        return true;
    }

    @Command(names = {"hologram insertbefore", "holo insertbefore"},
             permission = "altara.holograms",
             description = "Insert a line before an index")
    public boolean insertBefore(CommandSender sender,
                                @Param(name = "id") StaticHologram hologram,
                                @Param(name = "index") int index,
                                @Param(name = "text", wildcard = true) String text) {
        List<HologramLine> lines = new ArrayList<>(hologram.getCurrentLines());
        if (--index < 0 || index >= lines.size()) {
            sender.sendMessage(CC.format("&cInvalid index. (&e1&c-&e%d&c)", lines.size()));
            return false;
        }

        text = text.equalsIgnoreCase("{empty}") ? "" : text;
        lines.add(index, new HologramLine(text));
        List<String> strings = new ArrayList<>();
        for (HologramLine l : lines) strings.add(l.getText());
        hologram.setLines(strings);
        hologramService.save();
        sender.sendMessage(CC.format("&9Inserted line at position &e%d &9on hologram &e#%d&9.",
                index + 1, hologram.getId()));
        return true;
    }

    @Command(names = {"hologram insertafter", "holo insertafter"},
             permission = "altara.holograms",
             description = "Insert a line after an index")
    public boolean insertAfter(CommandSender sender,
                               @Param(name = "id") StaticHologram hologram,
                               @Param(name = "index") int index,
                               @Param(name = "text", wildcard = true) String text) {
        List<HologramLine> lines = new ArrayList<>(hologram.getCurrentLines());
        if (--index < 0 || index >= lines.size()) {
            sender.sendMessage(CC.format("&cInvalid index. (&e1&c-&e%d&c)", lines.size()));
            return false;
        }

        text = text.equalsIgnoreCase("{empty}") ? "" : text;
        lines.add(index + 1, new HologramLine(text));
        List<String> strings = new ArrayList<>();
        for (HologramLine l : lines) strings.add(l.getText());
        hologram.setLines(strings);
        hologramService.save();
        sender.sendMessage(CC.format("&9Inserted line at position &e%d &9on hologram &e#%d&9.",
                index + 2, hologram.getId()));
        return true;
    }

    @Command(names = {"hologram tphere", "hologram movehere", "holo tphere"},
             permission = "altara.holograms",
             description = "Move a hologram to your location",
             playerOnly = true)
    public boolean tphere(Player sender, @Param(name = "id") StaticHologram hologram) {
        hologram.setLocation(sender.getLocation());
        hologramService.save();
        sender.sendMessage(CC.format("&9Moved hologram &e#%d &9to your location.", hologram.getId()));
        return true;
    }

    @Command(names = {"hologram tpto", "holo tpto"},
             permission = "altara.holograms",
             description = "Teleport to a hologram",
             playerOnly = true)
    public boolean tpto(Player sender, @Param(name = "id") StaticHologram hologram) {
        sender.teleport(hologram.getLocation());
        sender.sendMessage(CC.format("&9Teleported to hologram &e#%d&9.", hologram.getId()));
        return true;
    }

    @Command(names = {"hologram setspacing", "holo setspacing"},
             permission = "altara.holograms",
             description = "Set the line spacing of a hologram")
    public boolean setSpacing(CommandSender sender,
                              @Param(name = "id") StaticHologram hologram,
                              @Param(name = "spacing") double spacing) {
        if (spacing <= 0) {
            sender.sendMessage(CC.RED + "Spacing must be greater than 0.");
            return false;
        }
        hologram.setLineSpacing(spacing);
        hologramService.save();
        sender.sendMessage(CC.format("&9Set spacing of hologram &e#%d &9to &e%.2f&9.", hologram.getId(), spacing));
        return true;
    }

    @Command(names = {"hologram leaderboard", "holo lb"},
             permission = "altara.holograms",
             description = "Spawn a demo leaderboard hologram with all features",
             playerOnly = true)
    public boolean leaderboard(Player sender) {
        List<LeaderboardCategory> categories = Arrays.asList(
                new LeaderboardCategory("Kills", Arrays.asList(
                        new LeaderboardEntry(1, "Notch",      42_000, "kills"),
                        new LeaderboardEntry(2, "Jeb_",       38_500, "kills"),
                        new LeaderboardEntry(3, "Dinnerbone", 31_200, "kills"),
                        new LeaderboardEntry(4, "Grumm",      28_000, "kills"),
                        new LeaderboardEntry(5, "Marc",       24_100, "kills"),
                        new LeaderboardEntry(6, "Searge",     19_800, "kills")
                )),
                new LeaderboardCategory("Wins", Arrays.asList(
                        new LeaderboardEntry(1, "Jeb_",   980, "wins"),
                        new LeaderboardEntry(2, "Notch",  870, "wins"),
                        new LeaderboardEntry(3, "Marc",   760, "wins")
                )),
                new LeaderboardCategory("Playtime", Arrays.asList(
                        new LeaderboardEntry(1, "Searge",     1_200, "hrs"),
                        new LeaderboardEntry(2, "Dinnerbone", 1_050, "hrs"),
                        new LeaderboardEntry(3, "Grumm",        940, "hrs"),
                        new LeaderboardEntry(4, "Notch",        880, "hrs")
                ))
        );

        LeaderboardHologram lb = new LeaderboardHologram.Builder(sender, sender.getLocation(), categories, 3)
                // Click sound — heard up to 3 blocks (default), slight pitch-up
                .clickSound(Sound.UI_BUTTON_CLICK)
                .clickSoundPitch(1.2f)
                // Auto-rotate: step through every page then advance category every 5 s (100 ticks)
                .autoRotateBoth(100L)
                // Pling when rotating — default 3-block range
                .autoRotateSound(Sound.BLOCK_NOTE_BLOCK_PLING)
                .autoRotateSoundPitch(1.5f)
                .build();

        lb.spawn();
        lb.start();
        sender.sendMessage(CC.GREEN + "Leaderboard hologram spawned — only you can see it.");
        return true;
    }

}
