package games.sparking.altara.command.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
public class FlagData implements Data {

    private final List<String> names;
    private final boolean defaultValue;
    private final String description;
    private final boolean hidden;
    private final int index;

}
