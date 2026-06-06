package games.sparking.altara.reboot;

import lombok.Getter;

import java.util.List;

/*
* Unused
*/
@Getter
public enum RestartReason {

    SINGLE_COMMAND(List.of(
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <red>The network will be rebooting in 5 minutes.",
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <gray>We'll be back within 10 minutes after the reboot begins."
    )),

    GROUP_COMMAND(List.of(
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <red>The network will be rebooting in 5 minutes.",
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <gray>We'll be back within 10 minutes after the reboot begins."
    )),

    JAR_UPDATE(List.of(
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <red>The network will be rebooting in %s minutes.",
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <white>This reboot will bring new features.",
            "<dark_gray>[<dark_red><bold>!<reset><dark_gray>] <gray>We'll be back within 10 minutes after the reboot begins."
    ));

    private final List<String> description;

    RestartReason(List<String> description) {
        this.description = description;
    }
}
