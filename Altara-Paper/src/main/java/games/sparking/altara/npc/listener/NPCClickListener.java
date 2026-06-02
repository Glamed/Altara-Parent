package games.sparking.altara.npc.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import games.sparking.altara.npc.NPC;
import games.sparking.altara.utils.timebased.TimeBasedContainer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * PacketEvents listener that intercepts {@code ATTACK} and {@code INTERACT_ENTITY}
 * packets to handle NPC click events.
 *
 * <p>Because NPCs are fake packet-only entities (they are absent from the
 * server's entity registry), Bukkit's entity-interact events will never fire
 * for them.  Cancelling the raw packet prevents the server from logging an
 * error about an unknown entity.
 *
 * <p>Left-clicks are detected via {@code ATTACK} and right-clicks via
 * {@code INTERACT_ENTITY} ({@code INTERACT} / {@code INTERACT_AT}).
 *
 * <p>Register with:
 * <pre>{@code
 *   PacketEvents.getAPI().getEventManager().registerListeners(new NPCClickListener());
 * }</pre>
 */
public class NPCClickListener extends PacketListenerAbstract {

    /** Per-player click cooldown to prevent rapid double-fires. */
    private final TimeBasedContainer<UUID> cooldown =
            new TimeBasedContainer<>(500, TimeUnit.MILLISECONDS);

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            handleAttack(event);
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            handleInteract(event);
        }
    }

    /** Left-click: client sends ATTACK with the target entity ID. */
    private void handleAttack(PacketReceiveEvent event) {
        WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
        NPC npc = NPC.getByEntityId(packet.getEntityId());
        if (npc == null) return;

        // Cancel the packet so the server never tries to look up the entity.
        event.setCancelled(true);

        if (!(event.getPlayer() instanceof Player player)) return;
        if (npc.getClickHandler() == null) return;

        UUID uuid = player.getUniqueId();
        if (cooldown.contains(uuid)) return;

        npc.getClickHandler().click(npc, player);
        cooldown.add(uuid);
    }

    /** Right-click: client sends INTERACT_ENTITY with action INTERACT or INTERACT_AT. */
    private void handleInteract(PacketReceiveEvent event) {
        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        NPC npc = NPC.getByEntityId(packet.getEntityId());
        if (npc == null) return;

        // Cancel the packet so the server never tries to look up the entity.
        event.setCancelled(true);

        if (!(event.getPlayer() instanceof Player player)) return;
        if (npc.getClickHandler() == null) return;

        UUID uuid = player.getUniqueId();
        if (cooldown.contains(uuid)) return;

        npc.getClickHandler().click(npc, player);
        cooldown.add(uuid);
    }
}


