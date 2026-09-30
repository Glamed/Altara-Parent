package games.sparking.altara.command.bukkit;

import games.sparking.altara.command.CommandNode;
import games.sparking.altara.command.CommandService;
import games.sparking.altara.command.annotation.Flag;
import games.sparking.altara.command.data.FlagData;
import games.sparking.altara.command.data.ParameterData;
import games.sparking.altara.command.permission.PermissionAdapter;
import games.sparking.altara.task.impl.AsynchronousTaskChain;
import games.sparking.altara.utils.CC;
import games.sparking.altara.utils.Messages;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@RequiredArgsConstructor
public class BukkitCommandNode implements CommandExecutor, TabCompleter {

    private static final AsynchronousTaskChain COMMAND_TASK_CHAIN = new AsynchronousTaskChain(true);

    @Getter
    private final CommandNode node;
    private final JavaPlugin plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> arguments = new ArrayList<>();
        List<String> flags = new ArrayList<>();
        parseArgs(args, arguments, flags);

        CommandNode executionNode = node.findNode(arguments);
        String realLabel = (sender instanceof Player ? "/" : "") + executionNode.getFullLabel();

        flags.removeIf(flag -> !executionNode.getFlags().contains(flag));

        if (executionNode.getPermission() != null) {
            PermissionAdapter adapter = CommandService.getPermissionAdapter(executionNode.getPermission());
            if (adapter != null && !executionNode.isHidden() && !adapter.test(sender))
                return true;
        }

        if (!executionNode.canUse(sender)) {
            sender.sendMessage(executionNode.isHidden()
                    ? CommandService.UNKNOWN_COMMAND_MESSAGE
                    : CommandService.NO_PERMISSION_MESSAGE);
            return true;
        }

        if (executionNode.isPlayerOnly() && !(sender instanceof Player)) {
            sender.sendMessage(CC.error(Messages.PLAYERS_ONLY));
            return true;
        }

        Runnable execution = () -> {
            try {
                if (!executionNode.invoke(sender, arguments, flags)) {
                    sender.sendMessage(executionNode.getUsage(realLabel));
                }
            } catch (Exception e) {
                handleCommandError(sender, realLabel, e);
            }
        };

        if (executionNode.isAsync()) {
            COMMAND_TASK_CHAIN.run(execution);
        } else {
            execution.run();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!node.canUse(sender)) return new ArrayList<>();

        List<String> arguments = new ArrayList<>();
        parseArgs(args, arguments, null);

        List<String> completions = new ArrayList<>();
        CommandNode tabbingNode = node.findNode(arguments);
        int offset = tabbingNode.getFullLabel().split(" ").length - 1;

        tabbingNode.getChilds().stream()
                .filter(child -> child.canUse(sender) && !child.isHidden())
                .forEach(child -> {
                    completions.add(child.getLabel());
                    completions.addAll(child.getAliases());
                });

        List<ParameterData> parameters = tabbingNode.getParameters().stream()
                .filter(d -> d instanceof ParameterData)
                .map(d -> (ParameterData) d)
                .toList();

        int index = Math.max(0, args.length - 1) - offset;
        if (index >= 0 && index < parameters.size()) {
            ParameterData param = parameters.get(index);
            if (param.getParameterType() != null) {
                completions.addAll(param.getParameterType().tabComplete(sender, param.getCompletionFlags()));
            }
        }

        tabbingNode.getParameters().stream()
                .filter(d -> d instanceof FlagData flag && !flag.isHidden())
                .forEach(d -> ((FlagData) d).getNames().forEach(s -> completions.add("-" + s)));

        return getCompletions(args, completions);
    }

    /**
     * Splits raw args into plain arguments and flag tokens.
     * Pass null for flags to skip flag collection (tab completion path).
     */
    private void parseArgs(String[] args, List<String> arguments, List<String> flags) {
        for (String s : args) {
            if (s.isEmpty()) continue;
            if (s.charAt(0) == '-' && Flag.FLAG_PATTERN.matcher(s).matches()) {
                if (flags != null) flags.add(s.replaceFirst("^--?", "").toLowerCase());
            } else {
                arguments.add(s);
            }
        }
    }

    private void handleCommandError(CommandSender sender, String label, Exception e) {
        if (sender.isOp()) {
            sender.sendMessage(CC.error("Command error.", e.getClass().getSimpleName() + ": " + e.getMessage()));
        } else {
            sender.sendMessage(CC.error(Messages.API_ERROR));
        }
        plugin.getLogger().log(Level.SEVERE, "Error executing " + label + " for " + sender.getName(), e);
    }

    private List<String> getCompletions(String[] args, List<String> input) {
        String argument = args.length == 0 ? "" : args[args.length - 1];
        List<String> result = new ArrayList<>();
        for (String s : input) {
            if (s.regionMatches(true, 0, argument, 0, argument.length()) && result.size() < 80)
                result.add(s);
        }
        return result;
    }
}
