package games.sparking.altara.configuration.entry;

import games.sparking.altara.configuration.StaticConfiguration;
import games.sparking.altara.profile.Profile;
import games.sparking.altara.rank.Rank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class LocalPermissionConfig implements StaticConfiguration {

    List<LocalPermissionEntry> rankPermissions = new CopyOnWriteArrayList<>();
    List<LocalPermissionEntry> playerPermissions = new CopyOnWriteArrayList<>();

    public LocalPermissionEntry getEntry(Rank rank) {
        for (LocalPermissionEntry entry : rankPermissions) {
            if (entry.getUuid().equals(rank.getUuid().toString()))
                return entry;
        }

        return null;
    }

    public LocalPermissionEntry getEntry(Profile profile) {
        for (LocalPermissionEntry entry : playerPermissions) {
            if (entry.getUuid().equals(profile.getUuid().toString()))
                return entry;
        }

        return null;
    }
}