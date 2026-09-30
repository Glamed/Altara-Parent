package games.sparking.altara.framework.module.team;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a colour-coded team inside a {@link games.sparking.altara.framework.GameType#TEAM} game.
 *
 * <p>Teams are created via {@link games.sparking.altara.framework.AbstractGame#createTeam(TeamColor)}
 * and players are assigned via
 * {@link games.sparking.altara.framework.AbstractGame#assignTeam(UUID, GameTeam)}.
 * The owning game always drives membership changes — do not mutate the member
 * set directly from outside the framework.
 */
public class GameTeam {

    @Getter
    private final TeamColor color;

    /** Ordered set of player UUIDs currently on this team (including dead/spectating members). */
    private final Set<UUID> members = new LinkedHashSet<>();

    public GameTeam(TeamColor color) {
        this.color = color;
    }

    // -------------------------------------------------------------------------
    // Membership
    // -------------------------------------------------------------------------

    /** Adds a player to this team. Called internally by the framework. */
    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    /** Removes a player from this team. Called internally by the framework. */
    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    /** Returns an unmodifiable view of all team members (alive + dead + spectating). */
    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    // -------------------------------------------------------------------------
    // Display helpers
    // -------------------------------------------------------------------------

    /** The team's name in its colour. */
    public Component getDisplayName() {
        return color.getColoredName();
    }

    // -------------------------------------------------------------------------
    // Messaging
    // -------------------------------------------------------------------------

    /**
     * Sends {@code message} to every online member of this team.
     *
     * @param message the message, e.g. from {@link games.sparking.altara.utils.CC}
     */
    public void broadcast(ComponentLike message) {
        for (UUID id : members) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.sendMessage(message);
            }
        }
    }
}

