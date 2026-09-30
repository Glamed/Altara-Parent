package games.sparking.altara.command;

import games.sparking.altara.command.annotation.CommandCooldown;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.data.Data;
import games.sparking.altara.command.data.FlagData;
import games.sparking.altara.command.data.ParameterData;
import games.sparking.altara.command.permission.PermissionAdapter;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Panel;
import games.sparking.altara.utils.Messages;
import games.sparking.altara.utils.Time;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@NoArgsConstructor
public class CommandNode {

    private Method method;
    private Object object;
    private String label = "";
    private List<String> aliases = new ArrayList<>();
    private String permission;
    private String description = "";
    private boolean async;
    private boolean hidden;
    private boolean playerOnly;
    private CommandCooldown commandCooldown;
    private Header header;
    private CommandNode parent;
    private List<CommandNode> childs = new ArrayList<>();
    private List<Data> parameters = new ArrayList<>();
    private List<String> flags = new ArrayList<>();

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private volatile long globalCooldown = -1;

    public void registerChild(CommandNode child) {
        child.setParent(this);
        childs.add(child);
    }

    public CommandNode getChild(String name) {
        for (CommandNode child : childs) {
            if (child.getLabel().equalsIgnoreCase(name) || child.getAliases().contains(name.toLowerCase()))
                return child;
        }
        return null;
    }

    /** Permission check only — {@link #isPlayerOnly()} is enforced separately. */
    public boolean canUse(CommandSender sender) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        PermissionAdapter adapter = CommandService.getPermissionAdapter(permission);
        if (adapter != null)
            return adapter.testSilent(sender);

        return sender.hasPermission(permission);
    }

    public CommandNode findNode(List<String> args) {
        if (!args.isEmpty()) {
            CommandNode child = getChild(args.get(0));
            if (child != null) {
                args.remove(0);
                return child.findNode(args);
            }
        }
        return this;
    }

    /** {@code ┃ Invalid syntax. Try /command [arg].} */
    public Component getUsage(String realLabel) {
        return CC.error("Invalid syntax.", "Try *" + realLabel + buildSignature() + "*.");
    }

    /**
     * Builds the argument signature: required parameters as {@code [name]},
     * optional ones as {@code (name)}, flags as {@code (-flag)}.
     */
    private String buildSignature() {
        StringBuilder sig = new StringBuilder();

        for (Data parameter : parameters) {
            if (parameter instanceof ParameterData data) {
                boolean required = data.getDefaultValue().isEmpty();
                sig.append(' ')
                        .append(required ? '[' : '(')
                        .append(data.getName())
                        .append(data.isWildCard() ? "..." : "")
                        .append(required ? ']' : ')');
            }
        }

        for (Data parameter : parameters) {
            if (parameter instanceof FlagData data && !data.isHidden()) {
                sig.append(" (-").append(data.getNames().get(0)).append(')');
            }
        }

        return sig.toString();
    }

    public boolean invoke(CommandSender sender, List<String> args, List<String> flags) {
        if (method == null) {
            sendHelp(sender);
            return true;
        }

        List<ParameterData> realParameters = new ArrayList<>();
        for (Data data : parameters) {
            if (data instanceof ParameterData parameterData) realParameters.add(parameterData);
        }

        List<String> unusedArguments = new ArrayList<>(args);
        List<ParameterData> defaultsUsed = new ArrayList<>();
        Map<Integer, Object> objects = new TreeMap<>();
        objects.put(0, sender);

        int index = 0;
        for (Data parameter : parameters) {
            if (parameter instanceof FlagData data) {
                boolean value = data.isDefaultValue();
                for (String name : data.getNames()) {
                    if (flags.contains(name.toLowerCase())) value = !value;
                }
                objects.put(data.getIndex(), value);
                continue;
            }

            ParameterData data = (ParameterData) parameter;
            if (data.getParameterType() == null) {
                sender.sendMessage(CC.error("Command error.", "No parser is registered for " + data.getType().getSimpleName() + "."));
                return true;
            }

            String argument;
            if (!data.getDefaultValue().isEmpty() && args.size() < realParameters.size()) {
                Object parsed = data.getParameterType().parse(sender, data.getDefaultValue());
                if (parsed == null) return true;

                objects.put(data.getIndex() + 1, parsed);
                defaultsUsed.add(data);
                continue;
            }

            if (index >= args.size()) return false;
            argument = args.get(index);
            unusedArguments.remove(argument);

            if (data.isWildCard()) {
                argument = StringUtils.join(args.toArray(new String[0]), " ", index, args.size());
            }

            Object parsed = data.getParameterType().parse(sender, argument);
            if (parsed == null) return true;

            objects.put(data.getIndex() + 1, parsed);
            index++;
        }

        // Arguments that were not consumed may still fill optional parameters, in order.
        int unusedIndex = 0;
        for (ParameterData data : defaultsUsed) {
            if (unusedIndex >= unusedArguments.size()) break;

            Object parsed = data.getParameterType().parse(sender, unusedArguments.get(unusedIndex));
            if (parsed == null) continue;

            objects.put(data.getIndex() + 1, parsed);
            unusedIndex++;
        }

        if (sender instanceof Player player && hasCooldown(player)) {
            sender.sendMessage(CC.error(Messages.COOLDOWN, formatRemainingCooldown(player)));
            return true;
        }

        try {
            Object result = method.invoke(object, objects.values().toArray());
            boolean success = method.getReturnType() != Boolean.TYPE || Boolean.TRUE.equals(result);

            if (success && commandCooldown != null && sender instanceof Player player) {
                if (commandCooldown.global()) globalCooldown = System.currentTimeMillis();
                else cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return true;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException(cause);
        }
    }

    /** Header + one {@code /command [args] - Description} line per usable sub-command. */
    private void sendHelp(CommandSender sender) {
        List<CommandNode> usable = new ArrayList<>();
        for (CommandNode node : childs) {
            if (node.canUse(sender) && !node.isHidden()) usable.add(node);
        }

        if (usable.isEmpty()) {
            sender.sendMessage(hidden || childs.isEmpty()
                    ? CommandService.UNKNOWN_COMMAND_MESSAGE
                    : CommandService.NO_PERMISSION_MESSAGE);
            return;
        }

        usable.sort(Comparator.comparing(CommandNode::getFullLabel));
        String prefix = sender instanceof Player ? "/" : "";
        String title = header != null ? header.value() : StringUtils.capitalize(label);
        String subtitle = header != null ? header.subtitle() : null;
        Panel panel = header != null ? header.panel() : Panel.PLAYER;

        sender.sendMessage(CC.header(panel, title, subtitle));
        for (CommandNode node : usable) {
            sender.sendMessage(CC.command(panel, prefix + node.getFullLabel() + node.buildSignature(), node.getDescription()));
        }
        sender.sendMessage(CC.footer(panel));
    }

    public String getFullLabel() {
        return parent != null ? parent.getFullLabel() + " " + label : label;
    }

    private boolean hasCooldown(Player player) {
        if (commandCooldown == null) return false;

        String bypass = commandCooldown.bypassPermission();
        if (!bypass.isEmpty() && player.hasPermission(bypass)) return false;

        long length = commandCooldown.timeUnit().toMillis(commandCooldown.time());
        if (commandCooldown.global()) {
            return globalCooldown + length > System.currentTimeMillis();
        }

        Long last = cooldowns.get(player.getUniqueId());
        return last != null && last + length > System.currentTimeMillis();
    }

    private String formatRemainingCooldown(Player player) {
        long start = commandCooldown.global() ? globalCooldown : cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long end = start + commandCooldown.timeUnit().toMillis(commandCooldown.time());
        return Time.formatDetailed(Math.max(1000, end - System.currentTimeMillis()));
    }
}
