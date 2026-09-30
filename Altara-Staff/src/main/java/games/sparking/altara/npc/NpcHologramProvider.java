package games.sparking.altara.npc;

import games.sparking.altara.hologram.updating.HologramProvider;
import games.sparking.altara.selector.ServerSelectorEntry;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Per-viewer nametag lines for a realm NPC: {@link ServerSelectorEntry#getNpcLines()} with
 * the same placeholders as the selector item.
 */
@RequiredArgsConstructor
public class NpcHologramProvider implements HologramProvider {

    private final ServerSelectorEntry entry;

    @Override
    public List<String> getLines(Player player) {
        return entry.resolve(entry.getNpcLines(), player);
    }
}
