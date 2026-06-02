package games.sparking.altara.hologram.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import games.sparking.altara.hologram.Hologram;
import games.sparking.altara.hologram.clickhandler.HologramClickHandler;
import games.sparking.altara.utils.timebased.TimeBasedContainer;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Intercepts hologram click interactions at the packet level.
 *
 * <p>Because holograms are <em>fake</em> packet-only entities (they do not exist
 * in the server's entity registry), Bukkit's {@code PlayerInteractAtEntityEvent}
 * and {@code EntityDamageByEntityEvent} will never fire for them.
 *
 * <p>Left-clicks are detected via {@code ATTACK} and right-clicks via
 * {@code INTERACT_ENTITY} ({@code INTERACT} / {@code INTERACT_AT}).
 *
 * <p>Register with:
 * <pre>{@code
 *   PacketEvents.getAPI().getEventManager().registerListeners(new HologramClickListener());
 * }</pre>
 */
public class HologramClickListener extends PacketListenerAbstract {

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

    /** Left-click: client sends ATTACK_ENTITY with the target entity ID. */
    private void handleAttack(PacketReceiveEvent event) {
        WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);

        Hologram.ClickData data = Hologram.getClickData(packet.getEntityId());
        if (data == null) return;

        event.setCancelled(true);

        if (!(event.getPlayer() instanceof Player player)) return;
        if (data.hologram().getClickHandler() == null) return;

        UUID uuid = player.getUniqueId();
        if (cooldown.contains(uuid)) return;

        data.hologram().getClickHandler().click(
                player, data.hologram(), data.lineIndex(), HologramClickHandler.ClickType.LEFT_CLICK);
        cooldown.add(uuid);
    }

    /** Right-click: client sends INTERACT_ENTITY with action INTERACT or INTERACT_AT. */
    private void handleInteract(PacketReceiveEvent event) {
        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

        Hologram.ClickData data = Hologram.getClickData(packet.getEntityId());
        if (data == null) return;

        event.setCancelled(true);

        if (!(event.getPlayer() instanceof Player player)) return;
        if (data.hologram().getClickHandler() == null) return;

        UUID uuid = player.getUniqueId();
        if (cooldown.contains(uuid)) return;

        data.hologram().getClickHandler().click(
                player, data.hologram(), data.lineIndex(), HologramClickHandler.ClickType.RIGHT_CLICK);
        cooldown.add(uuid);
    }
}
