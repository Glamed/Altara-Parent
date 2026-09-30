package games.sparking.altara.chatinput;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.utils.CC;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Prompts a player to type a value in chat, parsed with the registered
 * {@link ParameterType} for {@code T}.  Typing an escape sequence (default
 * {@code cancel}) aborts the prompt.
 */
public class ChatInput<T> {

    private static final Map<UUID, ChatInput<?>> INPUT_MAP = new ConcurrentHashMap<>();

    private final Class<?> clazz;
    private ChatInputChain parent;

    private Component[] text = new Component[0];

    private boolean exitOnInvalidInput = false;
    private String[] escapeSequences = new String[]{"cancel"};
    private Component escapeMessage = CC.info("Input cancelled.");

    private ChatInputConsumer<T> consumer;
    private Consumer<Player> onCancel;

    public ChatInput(Class<?> clazz) {
        this.clazz = clazz;
    }

    // ── Static tracking ──────────────────────────────────────────────────────

    protected static ChatInput<?> getInput(Player player) {
        return INPUT_MAP.get(player.getUniqueId());
    }

    protected static void clear(UUID uuid) {
        INPUT_MAP.remove(uuid);
    }

    // ── Builder API ──────────────────────────────────────────────────────────

    public ChatInput<T> text(Component... text) {
        this.text = text == null ? new Component[0] : text;
        return this;
    }

    public ChatInput<T> exitOnInvalidInput() {
        this.exitOnInvalidInput = true;
        return this;
    }

    public ChatInput<T> escapeSequences(String... escapeSequences) {
        if (escapeSequences != null && escapeSequences.length > 0) {
            this.escapeSequences = escapeSequences;
        }
        return this;
    }

    public ChatInput<T> escapeMessage(Component escapeMessage) {
        if (escapeMessage != null) {
            this.escapeMessage = escapeMessage;
        }
        return this;
    }

    /** Called with the parsed value; return {@code false} to reject it and re-prompt. */
    public ChatInput<T> accept(ChatInputConsumer<T> consumer) {
        this.consumer = consumer;
        return this;
    }

    public ChatInput<T> onCancel(Consumer<Player> onCancel) {
        this.onCancel = onCancel;
        return this;
    }

    public void setParent(ChatInputChain parent) {
        this.parent = parent;
    }

    // ── Prompt ───────────────────────────────────────────────────────────────

    public void send(Player player) {
        for (Component line : text) {
            if (line != null) player.sendMessage(line);
        }
        INPUT_MAP.put(player.getUniqueId(), this);
    }

    // ── Input handling (main thread) ─────────────────────────────────────────

    @SuppressWarnings("unchecked")
    protected void handle(Player player, String message) {
        message = message.trim();

        for (String escape : escapeSequences) {
            if (escape.equalsIgnoreCase(message)) {
                clear(player.getUniqueId());
                player.sendMessage(escapeMessage);
                if (onCancel != null) onCancel.accept(player);
                return;
            }
        }

        ParameterType<T> parameter = (ParameterType<T>) CommandService.getParameter(clazz);
        if (parameter == null) {
            clear(player.getUniqueId());
            player.sendMessage(CC.error("Input error.", "No parser is registered for " + clazz.getSimpleName() + "."));
            return;
        }

        T parsed = parameter.parse(player, message);
        boolean accepted = parsed != null && (consumer == null || consumer.accept(player, parsed));

        if (!accepted) {
            if (exitOnInvalidInput) clear(player.getUniqueId());
            else send(player);
            return;
        }

        // The consumer may have started a new prompt; only clear if it's still ours.
        INPUT_MAP.remove(player.getUniqueId(), this);
        if (parent != null) {
            parent.next(player);
        }
    }
}
