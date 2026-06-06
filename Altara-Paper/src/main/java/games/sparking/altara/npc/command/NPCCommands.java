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
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

@Header(
        primaryColor = "blue",
        secondaryColor = "dark_gray",
        tertiaryColor = "aqua",
        header = "NPC"
)
public class NPCCommands {

    private NPCService npcService() {
        return AltaraPaper.getPaperInstance().getNpcService();
    }

    @Command(names = {"npc create"},
             permission = "altara.npcs",
             description = "Create a new NPC at your location",
             playerOnly = true)
    public boolean create(Player sender, @Param(name = "name") String name) {
        try {
            Integer.parseInt(name);
            sender.sendMessage(CC.RED + "NPC names cannot be pure integers.");
            return false;
        } catch (NumberFormatException ignored) { }

        NPC npc = new NPCBuilder()
                .at(sender.getLocation())
                .buildAndSpawn();

        npc.setName(name);
        npc.spawn();
        npcService().register(npc);
        npcService().save();
        sender.sendMessage(CC.format("<blue>NPC <yellow>#%d <blue>(<yellow>%s<blue>) created.", npc.getId(), name));
        return true;
    }

    @Command(names = {"npc delete", "npc remove"},
             permission = "altara.npcs",
             description = "Delete a NPC")
    public boolean delete(CommandSender sender, @Param(name = "npc") NPC npc) {
        npcService().remove(npc);
        npcService().save();
        sender.sendMessage(CC.format("<blue>Deleted NPC <yellow>#%d<blue>.", npc.getId()));
        return true;
    }

    @Command(names = {"npc list"},
             permission = "altara.npcs",
             description = "List all NPCs")
    public boolean list(CommandSender sender) {
        List<NPC> npcs = npcService().getSerializedNpcs();
        if (npcs.isEmpty()) {
            sender.sendMessage(CC.RED + "No NPCs exist.");
            return true;
        }

        for (NPC npc : npcs) {
            String location = String.format("[%.1f, %.1f, %.1f]",
                    npc.getLocation().getX(),
                    npc.getLocation().getY(),
                    npc.getLocation().getZ());

            List<String> hover = new ArrayList<>();
            hover.add(CC.GREEN + "Location: " + location);
            hover.add(CC.YELLOW + "Click to teleport");

            if (npc.getCommand() != null) {
                hover.add(" ");
                hover.add(CC.BLUE + "Command: " + CC.YELLOW + npc.getCommand());
                if (npc.isConsoleCommand())
                    hover.add(CC.GRAY + "(Console Command)");
            }

            Component msg = Component.text(npc.getName() + " - #" + npc.getId(), CC.RED)
                    .hoverEvent(HoverEvent.showText(CC.format(String.join("\n", hover))))
                    .clickEvent(ClickEvent.runCommand("/npc tpto " + npc.getId()));
            sender.sendMessage(msg);
        }
        return true;
    }

    @Command(names = {"npc setname", "npc name"},
             permission = "altara.npcs",
             description = "Set the display name of a NPC")
    public boolean setName(CommandSender sender,
                           @Param(name = "npc") NPC npc,
                           @Param(name = "displayName", wildcard = true) String displayName) {
        npc.setDisplayName(displayName); // triggers re-spawn
        npcService().save();
        sender.sendMessage(CC.format("<blue>Set display name of NPC <yellow>#%d <blue>to '<reset>%s<blue>'.",
                npc.getId(), displayName));
        return true;
    }

    @Command(names = {"npc command"},
             permission = "altara.npcs",
             description = "Set the command run when a NPC is clicked")
    public boolean command(CommandSender sender,
                           @Param(name = "npc") NPC npc,
                           @Param(name = "command", wildcard = true) String command,
                           @Flag(names = {"-console"}, description = "Execute from the console") boolean consoleCommand) {
        npc.setCommand(command);
        npc.setConsoleCommand(consoleCommand);
        npcService().save();
        sender.sendMessage(CC.format("<blue>Set the command of NPC <yellow>#%d <blue>to <yellow>%s<blue>.%s",
                npc.getId(), command,
                consoleCommand ? " <gray>(Console Command)" : ""));
        return true;
    }

    @Command(names = {"npc removecommand"},
             permission = "altara.npcs",
             description = "Remove the command from a NPC")
    public boolean removeCommand(CommandSender sender, @Param(name = "npc") NPC npc) {
        npc.setCommand(null);
        npc.setConsoleCommand(false);
        npcService().save();
        sender.sendMessage(CC.format("<blue>Removed the command from NPC <yellow>#%d<blue>.", npc.getId()));
        return true;
    }

    @Command(names = {"npc skin"},
             permission = "altara.npcs",
             description = "Set the skin of a NPC by player name",
             async = true)
    public boolean skin(CommandSender sender,
                        @Param(name = "npc") NPC npc,
                        @Param(name = "playerName") String playerName) {
        String[] skin = NPC.fetchSkin(playerName);
        npc.setSkin(skin); // triggers re-spawn on main thread (setSkin calls destroy+spawn)
        npcService().save();

        if (skin == null) {
            sender.sendMessage(CC.format("<red>Could not fetch skin for <yellow>%s<red>. NPC reset to default.", playerName));
        } else {
            sender.sendMessage(CC.format("<blue>NPC <yellow>#%d <blue>now has the skin of <yellow>%s<blue>.", npc.getId(), playerName));
        }
        return true;
    }

    @Command(names = {"npc tphere", "npc movehere"},
             permission = "altara.npcs",
             description = "Teleport a NPC to your location",
             playerOnly = true)
    public boolean tphere(Player sender, @Param(name = "npc") NPC npc) {
        npc.setLocation(sender.getLocation()); // triggers re-spawn
        npcService().save();
        sender.sendMessage(CC.format("<blue>Teleported NPC <yellow>#%d <blue>to your location.", npc.getId()));
        return true;
    }

    @Command(names = {"npc tpto"},
             permission = "altara.npcs",
             description = "Teleport to a NPC",
             playerOnly = true)
    public boolean tpto(Player sender, @Param(name = "npc") NPC npc) {
        sender.teleport(npc.getLocation());
        sender.sendMessage(CC.format("<blue>Teleported to NPC <yellow>#%d<blue>.", npc.getId()));
        return true;
    }

    @Command(names = {"npc equipment", "npc equip"},
             permission = "altara.npcs",
             description = "Set equipment on a NPC from your held item",
             playerOnly = true)
    public boolean equipment(Player sender,
                             @Param(name = "npc") NPC npc,
                             @Param(name = "slot") EquipmentSlot slot) {
        var held = sender.getInventory().getItemInMainHand();
        var item = held.getType() == Material.AIR ? null : held.clone();
        npc.setEquipment(slot, item);
        npcService().save();
        sender.sendMessage(CC.format(
                "<blue>Set <yellow>%s <blue>slot of NPC <yellow>#%d <blue>to <yellow>%s<blue>.",
                slot.name(),
                npc.getId(),
                item == null ? "empty" : item.getType().name()));
        return true;
    }
}
