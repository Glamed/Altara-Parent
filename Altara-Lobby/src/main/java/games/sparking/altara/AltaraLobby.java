package games.sparking.altara;

import games.sparking.altara.command.CommandService;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class AltaraLobby extends AltaraPaper {

    public AltaraLobby(JavaPlugin instance) {
        super(instance);
        Altara.setServerIdentifier("Lobby");
        registerCommands();
        registerListener();
        getPaperInstance().getServer().getConsoleSender().sendMessage(Component.text("Altara lobby has booted"));
    }


    @Override
    public void registerCommands() {
        super.registerCommands();
        CommandService.register(AltaraPaper.getPaperInstance(),
                new AltaraCommand()
        );
    }
}
