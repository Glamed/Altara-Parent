package games.sparking.altara;

import games.sparking.altara.command.annotation.Command;
import me.tofaa.entitylib.wrapper.hologram.Hologram;
import net.developertobi.entitylib.api.dsl.EntityDsl;
import net.developertobi.entitylib.api.hologram.FakeHologram;
import net.developertobi.entitylib.api.hologram.HologramType;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class AltaraCommand {

    @Command(names = "test", description = "A test command")
    public void testCommand(Player player) {

        FakeHologram hologram = EntityDsl.fakeHologram(
                "my-legacy-hologram-" + player.getUniqueId(),
                HologramType.LEGACY,
                player.getLocation().add(0, 2.2, 0),
                builder -> {
                    builder.line(Component.text("Hello"));
                    builder.line(Component.text("Only you can see this"));
                    builder.interaction(event -> {
                        event.getPlayer().sendMessage("Clicked!");
                    });
                }
        );

        hologram.spawn(player.getUniqueId());
        player.sendMessage(Component.text("Altara test command"));
    }
}
