package games.sparking.altara.configuration.entry;

import games.sparking.altara.configuration.StaticConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;

@NoArgsConstructor
@Data
@EqualsAndHashCode
@ToString
public class LocalPermissionEntry implements StaticConfiguration {

    private String uuid = "";
    private ArrayList<String> permissions = new ArrayList<>();
}