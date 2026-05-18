package games.sparking.altara.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {

    private final List<Component> components = new ArrayList<>();

    // Message-level events applied to the root so ALL child components inherit them
    private ClickEvent messageClickEvent;
    private HoverEvent<?> messageHoverEvent;

    public ChatMessage(String text) {
        add(text);
    }

    public ChatMessage(String text, String command, String hoverText) {
        add(text);

        if (command != null) {
            runCommand(command);
        }
        if (hoverText != null) {
            hoverText(hoverText);
        }
    }

    // ------------------------
    // ADD TEXT
    // ------------------------

    public ChatMessage add(String text) {
        components.add(CC.translateToComponent(text));
        return this;
    }

    public ChatMessage add(ChatMessage other) {
        components.add(other.build());
        return this;
    }

    // ------------------------
    // STYLE LAST COMPONENT
    // ------------------------

    private Component getLast() {
        return components.get(components.size() - 1);
    }

    private void replaceLast(Component updated) {
        components.set(components.size() - 1, updated);
    }

    public ChatMessage color(NamedTextColor color) {
        replaceLast(getLast().color(color));
        return this;
    }

    public ChatMessage color(ChatColor chatColor) {
        java.awt.Color awtColor = chatColor.getColor();
        if (awtColor != null) {
            replaceLast(getLast().color(TextColor.color(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue())));
        }
        return this;
    }

    public ChatMessage decorate(TextDecoration decoration) {
        replaceLast(getLast().decorate(decoration));
        return this;
    }

    public String toPlainText() {
        return PlainTextComponentSerializer.plainText().serialize(build());
    }

    // ------------------------
    // EVENTS
    // ------------------------

    public ChatMessage hoverText(String text) {
        Component hover = CC.translateToComponent(text);
        messageHoverEvent = HoverEvent.showText(hover);
        // Also apply to last component for backwards-compat with per-component hover
        replaceLast(getLast().hoverEvent(messageHoverEvent));
        return this;
    }

    public ChatMessage runCommand(String command) {
        messageClickEvent = ClickEvent.runCommand(command);
        replaceLast(getLast().clickEvent(messageClickEvent));
        return this;
    }

    public ChatMessage suggestCommand(String command) {
        messageClickEvent = ClickEvent.suggestCommand(command);
        replaceLast(getLast().clickEvent(messageClickEvent));
        return this;
    }

    public ChatMessage openUrl(String url) {
        messageClickEvent = ClickEvent.openUrl(url);
        replaceLast(getLast().clickEvent(messageClickEvent));
        return this;
    }

    // ------------------------
    // BUILD / SEND
    // ------------------------

    public Component build() {
        Component result = Component.empty();

        for (Component component : components) {
            result = result.append(component);
        }

        // Apply message-level events to the root so every child component inherits them
        if (messageClickEvent != null) result = result.clickEvent(messageClickEvent);
        if (messageHoverEvent != null) result = result.hoverEvent(messageHoverEvent);

        return result;
    }

    public void send(CommandSender sender) {
        send(sender, null);
    }

    public void send(CommandSender sender, String permission) {
        Component built = build();

        if (sender instanceof Player player) {
            if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                player.sendMessage(built);
            } else {
                player.sendMessage(
                        PlainTextComponentSerializer.plainText().serialize(built)
                );
            }
        } else {
            sender.sendMessage(
                    PlainTextComponentSerializer.plainText().serialize(built)
            );
        }
    }

    // ------------------------
    // LENGTH
    // ------------------------

    public int length() {
        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        int len = 0;
        for (Component c : components) {
            len += serializer.serialize(c).length();
        }
        return len;
    }
}