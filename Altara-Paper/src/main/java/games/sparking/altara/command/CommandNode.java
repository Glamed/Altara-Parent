package games.sparking.altara.command;

import games.sparking.altara.command.annotation.CommandCooldown;
import games.sparking.altara.command.annotation.Header;
import games.sparking.altara.command.data.Data;
import games.sparking.altara.command.data.FlagData;
import games.sparking.altara.command.data.ParameterData;
import games.sparking.altara.command.permission.PermissionAdapter;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Time;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class CommandNode {

    private static HashMap<String, CommandNode> commands = new HashMap<>();

    private Method method;
    private Object object;
    private String label = "";
    private List<String> aliases = new ArrayList<>();
    private String permission;
    private String description = "N/A";
    private TextColor color = NamedTextColor.GRAY;    private boolean async;
    private boolean hidden;
    private boolean playerOnly;
    private CommandCooldown commandCooldown;
    private Header header;
    private CommandNode parent;
    private List<CommandNode> childs = new ArrayList<>();
    private List<Data> parameters = new ArrayList<>();
    private List<String> flags = new ArrayList<>();

    private Map<Player, Long> cooldowns = new HashMap<>();
    private long globalCooldown = -1;

    public void registerChild(CommandNode child) {
        child.setParent(this);
        childs.add(child);
    }

    public CommandNode getChild(String name) {
        for (CommandNode child : childs) {
            if (child.getLabel().equalsIgnoreCase(name)
                    || child.getAliases().contains(name))
                return child;
        }

        return null;
    }

    public boolean canUse(CommandSender sender) {
        if (permission == null) {
            return true;
        }

        PermissionAdapter adapter = CommandService.getPermissionAdapter(permission);
        if (adapter != null)
            return adapter.testSilent(sender);

        if (permission.equals("")) {
            return !playerOnly || sender instanceof Player;
        }

        return playerOnly ? sender instanceof Player && sender.hasPermission(permission) :
                sender.hasPermission(permission);
    }

    public CommandNode findNode(List<String> args) {
        if (!args.isEmpty()) {
            String s = args.get(0);
            if (getChild(s) != null) {
                args.remove(s);
                return getChild(s).findNode(args);
            }
        }
        return this;
    }

    public Component getUsage(String realLabel) {
        return CC.errorMsg("Invalid syntax.", "Try " + realLabel + " " + buildSignature());
    }

    public String getList(String realLabel) {
        return realLabel + buildSignature();
    }

    /** Builds a plain-text parameter/flag signature, e.g. {@code "(name) [age] (-silent)."} */
    private String buildSignature() {
        List<FlagData> flags = new ArrayList<>();
        for (Data parameter : parameters) {
            if (!(parameter instanceof FlagData data) || data.isHidden()) continue;
            flags.add(data);
        }

        List<ParameterData> params = new ArrayList<>();
        for (Data parameter : parameters) {
            if (parameter instanceof ParameterData data) params.add(data);
        }

        StringBuilder sig = new StringBuilder();

        if (!params.isEmpty()) {
            sig.append(" ");
            int index = 0;
            for (ParameterData param : params) {
                boolean required = param.getDefaultValue().isEmpty();
                boolean wildcard = param.isWildCard();
                sig.append(required ? "(" : "[")
                   .append(param.getName())
                   .append(wildcard ? "..." : "")
                   .append(required ? ")" : "]");
                if (index != params.size() - 1) sig.append(" ");
                index++;
            }
        }

        if (!flags.isEmpty()) {
            sig.append(" (");
            boolean firstFlag = true;
            for (FlagData flag : flags) {
                if (!firstFlag) sig.append(" | ");
                firstFlag = false;
                sig.append("-").append(flag.getNames().get(0));
            }
            sig.append(")");
        }

        sig.append(".");
        return sig.toString();
    }

    public boolean invoke(CommandSender sender, List<String> args, List<String> flags) {
        if (method == null) {
            if (!childs.isEmpty()) {

                Set<CommandNode> usable = new HashSet<>();
                for (CommandNode node : childs)
                    if (node.canUse(sender) && !node.isHidden())
                        usable.add(node);

                if (usable.isEmpty()) {
                    if (hidden) {
                        sender.sendMessage(CommandService.UNKNOWN_COMMAND_MESSAGE);
                        return true;
                    }

                    sender.sendMessage(CommandService.NO_PERMISSION_MESSAGE);
                    return true;
                }

                String primaryColor   = header.primaryColor();
                String secondaryColor = header.secondaryColor();
                String tertiaryColor  = header.tertiaryColor();
                String title          = header.header();
                String subHeaderColor = header.subHeaderColor();
                String subHeader      = header.subHeader();

                TextColor primary   = parseHeaderColor(primaryColor);
                TextColor secondary = parseHeaderColor(secondaryColor);
                TextColor tertiary  = parseHeaderColor(tertiaryColor);
                TextColor subHdrClr = parseHeaderColor(subHeaderColor);

                sender.sendMessage(CC.genLine(primary, secondary, tertiary,
                        title.isEmpty()    ? Component.empty() : Component.text(title),
                        subHdrClr,
                        subHeader.isEmpty() ? Component.empty() : Component.text(subHeader)));

                usable.forEach(node -> {
                    String listText = node.getList(node.getFullLabel());
                    String prefix   = (sender instanceof Player ? "/" : "");
                    String desc     = node.getDescription();
                    Component line = Component.text()
                            .append(Component.text(prefix + listText, tertiary))
                            .append(desc.isEmpty() || desc.equalsIgnoreCase("N/A")
                                    ? Component.empty()
                                    : Component.text(" - ", CC.GRAY)
                                            .append(Component.text(desc, CC.WHITE)))
                            .build();
                    sender.sendMessage(line);
                });

                sender.sendMessage(CC.genLine(primary, secondary));
                return true;
            }
            sender.sendMessage(CommandService.UNKNOWN_COMMAND_MESSAGE);
            return true;
        }

        List<ParameterData> realParameters = new ArrayList<>();
        for (Data data : parameters) {
            if (data instanceof ParameterData) {
                realParameters.add((ParameterData) data);
            }
        }
        List<String> unusedArguments = new ArrayList<>(args);
        List<ParameterData> defaultsUsed = new ArrayList<>();
        Map<Integer, Object> objects = new HashMap<>();
        objects.put(0, sender);
        int index = 0;
        for (Data parameter : parameters) {
            if (parameter instanceof FlagData data) {
                boolean value = data.isDefaultValue();
                for (String name : data.getNames()) {
                    if (flags.contains(name.toLowerCase())) {
                        value = !value;
                    }
                }
                objects.put(data.getIndex(), value);
            } else if (parameter instanceof ParameterData data) {
                String argument;
                if ((!data.getDefaultValue().isEmpty()) && (args.size() < realParameters.size())) {
                    argument = data.getDefaultValue();
                    Object parsed = data.getParameterType().parse(sender, argument);
                    if (parsed == null) {
                        return true;
                    }
                    objects.put(data.getIndex() + 1, parsed);
                    defaultsUsed.add(data);
                    continue;
                } else {
                    try {
                        argument = args.get(index);
                    } catch (Exception e) {
                        return false;
                    }
                }
                unusedArguments.remove(argument);
                if ((data.isWildCard()) && (!args.isEmpty()) /*|| (argument.equals(data.getDefaultValue()))*/) {
                    argument = StringUtils.join(args.toArray(new String[0]), " ", index, args.size());
                }
                if (data.getParameterType() == null) {
                    sender.sendMessage(CC.RED + "Could not find a ParameterType to parse " + data.getType().getName() + "." +
                            " Please contact the server administration if this continues to happen.");
                    return true;
                }
                Object parsed = data.getParameterType().parse(sender, argument);
                if (parsed == null) {
                    return true;
                }
                objects.put(data.getIndex() + 1, parsed);
            }
            index++;
        }

        index = 0;
        for (ParameterData data : defaultsUsed) {
            String argument;
            try {
                argument = unusedArguments.get(index);
            } catch (Exception e) {
                continue;
            }
            Object parsed = data.getParameterType().parse(sender, argument);
            if (parsed == null) {
                continue;
            }
            objects.put(data.getIndex() + 1, parsed);
            index++;
        }


        /*if ((!hasWildCard) && (args.size() > flagsUsed + parametersUsed)) {
            return false;
        }*/

        if (sender instanceof Player
                && hasCooldown((Player) sender)) {
            if (commandCooldown.global())
                sender.sendMessage(CC.format("<red>This command is on a global cooldown for another <yellow>%s<red>.",
                        formatRemainingCooldown((Player) sender)));
            else sender.sendMessage(CC.format("<red>You cannot use this command for another <yellow>%s<red>.",
                    formatRemainingCooldown((Player) sender)));
            return true;
        }

        try {
            if (method.getReturnType() == Boolean.TYPE) {
                boolean success = (Boolean) method.invoke(object, objects.values().toArray());
                if ((success) && (commandCooldown != null) && (sender instanceof Player)) {
                    if (commandCooldown.global())
                        globalCooldown = System.currentTimeMillis();
                    else cooldowns.put((Player) sender, System.currentTimeMillis());
                }
            } else {
                method.invoke(object, objects.values().toArray());
            }
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String getFullLabel() {
        return parent != null ? parent.getFullLabel() + " " + label : label;
    }

    private boolean hasCooldown(Player player) {
        if (commandCooldown == null) {
            return false;
        }

        if (commandCooldown.global()) {
            if (!commandCooldown.bypassPermission().isEmpty() && player.hasPermission(commandCooldown.bypassPermission())) {
                return false;
            }

            return globalCooldown + commandCooldown.timeUnit().toMillis(commandCooldown.time()) >= System.currentTimeMillis();
        }

        if (!cooldowns.containsKey(player)) {
            return false;
        }

        if (!commandCooldown.bypassPermission().isEmpty() && player.hasPermission(commandCooldown.bypassPermission())) {
            return false;
        }

        return cooldowns.get(player) + commandCooldown.timeUnit().toMillis(commandCooldown.time()) >= System.currentTimeMillis();
    }

    private String formatRemainingCooldown(Player player) {
        long end = (commandCooldown.global() ? globalCooldown : cooldowns.get(player))
                + commandCooldown.timeUnit().toMillis(commandCooldown.time());
        return Time.formatDetailed(end - System.currentTimeMillis());
    }

    /**
     * Parses a color name from a {@link Header} annotation field into an
     * Adventure {@link TextColor}.  Accepts any name in
     * {@link NamedTextColor#NAMES} (e.g. {@code "red"}, {@code "dark_gray"}).
     * Falls back to {@link NamedTextColor#WHITE} for unknown names.
     */
    private static TextColor parseHeaderColor(String name) {
        if (name == null || name.isBlank()) return NamedTextColor.WHITE;
        TextColor c = NamedTextColor.NAMES.value(name);
        return c != null ? c : NamedTextColor.WHITE;
    }

}
