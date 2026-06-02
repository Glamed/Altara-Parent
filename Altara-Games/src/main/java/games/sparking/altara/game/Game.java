package games.sparking.altara.game;

import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;

@AllArgsConstructor
public class Game implements Listener {

    private final String name;
    private final String description;

    private final Kit[] kits;
    private final Module[] modules;

}
