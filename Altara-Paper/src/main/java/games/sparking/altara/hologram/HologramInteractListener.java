package games.sparking.altara.hologram;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HologramInteractListener extends PacketListenerAbstract {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();

        Hologram hologram = HologramService.getHologramForEntityId(entityId);
        if (hologram == null || hologram.getClickHandler() == null) return;

        UUID playerUuid = event.getUser().getUUID();
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        HologramClickHandler.ClickType clickType =
                packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK
                        ? HologramClickHandler.ClickType.LEFT_CLICK
                        : HologramClickHandler.ClickType.RIGHT_CLICK;

        hologram.getClickHandler().click(player, hologram, clickType);
    }
}

