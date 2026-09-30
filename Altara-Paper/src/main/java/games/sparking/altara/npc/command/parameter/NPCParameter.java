package games.sparking.altara.npc.command.parameter;

import games.sparking.altara.AltaraPaper;
import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.npc.NPC;
import games.sparking.altara.utils.CC;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class NPCParameter implements ParameterType<NPC> {

    @Override
    public NPC parse(CommandSender sender, String source) {
        NPC npc = AltaraPaper.getPaperInstance().getNpcService().getNpc(source);
        if (npc != null) return npc;

        try {
            npc = AltaraPaper.getPaperInstance().getNpcService().getNpc(Integer.parseInt(source));
        } catch (NumberFormatException ignored) {
            npc = null;
        }

        if (npc == null) {
            sender.sendMessage(CC.error("Invalid NPC.", "*" + source + "* doesn't exist."));
        }
        return npc;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, List<String> flags) {
        List<String> completions = new ArrayList<>();
        for (NPC npc : AltaraPaper.getPaperInstance().getNpcService().getSerializedNpcs()) {
            completions.add(npc.getName() != null ? npc.getName() : String.valueOf(npc.getId()));
        }
        return completions;
    }
}
