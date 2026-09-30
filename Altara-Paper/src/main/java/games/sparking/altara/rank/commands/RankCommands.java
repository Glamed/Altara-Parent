package games.sparking.altara.rank.commands;

import games.sparking.altara.Altara;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Flag;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.connection.RequestHandler;
import games.sparking.altara.connection.RequestResponse;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import games.sparking.altara.rank.menu.RankEditOverviewMenu;
import games.sparking.altara.rank.menu.RankEditingMenu;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.utils.PasteUtils;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Header(value = "Rank", panel = Panel.STAFF)
public class RankCommands {

    public static final RankCommands INSTANCE = new RankCommands();

    // ── Shared helpers (also used by the rank setup prompts) ─────────────────

    /**
     * Creates a rank locally and persists it asynchronously.  Returns {@code null} (after
     * telling the sender) if the name is taken or invalid.
     */
    public static Rank createRank(CommandSender sender, String rankName) {
        if (!rankName.matches("[A-Za-z0-9_-]{1,32}")) {
            sender.sendMessage(CC.error("Invalid name.", "Use up to 32 letters, numbers, dashes or underscores."));
            return null;
        }
        if (Altara.getSharedInstance().getRankService().getRank(rankName) != null) {
            sender.sendMessage(CC.error("Rank exists.", "*" + rankName + "* already exists."));
            return null;
        }

        Rank rank = new Rank(rankName);
        Altara.getSharedInstance().getRankService().cacheRank(rank);

        // The API broadcasts RankCreatePacket to every server once it's stored.
        Tasks.runAsync(() -> {
            RequestResponse response = RequestHandler.post("api/rank", rank.toJson());
            if (!response.wasSuccessful()) {
                sender.sendMessage(CC.error("Unable to create rank.", response.getErrorMessage() + " (" + response.getCode() + ")"));
            }
        });

        sender.sendMessage(CC.success("Rank created.", "*" + rank.getName() + "* is ready to configure."));
        return rank;
    }

    /** True if {@code input} is only MiniMessage style tags, e.g. {@code <red>} or {@code <#ff8800><bold>}. */
    public static boolean isStyleTag(String input) {
        // Every tag must be recognised: unknown tags would survive as literal text.
        return input.startsWith("<") && CC.plain(CC.format(input + "x")).equals("x");
    }

    private static Component rankName(Rank rank) {
        return CC.format(rank.getDisplayName());
    }

    // ── Viewing ──────────────────────────────────────────────────────────────

    @Command(names = {"rank list"}, permission = "rank.command.argument.list", description = "List every rank")
    public boolean rankList(CommandSender sender,
                            @Flag(names = {"priority"}, description = "Sort by queue priority") boolean priority) {
        List<Rank> ranks = priority
                ? Altara.getSharedInstance().getRankService().getRanksSortedPriority()
                : Altara.getSharedInstance().getRankService().getRanksSorted();

        sender.sendMessage(CC.header(Panel.STAFF, "Ranks", priority ? "By priority" : null));
        for (Rank rank : ranks) {
            Component line = Component.text()
                    .append(rankName(rank))
                    .append(Component.text(" " + (priority ? "priority " + rank.getQueuePriority() : "weight " + rank.getWeight()), Theme.TEXT))
                    .append(rank.isDefaultRank() ? Component.text(" (default)", Theme.TEXT) : Component.empty())
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view this rank.", Theme.TEXT)))
                    .clickEvent(ClickEvent.runCommand("/rank info " + rank.getName()))
                    .build();
            sender.sendMessage(CC.item(line));
        }
        sender.sendMessage(CC.footer(Panel.STAFF));
        return true;
    }

    @Command(names = {"rank info"}, permission = "rank.command.argument.info", description = "View a rank's details")
    public boolean rankInfo(CommandSender sender, @Param(name = "rank") Rank rank) {
        sender.sendMessage(CC.header(Panel.STAFF, "Rank", rank.getName()));
        sender.sendMessage(CC.item("Color", CC.format(rank.getColor() + "Example")));
        sender.sendMessage(CC.item("Chat color", CC.format(rank.getChatColor() + "Example")));
        sender.sendMessage(CC.item("Prefix", CC.format(rank.getPrefix() + "Example")));
        sender.sendMessage(CC.item("Suffix", CC.format("<white>Example" + rank.getSuffix())));
        sender.sendMessage(CC.item("Weight", String.valueOf(rank.getWeight())));
        sender.sendMessage(CC.item("Queue priority", String.valueOf(rank.getQueuePriority())));
        sender.sendMessage(CC.item("Default", CC.state(rank.isDefaultRank(), "Yes", "No")));
        sender.sendMessage(CC.item("Disguisable", CC.state(rank.isDisguisable(), "Yes", "No")));
        sender.sendMessage(CC.item("Discord ID", rank.getDiscordId() == null ? "None" : rank.getDiscordId()));
        sender.sendMessage(CC.item("Inherits", rank.getInherits().isEmpty() ? "None"
                : String.join(", ", rank.getInherits().stream().map(Rank::getName).toList())));
        sender.sendMessage(CC.item("Permissions", String.valueOf(rank.getPermissions().size())));
        sender.sendMessage(CC.item("Local permissions", String.valueOf(rank.getLocalPermissions().size())));
        sender.sendMessage(CC.item("Inherited permissions", String.valueOf(rank.getInheritPermissions().size())));
        sender.sendMessage(Component.empty());
        sender.sendMessage(CC.line(Component.text()
                .append(Component.text("Use ", Theme.TEXT))
                .append(Component.text("/rank exportperms " + rank.getName(), Theme.TEXT_STRONG))
                .append(Component.text(" for the full permission list.", Theme.TEXT))));
        sender.sendMessage(CC.footer(Panel.STAFF));
        return true;
    }

    @Command(names = {"rank edit"}, permission = "rank.command.argument.edit", playerOnly = true,
            description = "Open the rank editor")
    public boolean rankEdit(Player sender, @Param(name = "rank", defaultValue = "@menu") String rankName) {
        Profile profile = Altara.getSharedInstance().getProfileService().getProfile(sender);
        if (profile == null) {
            sender.sendMessage(CC.error(Messages.API_ERROR));
            return true;
        }

        if (rankName.equals("@menu")) {
            new RankEditOverviewMenu(profile).openMenu(sender);
            return true;
        }

        Rank rank = Altara.getSharedInstance().getRankService().getRank(rankName);
        if (rank == null) {
            sender.sendMessage(CC.error("Invalid rank.", "*" + rankName + "* doesn't exist."));
            return false;
        }

        new RankEditingMenu(profile, rank).openMenu(sender);
        return true;
    }

    @Command(names = {"rank exportperms", "rank exportpermissions"}, permission = "rank.command.argument.exportperms",
            description = "Upload a rank's permissions to a paste", async = true)
    public boolean rankExportPerms(CommandSender sender,
                                   @Param(name = "rank") Rank rank,
                                   @Param(name = "prefix", defaultValue = "@none") String prefix) {
        StringBuilder builder = new StringBuilder("Permissions of ").append(rank.getName()).append(":\n\nGlobal:\n");
        for (String permission : rank.getPermissions()) {
            if (prefix.equals("@none") || permission.startsWith(prefix)) builder.append(permission).append('\n');
        }

        builder.append("\nLocal on ").append(Altara.getSharedInstance().getLocalServerName()).append(":\n");
        for (String permission : rank.getLocalPermissions()) {
            if (prefix.equals("@none") || permission.startsWith(prefix)) builder.append(permission).append('\n');
        }

        String url = PasteUtils.paste(builder.toString(), false);
        if (url == null) {
            sender.sendMessage(CC.error("Upload failed.", "The permissions couldn't be uploaded. Try again later."));
            return false;
        }

        sender.sendMessage(CC.success(Component.text("Permissions exported."), Component.text(url, Theme.TEXT_STRONG)
                .hoverEvent(HoverEvent.showText(Component.text("Click to open the paste.", Theme.TEXT)))
                .clickEvent(ClickEvent.openUrl(url))));
        return true;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Command(names = {"rank create"}, permission = "rank.command.argument.create", description = "Create a rank")
    public boolean rankCreate(CommandSender sender, @Param(name = "rank") String rankName) {
        return createRank(sender, rankName) != null;
    }

    @Command(names = {"rank delete"}, permission = "rank.command.argument.delete", description = "Delete a rank")
    public boolean rankDelete(CommandSender sender, @Param(name = "rank") Rank rank) {
        if (rank.isDefaultRank()) {
            sender.sendMessage(CC.error("Unable to delete.", "Set another default rank before deleting *" + rank.getName() + "*."));
            return false;
        }

        Altara.getSharedInstance().getRankService().deleteRank(rank.getUuid(),
                error -> sender.sendMessage(CC.error("Unable to delete rank.", error)),
                () -> sender.sendMessage(CC.success("Rank deleted.", "*" + rank.getName() + "* has been removed.")));
        return true;
    }

    @Command(names = {"rank rename"}, permission = "rank.command.argument.rename", description = "Rename a rank")
    public boolean rankRename(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "name") String name) {
        if (!name.matches("[A-Za-z0-9_-]{1,32}")) {
            sender.sendMessage(CC.error("Invalid name.", "Use up to 32 letters, numbers, dashes or underscores."));
            return false;
        }
        Rank existing = Altara.getSharedInstance().getRankService().getRank(name);
        if (existing != null && existing != rank) {
            sender.sendMessage(CC.error("Rank exists.", "*" + name + "* already exists."));
            return false;
        }

        String old = rank.getName();
        rank.setName(name);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Rank renamed.", "*" + old + "* is now *" + name + "*."));
        return true;
    }

    @Command(names = {"rank setdefault"}, permission = "rank.command.argument.setdefault",
            description = "Make a rank the default")
    public boolean rankSetDefault(CommandSender sender, @Param(name = "rank") Rank rank) {
        for (Rank current : Altara.getSharedInstance().getRankService().getRanks()) {
            if (current.isDefaultRank() && current != rank) {
                current.setDefaultRank(false);
                current.save(sender, () -> {});
            }
        }

        rank.setDefaultRank(true);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Default rank updated.", "New players now receive *" + rank.getName() + "*."));
        return true;
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    @Command(names = {"rank addperm", "rank addpermission"}, permission = "rank.command.argument.addperm",
            description = "Add a network-wide permission")
    public boolean rankAddPerm(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "permission") String permission) {
        return addPermission(sender, rank, permission, false);
    }

    @Command(names = {"rank delperm", "rank delpermission", "rank removeperm", "rank removepermission"},
            permission = "rank.command.argument.delperm", description = "Remove a network-wide permission")
    public boolean rankDelPerm(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "permission") String permission) {
        return removePermission(sender, rank, permission, false);
    }

    @Command(names = {"rank addpermlocal", "rank addpermissionlocal"}, permission = "rank.command.argument.addpermlocal",
            description = "Add a permission on this realm only")
    public boolean rankAddPermLocal(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "permission") String permission) {
        return addPermission(sender, rank, permission, true);
    }

    @Command(names = {"rank delpermlocal", "rank delpermissionlocal", "rank removepermlocal", "rank removepermissionlocal"},
            permission = "rank.command.argument.delpermlocal", description = "Remove a permission on this realm only")
    public boolean rankDelPermLocal(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "permission") String permission) {
        return removePermission(sender, rank, permission, true);
    }

    @Command(names = {"rank clearperms", "rank clearpermissions"}, permission = "console",
            description = "Remove all (or prefix-matching) permissions")
    public boolean rankClearPerms(CommandSender sender,
                                  @Param(name = "rank") Rank rank,
                                  @Param(name = "prefix", defaultValue = "@none") String prefix) {
        boolean all = prefix.equals("@none");
        int before = rank.getPermissions().size() + rank.getLocalPermissions().size();
        rank.getPermissions().removeIf(permission -> all || permission.startsWith(prefix));
        rank.getLocalPermissions().removeIf(permission -> all || permission.startsWith(prefix));
        int removed = before - rank.getPermissions().size() - rank.getLocalPermissions().size();

        rank.save(sender, () -> {});
        saveLocal(rank);
        sender.sendMessage(CC.success("Permissions cleared.", "Removed *" + removed + "* " + CC.plural(removed, "permission")
                + " from *" + rank.getName() + "*."));
        return true;
    }

    /** Shared by the command and the rank editor.  Local permissions are stored in permissions.json. */
    public static boolean addPermission(CommandSender sender, Rank rank, String permission, boolean local) {
        String node = permission.toLowerCase();
        List<String> target = local ? rank.getLocalPermissions() : rank.getPermissions();
        if (target.contains(node)) {
            sender.sendMessage(CC.error("Already added.", "*" + rank.getName() + "* already has *" + node + "*."));
            return false;
        }

        target.add(node);
        if (local) saveLocal(rank);
        else rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Permission added.", "*" + rank.getName() + "* now has *" + node + "*"
                + (local ? " on this realm." : ".")));
        return true;
    }

    public static boolean removePermission(CommandSender sender, Rank rank, String permission, boolean local) {
        String node = permission.toLowerCase();
        List<String> target = local ? rank.getLocalPermissions() : rank.getPermissions();
        if (!target.remove(node)) {
            sender.sendMessage(CC.error("Not found.", "*" + rank.getName() + "* doesn't have *" + node + "*."));
            return false;
        }

        if (local) saveLocal(rank);
        else rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Permission removed.", "*" + rank.getName() + "* no longer has *" + node + "*."));
        return true;
    }

    private static void saveLocal(Rank rank) {
        Altara.getSharedInstance().saveLocalPermissions(rank);
        Altara.getSharedInstance().updatePermissionsWithRank(rank);
    }

    // ── Inheritance ──────────────────────────────────────────────────────────

    @Command(names = {"rank inherit"}, permission = "rank.command.argument.inherit",
            description = "Toggle whether a rank inherits another")
    public boolean rankInherit(CommandSender sender, @Param(name = "rank") Rank parent, @Param(name = "inherits") Rank child) {
        return toggleInherit(sender, parent, child);
    }

    public static boolean toggleInherit(CommandSender sender, Rank parent, Rank child) {
        if (parent.inherits(child)) {
            parent.removeInherit(child);
            parent.save(sender, () -> {});
            sender.sendMessage(CC.success("Inheritance removed.", "*" + parent.getName() + "* no longer inherits *" + child.getName() + "*."));
            return true;
        }

        if (parent == child || inheritsTransitively(child, parent)) {
            sender.sendMessage(CC.error("Invalid inheritance.", "That would make *" + parent.getName() + "* inherit itself."));
            return false;
        }

        parent.addInherit(child);
        parent.save(sender, () -> {});
        sender.sendMessage(CC.success("Inheritance added.", "*" + parent.getName() + "* now inherits *" + child.getName() + "*."));
        return true;
    }

    /** Whether {@code rank} already inherits {@code target}, directly or indirectly. */
    private static boolean inheritsTransitively(Rank rank, Rank target) {
        Set<UUID> visited = new HashSet<>();
        List<Rank> queue = new java.util.ArrayList<>(rank.getInherits());
        while (!queue.isEmpty()) {
            Rank next = queue.remove(0);
            if (next.getUuid().equals(target.getUuid())) return true;
            if (visited.add(next.getUuid())) queue.addAll(next.getInherits());
        }
        return false;
    }

    // ── Properties ───────────────────────────────────────────────────────────

    @Command(names = {"rank setweight", "rank weight"}, permission = "rank.command.argument.setweight",
            description = "Set a rank's weight")
    public boolean rankSetWeight(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "weight") int weight) {
        rank.setWeight(weight);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Rank updated.", "*" + rank.getName() + "* now has a weight of *" + weight + "*."));
        return true;
    }

    @Command(names = {"rank setqueuepriority", "rank queuepriority"}, permission = "rank.command.argument.setqueuepriority",
            description = "Set a rank's queue priority")
    public boolean rankSetQueuePriority(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "priority") int priority) {
        rank.setQueuePriority(priority);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Rank updated.", "*" + rank.getName() + "* now has a queue priority of *" + priority + "*."));
        return true;
    }

    @Command(names = {"rank setdisguisable", "rank disguisable"}, permission = "rank.command.argument.setdisguisable",
            description = "Set whether staff can disguise as a rank")
    public boolean rankSetDisguisable(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "disguisable") boolean disguisable) {
        rank.setDisguisable(disguisable);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Rank updated.", "*" + rank.getName() + "* is " + (disguisable ? "now" : "no longer") + " disguisable."));
        return true;
    }

    @Command(names = {"rank setcolor", "rank color"}, permission = "rank.command.argument.setcolor",
            description = "Set a rank's name color")
    public boolean rankSetColor(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "color") String color) {
        if (!isStyleTag(color)) {
            sender.sendMessage(CC.error("Invalid color.", "Use a MiniMessage tag like *<red>* or *<#ff8800>*."));
            return false;
        }
        rank.setColor(color);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success(Component.text("Rank updated."), CC.format(color + "<name>'s name color was changed.",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("name", rank.getName()))));
        return true;
    }

    @Command(names = {"rank setchatcolor", "rank chatcolor"}, permission = "rank.command.argument.setchatcolor",
            description = "Set a rank's chat color")
    public boolean rankSetChatColor(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "color") String color) {
        if (!isStyleTag(color)) {
            sender.sendMessage(CC.error("Invalid color.", "Use a MiniMessage tag like *<white>* or *<gray>*."));
            return false;
        }
        rank.setChatColor(color);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success(Component.text("Rank updated."), CC.format(color + "This is how their chat will look.")));
        return true;
    }

    @Command(names = {"rank setprefix", "rank prefix"}, permission = "rank.command.argument.setprefix",
            description = "Set a rank's prefix")
    public boolean rankSetPrefix(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "prefix", wildcard = true) String prefix) {
        rank.setPrefix(prefix);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success(Component.text("Rank updated."), CC.format(prefix + rank.getColor() + "Example")));
        return true;
    }

    @Command(names = {"rank setsuffix", "rank suffix"}, permission = "rank.command.argument.setsuffix",
            description = "Set a rank's suffix")
    public boolean rankSetSuffix(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "suffix", wildcard = true) String suffix) {
        rank.setSuffix(suffix);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success(Component.text("Rank updated."), CC.format(rank.getColor() + "Example" + suffix)));
        return true;
    }

    @Command(names = {"rank setdiscordid", "rank discordid"}, permission = "rank.command.argument.setdiscordid",
            description = "Link a rank to a Discord role")
    public boolean rankSetDiscordId(CommandSender sender, @Param(name = "rank") Rank rank, @Param(name = "id") String id) {
        rank.setDiscordId(id.equalsIgnoreCase("none") || id.equalsIgnoreCase("null") ? null : id);
        rank.save(sender, () -> {});
        sender.sendMessage(CC.success("Rank updated.", rank.getDiscordId() == null
                ? "*" + rank.getName() + "* is no longer linked to Discord."
                : "*" + rank.getName() + "* is now linked to role *" + rank.getDiscordId() + "*."));
        return true;
    }
}
