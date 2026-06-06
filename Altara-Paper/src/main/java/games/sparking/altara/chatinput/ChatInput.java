package games.sparking.altara.chatinput;

import games.sparking.altara.command.CommandService;
import games.sparking.altara.command.parameter.ParameterType;
import games.sparking.altara.task.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInput<T> {

    private static final Map<UUID, ChatInput<?>> INPUT_MAP = new ConcurrentHashMap<>();

    private final Class<?> clazz;
    private ChatInputChain parent;

    /** Prompt lines (already formatted Components) */
    private Component[] text = new Component[]{Component.empty()};

    private boolean exitOnInvalidInput = false;
    private String[] escapeSequences = new String[]{"cancel"};

    /** Cancellation message (Component) */
    private Component escapeMessage =
            Component.text("You cancelled the active input.", NamedTextColor.RED);

    private ChatInputConsumer<T> consumer;
    private Consumer<Player> onCancel;

    public ChatInput(Class<?> clazz) {
        this.clazz = clazz;
    }

    // ─────────────────────────────────────────────────────────────
    // Static tracking
    // ─────────────────────────────────────────────────────────────

    protected static ChatInput<?> getInput(Player player) {
        return INPUT_MAP.get(player.getUniqueId());
    }

    protected static void clear(UUID uuid) {
        INPUT_MAP.remove(uuid);
    }

    // ─────────────────────────────────────────────────────────────
    // Builder API
    // ─────────────────────────────────────────────────────────────

    public ChatInput<T> text(Component... text) {
        if (text == null || text.length == 0) {
            text = new Component[]{Component.empty()};
        }
        this.text = text;
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

    // ─────────────────────────────────────────────────────────────
    // Send prompt
    // ─────────────────────────────────────────────────────────────

    public void send(Player player) {
        if (text != null && text.length > 0) {
            for (Component line : text) {
                if (line != null && !line.equals(Component.empty())) {
                    player.sendMessage(line);
                }
            }
        }

        INPUT_MAP.put(player.getUniqueId(), this);
    }

    // ─────────────────────────────────────────────────────────────
    // Input handling
    // ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    protected void handle(Player player, String message) {
        message = message.trim();

        // ── Escape handling ───────────────────────────────────────────────
        for (String escape : escapeSequences) {
            if (escape.equalsIgnoreCase(message)) {
                clear(player.getUniqueId());
                player.sendMessage(escapeMessage);

                if (onCancel != null) {
                    onCancel.accept(player);
                }
                return;
            }
        }

        // ── Resolve parser ────────────────────────────────────────────────
        ParameterType<T> parameter =
                (ParameterType<T>) CommandService.getParameter(clazz);

        if (parameter == null) {
            clear(player.getUniqueId());

            player.sendMessage(Component.text(
                    "No ParameterType found for " + clazz.getName()
                            + ". Contact an administrator.",
                    NamedTextColor.RED
            ));
            return;
        }

        // ── Parse input ───────────────────────────────────────────────────
        T parsed = parameter.parse(player, message);

        if (parsed == null) {
            if (exitOnInvalidInput) {
                clear(player.getUniqueId());
                return;
            }

            send(player);
            return;
        }

        // ── Consume result ────────────────────────────────────────────────
        Tasks.run(() -> {
            if (consumer == null || !consumer.accept(player, parsed)) {
                if (exitOnInvalidInput) {
                    clear(player.getUniqueId());
                } else {
                    send(player);
                }
            }
        });

        clear(player.getUniqueId());

        if (parent != null) {
            parent.next(player);
        }
    }
}