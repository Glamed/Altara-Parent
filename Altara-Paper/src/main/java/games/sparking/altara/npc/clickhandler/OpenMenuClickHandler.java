package games.sparking.altara.npc.clickhandler;

import games.sparking.altara.menu.Menu;
import games.sparking.altara.npc.NPC;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class OpenMenuClickHandler implements NPCClickHandler {

    private final Menu menu;

    @Override
    public void click(NPC npc, Player player) {
        menu.openMenu(player);
    }
}
