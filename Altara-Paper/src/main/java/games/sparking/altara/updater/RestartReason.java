package games.sparking.altara.updater;

import lombok.Getter;

import java.util.List;

@Getter
public enum RestartReason {

    SINGLE_COMMAND(List.of(
            "&8[&4&l!&8] &cThe network will be rebooting in 5 minutes.",
            "&8[&4&l!&8] &7We'll be back within 10 minutes after the reboot begins."
    )),

    GROUP_COMMAND(List.of(
            "&8[&4&l!&8] &cThe network will be rebooting in 5 minutes.",
            "&8[&4&l!&8] &7We'll be back within 10 minutes after the reboot begins."
    )),

    JAR_UPDATE(List.of(
            "&8[&4&l!&8] &cThe network will be rebooting in 5 minutes.",
            "&8[&4&l!&8] &fThis is reboot will bring new features.",
            "&8[&4&l!&8] &7We'll be back within 10 minutes after the reboot begins."
    ));

    private final List<String> description;

    RestartReason(List<String> description) {
        this.description = description;
    }
}
