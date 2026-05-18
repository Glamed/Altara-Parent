package games.sparking.altara;

import org.bukkit.plugin.java.JavaPlugin;

public class AltaraLobbyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        new AltaraLobby(this);
    }
}
