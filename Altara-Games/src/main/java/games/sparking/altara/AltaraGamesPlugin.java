package games.sparking.altara;

import org.bukkit.plugin.java.JavaPlugin;

public class AltaraGamesPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        new AltaraGames(this);
    }
}
