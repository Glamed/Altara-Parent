package games.sparking.altara.npc;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class NPCInteractListener extends PacketListenerAbstract {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();

        NPC npc = NPCService.getNpcForEntityId(entityId);
        if (npc == null || npc.getClickHandler() == null) return;

        UUID playerUuid = event.getUser().getUUID();
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return;

        NPCClickHandler.ClickType clickType =
                packet.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK
                        ? NPCClickHandler.ClickType.LEFT_CLICK
                        : NPCClickHandler.ClickType.RIGHT_CLICK;

        npc.getClickHandler().click(player, npc, clickType);
    }
}

