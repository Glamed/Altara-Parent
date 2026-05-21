package games.sparking.altara;

import games.sparking.altara.command.BuildVersionCommand;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.gamemode.GamemodeCommand;
import games.sparking.altara.task.Tasks;
import games.sparking.altara.task.UpdateTask;
import games.sparking.altara.task.impl.BukkitTaskImplementor;
import games.sparking.altara.updater.FileUpdater;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Properties;

public class AltaraPaper extends Altara {

    @Getter private static JavaPlugin paperInstance;

    public AltaraPaper(JavaPlugin paperInstance) {
        super(SystemType.PAPER);
        AltaraPaper.paperInstance = paperInstance;

        init();
    }

    @Override
    public void init() {
        Tasks.setTaskImplementor(new BukkitTaskImplementor(paperInstance));
        UpdateTask.start();
    }

    @Override
    public void registerCommands() {
        CommandService.register(AltaraPaper.getPaperInstance(),
                new GamemodeCommand(),
                new BuildVersionCommand()
        );
    }

    @Override
    public void registerListener() {
        new FileUpdater();
    }
}


