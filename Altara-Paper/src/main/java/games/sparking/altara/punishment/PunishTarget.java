package games.sparking.altara.punishment;

import java.util.UUID;

/** The player being punished — online or not. */
public record PunishTarget(UUID uuid, String name) {
}
