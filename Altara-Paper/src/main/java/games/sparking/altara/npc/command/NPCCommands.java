package games.sparking.altara.npc.command;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.annotation.Command;
import games.sparking.altara.command.annotation.Flag;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.annotation.Param;
import games.sparking.altara.npc.NPC;
import games.sparking.altara.npc.NPCBuilder;
import games.sparking.altara.npc.NPCService;
import games.sparking.altara.npc.equipment.EquipmentSlot;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * NPC management.  Display names are MiniMessage; use {@code \n} for multiple nametag
 * lines.  Commands may contain {@code %player%}, replaced with the clicker's name.
 */
@Header(value = "NPC", panel = Panel.DEV)
public class NPCCommands {

    private static final String PERMISSION = "altara.npcs";

    private NPCService npcService() {
        return AltaraPaper.getPaperInstance().getNpcService();
    }

    private static String label(NPC npc) {
        return npc.getName() != null ? npc.getName() : "#" + npc.getId();
    }

    @Command(names = {"npc create"}, permission = PERMISSION, description = "Create an NPC where you stand")
    public boolean create(Player sender, @Param(name = "name") String name) {
        if (name.matches("\\d+")) {
            sender.sendMessage(CC.error("Invalid name.", "Names can't be numbers — those are used for IDs."));
            return false;
        }
        if (npcService().getNpc(name) != null) {
            sender.sendMessage(CC.error("Name taken.", "An NPC called *" + name + "* already exists."));
            return false;
        }

        NPC npc = new NPCBuilder().at(sender.getLocation()).buildAndSpawn();
        npc.setName(name);
        npc.spawn();
        npcService().register(npc);
        npcService().save();
        sender.sendMessage(CC.success("NPC created.", "*" + name + "* (#" + npc.getId() + ") is ready."));
        return true;
    }

    @Command(names = {"npc delete", "npc remove"}, permission = PERMISSION, description = "Delete an NPC")
    public boolean delete(CommandSender sender, @Param(name = "npc") NPC npc) {
        npcService().remove(npc);
        npcService().save();
        sender.sendMessage(CC.success("NPC deleted.", "*" + label(npc) + "* has been removed."));
        return true;
    }

    @Command(names = {"npc list"}, permission = PERMISSION, description = "List saved NPCs")
    public boolean list(CommandSender sender) {
        List<NPC> npcs = npcService().getSerializedNpcs();

        sender.sendMessage(CC.header(Panel.DEV, "NPCs"));
        if (npcs.isEmpty()) sender.sendMessage(CC.empty("No NPCs have been created yet."));

        for (NPC npc : npcs) {
            Location loc = npc.getLocation();
            TextComponent.Builder hover = Component.text()
                    .append(Component.text(String.format("%s %.1f, %.1f, %.1f", loc.getWorld().getName(),
                            loc.getX(), loc.getY(), loc.getZ()), Theme.TEXT));
            if (npc.getCommand() != null) {
                hover.append(Component.newline())
                        .append(Component.text("Runs /" + npc.getCommand() + (npc.isConsoleCommand() ? " as console" : ""), Theme.TEXT));
            }
            hover.append(Component.newline()).append(Component.newline())
                    .append(Component.text("Click to teleport to this NPC.", Theme.TEXT));

            sender.sendMessage(CC.item(Component.text()
                    .append(Component.text(label(npc), Theme.TEXT_STRONG))
                    .append(Component.text(" #" + npc.getId(), Theme.TEXT))
                    .hoverEvent(HoverEvent.showText(hover.build()))
                    .clickEvent(ClickEvent.runCommand("/npc tpto " + npc.getId()))
                    .build()));
        }
        sender.sendMessage(CC.footer(Panel.DEV));
        return true;
    }

    @Command(names = {"npc setname", "npc name"}, permission = PERMISSION, description = "Set an NPC's nametag")
    public boolean setName(CommandSender sender, @Param(name = "npc") NPC npc,
                           @Param(name = "displayName", wildcard = true) String displayName) {
        npc.setDisplayName(displayName.equalsIgnoreCase("none") ? "" : displayName);
        npcService().save();
        sender.sendMessage(CC.success("Nametag updated.", "*" + label(npc) + "*'s nametag was changed."));
        return true;
    }

    @Command(names = {"npc command"}, permission = PERMISSION, description = "Set the command an NPC runs")
    public boolean command(CommandSender sender, @Param(name = "npc") NPC npc,
                           @Param(name = "command", wildcard = true) String command,
                           @Flag(names = {"console"}, description = "Run as the console") boolean console) {
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        npc.setCommand(normalized);
        npc.setConsoleCommand(console);
        npcService().save();
        sender.sendMessage(CC.success("Command set.", "*" + label(npc) + "* now runs */" + normalized + "*"
                + (console ? " as the console." : ".")));
        return true;
    }

    @Command(names = {"npc removecommand"}, permission = PERMISSION, description = "Remove an NPC's command")
    public boolean removeCommand(CommandSender sender, @Param(name = "npc") NPC npc) {
        npc.setCommand(null);
        npc.setConsoleCommand(false);
        npcService().save();
        sender.sendMessage(CC.success("Command removed.", "*" + label(npc) + "* no longer runs a command."));
        return true;
    }

    @Command(names = {"npc skin"}, permission = PERMISSION, async = true, description = "Copy a player's skin")
    public boolean skin(CommandSender sender, @Param(name = "npc") NPC npc, @Param(name = "player") String playerName) {
        String[] skin = NPC.fetchSkin(playerName);
        if (skin == null) {
            sender.sendMessage(CC.error("Skin not found.", "Couldn't load *" + playerName + "*'s skin from Mojang."));
            return false;
        }

        Tasks.run(() -> {
            npc.setSkin(skin);
            npcService().save();
        });
        sender.sendMessage(CC.success("Skin updated.", "*" + label(npc) + "* now uses *" + playerName + "*'s skin."));
        return true;
    }

    @Command(names = {"npc tphere", "npc movehere"}, permission = PERMISSION, description = "Move an NPC to you")
    public boolean tphere(Player sender, @Param(name = "npc") NPC npc) {
        npc.setLocation(sender.getLocation());
        npcService().save();
        sender.sendMessage(CC.success("NPC moved.", "*" + label(npc) + "* is now at your location."));
        return true;
    }

    @Command(names = {"npc tpto"}, permission = PERMISSION, description = "Teleport to an NPC")
    public boolean tpto(Player sender, @Param(name = "npc") NPC npc) {
        sender.teleport(npc.getLocation());
        sender.sendMessage(CC.info("Teleported to *" + label(npc) + "*."));
        return true;
    }

    @Command(names = {"npc equipment", "npc equip"}, permission = PERMISSION,
            description = "Give an NPC your held item")
    public boolean equipment(Player sender, @Param(name = "npc") NPC npc, @Param(name = "slot") EquipmentSlot slot) {
        ItemStack held = sender.getInventory().getItemInMainHand();
        ItemStack item = held.getType() == Material.AIR ? null : held.clone();

        npc.setEquipment(slot, item);
        npcService().save();
        String slotName = slot.name().toLowerCase();
        sender.sendMessage(item == null
                ? CC.success("Equipment cleared.", "*" + label(npc) + "*'s " + slotName + " slot is now empty.")
                : CC.success("Equipment set.", "*" + label(npc) + "* is now holding your item in its " + slotName + " slot."));
        return true;
    }
}
