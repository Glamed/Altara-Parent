package games.sparking.altara.scoreboard;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResetScore;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Scoreboard {

    private final ScoreboardEntry[] entries = new ScoreboardEntry[15];
    private final String internalName;

    private final User user;
    private Component title;
    private boolean created, changedTitle;

    /**
     * Initialize the Scoreboard object.
     */
    public Scoreboard(User user, String internalName) {
        this(user, internalName, Component.empty());
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(user.getUUID());
    }

    /**
     * Initialize the Scoreboard object.
     */
    public Scoreboard(User user, String internalName, Component title) {
        this.user = user;
        this.internalName = internalName;
        this.title = title;
        created = false;

        for (int i = 0; i < 15; i++) entries[i] = new ScoreboardEntry(String.valueOf(i));
    }

    /*
     * Setters.
     */

    /**
     * Sets the left-aligned text for the given line to the given component.
     * Setting the value to null will remove the line from the scoreboard.
     * Valid indices: 0-14.
     */
    public void setLeftAlignedText(int index, Component text) {
        entries[index].updateLeftAlignedText(text);
    }

    /**
     * Sets the right-aligned text for the given line to the given component.
     * Setting the value to null will remove the line from the scoreboard.
     * Valid indices: 0-14.
     */
    public void setRightAlignedText(int index, Component text) {
        entries[index].updateRightAlignedText(text);
    }

    /**
     * Set the scoreboard's title.
     */
    public void setTitle(Component title) {
        if (this.title.equals(title)) return;
        this.title = title;
        changedTitle = true;
    }

    /*
     * Scoreboard Handlers.
     */

    /**
     * Calling this method will register the scoreboard inside the client.
     */
    public void create() {
        if (created) return;

        user.sendPacket(new WrapperPlayServerScoreboardObjective(
                internalName,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
                title,
                null
        ));

        changedTitle = false;
        created = true;
    }

    /**
     * Calling this method will unregister the scoreboard inside the client.
     */
    public void destroy() {
        if (!created) return;

        user.sendPacket(new WrapperPlayServerScoreboardObjective(
                internalName,
                WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
                title,
                null
        ));

        created = false;

        for (ScoreboardEntry entry : entries) {
            entry.setNameChanged(entry.getLeftDisplayName() != null || entry.getRightDisplayName() != null);
        }
    }

    /**
     * Calling this method will set this scoreboard as the client's active scoreboard.
     * Some versions may require a line to be set to display.
     */
    public void display() {
        if (!created) return;

        user.sendPacket(new WrapperPlayServerDisplayScoreboard(
                1,
                internalName
        ));
    }

    /**
     * Calling this method will send out all appropriate packets to update the client's scoreboard.
     * Don't be afraid to call this often, it will only send packets when required.
     */
    public void update() {
        if (!created) return;

        boolean updated = false;

        /*
         * Important note:
         * We do not send any packets until the update has completed.
         * We write packets to the channel, flushing only once all packets have been constructed!
         */

        for (int i = 0; i < 15; i++) {
            ScoreboardEntry entry = entries[i];

            if (!entry.hasNameChanged()) continue;

            /*
             * If both display names are null, this line is intended to be removed from the scoreboard.
             * Otherwise, this line is intended to be modified on the scoreboard.
             */
            if (entry.getLeftDisplayName() != null || entry.getRightDisplayName() != null) {
                Component left = entry.getLeftDisplayName();
                Component right = entry.getRightDisplayName();

                user.writePacket(new WrapperPlayServerUpdateScore(
                        entry.getIdentifyingName(),
                        WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
                        internalName,
                        15 - i,
                        left == null ? Component.empty() : left,
                        ScoreFormat.fixedScore(right == null ? Component.empty() : right)
                ));

                updated = true;
            } else {
                // In 1.20.3 the ResetScore packet replaced the REMOVE action on the UpdateScore packet.
                user.writePacket(new WrapperPlayServerResetScore(
                        entry.getIdentifyingName(),
                        internalName
                ));

                updated = true;
            }

            entry.update();
        }

        // If the title has been changed then update the scoreboard itself.
        if (changedTitle) {
            user.writePacket(new WrapperPlayServerScoreboardObjective(
                    internalName,
                    WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
                    title,
                    null
            ));

            updated = true;
        }

        // Finally, send all packets to the player!
        if (updated) user.flushPackets();
    }
}